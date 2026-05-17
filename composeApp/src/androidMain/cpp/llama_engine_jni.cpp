#include <android/log.h>
#include <jni.h>
#include <cmath>
#include <string>
#include <vector>
#include <unistd.h>

#include "chat.h"
#include "common.h"
#include "llama.h"
#include "sampling.h"

#define LOG_TAG "LingoLocalLlama"
#define LOGi(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGw(...) __android_log_print(ANDROID_LOG_WARN,  LOG_TAG, __VA_ARGS__)
#define LOGe(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGd(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// ---------------------------------------------------------------------------
// Global engine state
// Single model + context per process (sufficient for Task 1.4 validation).
// Task 2.3: aggiunto contesto dedicato per embedding (pooling MEAN) creato
// lazy al primo nativeEmbed; condivide lo stesso `g_model` per non duplicare
// i pesi in memoria.
// ---------------------------------------------------------------------------
static llama_model                 * g_model          = nullptr;
static llama_context               * g_ctx            = nullptr;
static llama_context               * g_ctx_embed      = nullptr;
static common_sampler              * g_sampler        = nullptr;
static llama_batch                   g_batch          = {};
static llama_batch                   g_batch_embed    = {};
static common_chat_templates_ptr     g_chat_templates = nullptr;

static int   g_n_ctx    = 2048;
static int   g_n_threads = 4;
static llama_pos g_cur_pos = 0;
static int   g_remaining = 0;

static std::string g_cached_chars;

static void llama_log_to_android(ggml_log_level level, const char * text, void * /*user*/) {
    int prio = ANDROID_LOG_INFO;
    switch (level) {
        case GGML_LOG_LEVEL_ERROR: prio = ANDROID_LOG_ERROR; break;
        case GGML_LOG_LEVEL_WARN:  prio = ANDROID_LOG_WARN;  break;
        case GGML_LOG_LEVEL_INFO:  prio = ANDROID_LOG_INFO;  break;
        case GGML_LOG_LEVEL_DEBUG: prio = ANDROID_LOG_DEBUG; break;
        default: break;
    }
    __android_log_print(prio, "llama.cpp", "%s", text);
}

static bool is_valid_utf8(const char * s) {
    if (!s) return true;
    const unsigned char * b = (const unsigned char *) s;
    while (*b) {
        int n;
        if      ((*b & 0x80) == 0x00) n = 1;
        else if ((*b & 0xE0) == 0xC0) n = 2;
        else if ((*b & 0xF0) == 0xE0) n = 3;
        else if ((*b & 0xF8) == 0xF0) n = 4;
        else return false;
        ++b;
        for (int i = 1; i < n; ++i) {
            if ((*b & 0xC0) != 0x80) return false;
            ++b;
        }
    }
    return true;
}

// ---------------------------------------------------------------------------
// JNI: backend init / shutdown
// ---------------------------------------------------------------------------
extern "C"
JNIEXPORT void JNICALL
Java_org_lingolocal_project_data_llama_LlamaEngineAndroid_nativeBackendInit(
        JNIEnv * env, jobject /*thiz*/, jstring jNativeLibDir) {
    llama_log_set(llama_log_to_android, nullptr);

    const char * dir = env->GetStringUTFChars(jNativeLibDir, nullptr);
    LOGi("Loading ggml backends from %s", dir);
    ggml_backend_load_all_from_path(dir);
    env->ReleaseStringUTFChars(jNativeLibDir, dir);

    llama_backend_init();
    LOGi("Backend ready");
}

extern "C"
JNIEXPORT void JNICALL
Java_org_lingolocal_project_data_llama_LlamaEngineAndroid_nativeBackendFree(
        JNIEnv * /*env*/, jobject /*thiz*/) {
    llama_backend_free();
}

// ---------------------------------------------------------------------------
// JNI: model load / unload
// ---------------------------------------------------------------------------
extern "C"
JNIEXPORT jboolean JNICALL
Java_org_lingolocal_project_data_llama_LlamaEngineAndroid_nativeLoadModel(
        JNIEnv * env, jobject /*thiz*/,
        jstring jModelPath, jint nCtx, jint nThreads) {
    if (g_model || g_ctx) {
        LOGw("Model already loaded — releasing first");
        if (g_sampler) { common_sampler_free(g_sampler); g_sampler = nullptr; }
        if (g_batch.token != nullptr) { llama_batch_free(g_batch); g_batch = {}; }
        if (g_batch_embed.token != nullptr) { llama_batch_free(g_batch_embed); g_batch_embed = {}; }
        if (g_ctx_embed) { llama_free(g_ctx_embed); g_ctx_embed = nullptr; }
        if (g_ctx)   { llama_free(g_ctx); g_ctx = nullptr; }
        if (g_model) { llama_model_free(g_model); g_model = nullptr; }
    }

    g_n_ctx = nCtx > 0 ? nCtx : 2048;
    // Auto-detect threads: use up to half the available CPUs (leaves headroom for UI)
    const int n_cpus = (int) sysconf(_SC_NPROCESSORS_ONLN);
    const int auto_threads = std::max(2, std::min(n_cpus - 2, 8));
    g_n_threads = nThreads > 0 ? nThreads : auto_threads;
    LOGi("Thread count: %d (CPUs online: %d)", g_n_threads, n_cpus);

    const char * path = env->GetStringUTFChars(jModelPath, nullptr);
    LOGi("Loading model: %s (n_ctx=%d, threads=%d)", path, g_n_ctx, g_n_threads);

    llama_model_params mparams = llama_model_default_params();
    g_model = llama_model_load_from_file(path, mparams);
    env->ReleaseStringUTFChars(jModelPath, path);

    if (!g_model) {
        LOGe("llama_model_load_from_file returned null");
        return JNI_FALSE;
    }

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx           = g_n_ctx;
    cparams.n_batch         = 512;
    cparams.n_ubatch        = 512;
    cparams.n_threads       = g_n_threads;
    cparams.n_threads_batch = g_n_threads;

    g_ctx = llama_init_from_model(g_model, cparams);
    if (!g_ctx) {
        LOGe("llama_init_from_model returned null");
        llama_model_free(g_model);
        g_model = nullptr;
        return JNI_FALSE;
    }

    g_batch = llama_batch_init(512, 0, 1);
    g_chat_templates = common_chat_templates_init(g_model, "");

    common_params_sampling sp;
    sp.temp = 0.7f;
    sp.top_k = 40;
    sp.top_p = 0.9f;
    g_sampler = common_sampler_init(g_model, sp);
    if (!g_sampler) {
        LOGe("common_sampler_init returned null");
        return JNI_FALSE;
    }

    g_cur_pos = 0;
    g_remaining = 0;
    g_cached_chars.clear();

    LOGi("Model loaded OK");
    return JNI_TRUE;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_lingolocal_project_data_llama_LlamaEngineAndroid_nativeFreeModel(
        JNIEnv * /*env*/, jobject /*thiz*/) {
    if (g_sampler)         { common_sampler_free(g_sampler); g_sampler = nullptr; }
    if (g_batch.token)     { llama_batch_free(g_batch); g_batch = {}; }
    if (g_batch_embed.token) { llama_batch_free(g_batch_embed); g_batch_embed = {}; }
    g_chat_templates.reset();
    if (g_ctx_embed)       { llama_free(g_ctx_embed); g_ctx_embed = nullptr; }
    if (g_ctx)             { llama_free(g_ctx); g_ctx = nullptr; }
    if (g_model)           { llama_model_free(g_model); g_model = nullptr; }
    g_cur_pos = 0;
    g_remaining = 0;
    g_cached_chars.clear();
    LOGi("Model freed");
}

// ---------------------------------------------------------------------------
// JNI: prompt feed
// ---------------------------------------------------------------------------
extern "C"
JNIEXPORT jboolean JNICALL
Java_org_lingolocal_project_data_llama_LlamaEngineAndroid_nativeBeginCompletion(
        JNIEnv * env, jobject /*thiz*/, jstring jPrompt, jint nPredict) {
    if (!g_ctx || !g_model || !g_sampler) {
        LOGe("nativeBeginCompletion: model not loaded");
        return JNI_FALSE;
    }

    // Reset KV cache + sampler for a fresh session
    llama_memory_clear(llama_get_memory(g_ctx), true);
    common_sampler_reset(g_sampler);
    g_cur_pos = 0;
    g_cached_chars.clear();
    g_remaining = nPredict > 0 ? nPredict : 128;

    const char * prompt = env->GetStringUTFChars(jPrompt, nullptr);
    std::string prompt_str(prompt);
    env->ReleaseStringUTFChars(jPrompt, prompt);

    // Apply chat template if the model has one (instruct models like Qwen, Gemma, Llama3…)
    if (g_chat_templates && common_chat_templates_was_explicit(g_chat_templates.get())) {
        common_chat_templates_inputs inputs;
        inputs.messages.push_back({"system", "You are a helpful assistant.", {}});
        inputs.messages.push_back({"user",   prompt_str, {}});
        inputs.add_generation_prompt = true;
        try {
            auto result = common_chat_templates_apply(g_chat_templates.get(), inputs);
            prompt_str = result.prompt;
            LOGd("Chat template applied. Formatted length: %d", (int) prompt_str.size());
        } catch (...) {
            LOGw("Chat template apply failed, using raw prompt");
        }
    } else {
        LOGd("No explicit chat template — using raw prompt");
    }

    std::vector<llama_token> tokens = common_tokenize(g_ctx, prompt_str, /*add_special=*/true, /*parse_special=*/true);
    LOGi("Prompt tokenized: %d tokens", (int) tokens.size());

    if ((int) tokens.size() >= g_n_ctx) {
        LOGe("Prompt exceeds context (%d >= %d)", (int) tokens.size(), g_n_ctx);
        return JNI_FALSE;
    }

    common_batch_clear(g_batch);
    for (size_t i = 0; i < tokens.size(); ++i) {
        const bool want_logit = (i == tokens.size() - 1);
        common_batch_add(g_batch, tokens[i], (llama_pos) i, {0}, want_logit);
    }

    if (llama_decode(g_ctx, g_batch) != 0) {
        LOGe("llama_decode failed during prompt feed");
        return JNI_FALSE;
    }

    g_cur_pos = (llama_pos) tokens.size();
    return JNI_TRUE;
}

// Returns null when generation should stop (EOG or budget exhausted),
// or a (possibly empty) UTF-8 chunk emitted for the latest token.
extern "C"
JNIEXPORT jstring JNICALL
Java_org_lingolocal_project_data_llama_LlamaEngineAndroid_nativeNextToken(
        JNIEnv * env, jobject /*thiz*/) {
    if (!g_ctx || !g_sampler || !g_model) return nullptr;
    if (g_remaining <= 0) return nullptr;
    if (g_cur_pos >= g_n_ctx - 4) {
        LOGw("Context budget reached — stopping");
        return nullptr;
    }

    const llama_token id = common_sampler_sample(g_sampler, g_ctx, -1);
    common_sampler_accept(g_sampler, id, /*accept_grammar=*/true);

    if (llama_vocab_is_eog(llama_model_get_vocab(g_model), id)) {
        return nullptr;
    }

    const std::string piece = common_token_to_piece(g_ctx, id);
    g_cached_chars += piece;

    common_batch_clear(g_batch);
    common_batch_add(g_batch, id, g_cur_pos, {0}, true);
    if (llama_decode(g_ctx, g_batch) != 0) {
        LOGe("llama_decode failed during generation");
        return nullptr;
    }
    g_cur_pos++;
    g_remaining--;

    jstring result;
    if (is_valid_utf8(g_cached_chars.c_str())) {
        result = env->NewStringUTF(g_cached_chars.c_str());
        g_cached_chars.clear();
    } else {
        result = env->NewStringUTF("");
    }
    return result;
}

// ---------------------------------------------------------------------------
// JNI: embedding (Task 2.3)
// ---------------------------------------------------------------------------
// Crea (lazy) un contesto dedicato con embeddings=true e pooling MEAN sullo
// stesso `g_model` già caricato. Tokenizza l'input, esegue una decode singola
// e copia il vettore prodotto da `llama_get_embeddings_seq(ctx, 0)` in un
// jfloatArray. Restituisce null in caso di errore (modello non caricato,
// prompt che eccede la context window, decode fallita).
// L'embedding viene normalizzato L2 prima del ritorno: questo allinea il
// comportamento alla cosine similarity calcolata in Kotlin (vedi
// CosineSimilarity.kt) senza richiedere modifiche al lato consumer.
// ---------------------------------------------------------------------------
static bool ensure_embed_context() {
    if (g_ctx_embed) return true;
    if (!g_model) {
        LOGe("ensure_embed_context: model not loaded");
        return false;
    }

    llama_context_params cparams = llama_context_default_params();
    cparams.embeddings       = true;
    cparams.pooling_type     = LLAMA_POOLING_TYPE_MEAN;
    cparams.n_ctx            = 512;   // chunk <= ~300 token + slack
    cparams.n_batch          = 512;
    cparams.n_ubatch         = 512;
    cparams.n_threads        = g_n_threads;
    cparams.n_threads_batch  = g_n_threads;

    g_ctx_embed = llama_init_from_model(g_model, cparams);
    if (!g_ctx_embed) {
        LOGe("llama_init_from_model (embed) returned null");
        return false;
    }

    g_batch_embed = llama_batch_init(512, 0, 1);
    LOGi("Embed context ready (n_embd=%d)", llama_model_n_embd(g_model));
    return true;
}

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_org_lingolocal_project_data_llama_LlamaEngineAndroid_nativeEmbed(
        JNIEnv * env, jobject /*thiz*/, jstring jText) {
    if (!g_model) {
        LOGe("nativeEmbed: model not loaded");
        return nullptr;
    }
    if (!ensure_embed_context()) {
        return nullptr;
    }

    const char * c_text = env->GetStringUTFChars(jText, nullptr);
    std::string text(c_text);
    env->ReleaseStringUTFChars(jText, c_text);

    if (text.empty()) {
        LOGw("nativeEmbed: empty input");
        return nullptr;
    }

    std::vector<llama_token> tokens =
        common_tokenize(g_ctx_embed, text, /*add_special=*/true, /*parse_special=*/false);
    if (tokens.empty()) {
        LOGe("nativeEmbed: tokenization produced 0 tokens");
        return nullptr;
    }

    const int n_ctx_embed = (int) llama_n_ctx(g_ctx_embed);
    if ((int) tokens.size() > n_ctx_embed) {
        LOGw("nativeEmbed: prompt %d > embed ctx %d, truncating",
             (int) tokens.size(), n_ctx_embed);
        tokens.resize(n_ctx_embed);
    }

    // Reset cache/sequence prima di una nuova embedding (sequenze indipendenti).
    llama_memory_clear(llama_get_memory(g_ctx_embed), true);

    common_batch_clear(g_batch_embed);
    for (size_t i = 0; i < tokens.size(); ++i) {
        common_batch_add(g_batch_embed, tokens[i], (llama_pos) i, {0}, /*logits=*/true);
    }

    if (llama_decode(g_ctx_embed, g_batch_embed) != 0) {
        LOGe("nativeEmbed: llama_decode failed");
        return nullptr;
    }

    const int n_embd = llama_model_n_embd(g_model);
    if (n_embd <= 0) {
        LOGe("nativeEmbed: invalid n_embd=%d", n_embd);
        return nullptr;
    }

    const float * embd = llama_get_embeddings_seq(g_ctx_embed, 0);
    if (!embd) {
        // Fallback per modelli senza pooled output: media manuale sui token.
        embd = llama_get_embeddings(g_ctx_embed);
        if (!embd) {
            LOGe("nativeEmbed: llama_get_embeddings* returned null");
            return nullptr;
        }
    }

    // Copia + normalizzazione L2.
    std::vector<float> out(n_embd);
    double norm_sq = 0.0;
    for (int i = 0; i < n_embd; ++i) {
        out[i] = embd[i];
        norm_sq += (double) embd[i] * (double) embd[i];
    }
    if (norm_sq > 0.0) {
        const float inv_norm = (float) (1.0 / std::sqrt(norm_sq));
        for (int i = 0; i < n_embd; ++i) out[i] *= inv_norm;
    }

    jfloatArray result = env->NewFloatArray(n_embd);
    if (!result) {
        LOGe("nativeEmbed: NewFloatArray failed");
        return nullptr;
    }
    env->SetFloatArrayRegion(result, 0, n_embd, out.data());
    return result;
}
