#include <android/log.h>
#include <chrono>
#include <cstring>
#include <fstream>
#include <jni.h>
#include <string>
#include <unistd.h>
#include <vector>

#include "whisper.h"

#define WLOG_TAG "LingoLocalWhisper"
#define WLOGi(...) __android_log_print(ANDROID_LOG_INFO,  WLOG_TAG, __VA_ARGS__)
#define WLOGw(...) __android_log_print(ANDROID_LOG_WARN,  WLOG_TAG, __VA_ARGS__)
#define WLOGe(...) __android_log_print(ANDROID_LOG_ERROR, WLOG_TAG, __VA_ARGS__)
#define WLOGd(...) __android_log_print(ANDROID_LOG_DEBUG, WLOG_TAG, __VA_ARGS__)

// Stato globale singolo: una sola istanza di Whisper attiva alla volta.
static whisper_context * g_wctx = nullptr;
static int               g_wthreads = 4;

// ---------------------------------------------------------------------------
// JNI: load whisper model
// ---------------------------------------------------------------------------
extern "C"
JNIEXPORT jboolean JNICALL
Java_org_lingolocal_project_data_whisper_WhisperEngineAndroid_nativeLoadModel(
        JNIEnv * env, jobject /*thiz*/, jstring jModelPath, jint nThreads) {
    if (g_wctx) {
        WLOGw("Whisper model already loaded — freeing first");
        whisper_free(g_wctx);
        g_wctx = nullptr;
    }

    g_wthreads = nThreads > 0 ? nThreads : 4;

    const char * path = env->GetStringUTFChars(jModelPath, nullptr);
    WLOGi("Loading whisper model: %s (threads=%d)", path, g_wthreads);

    whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false; // CPU-only su Android

    const auto t0 = std::chrono::steady_clock::now();
    g_wctx = whisper_init_from_file_with_params(path, cparams);
    const auto t1 = std::chrono::steady_clock::now();
    const double load_ms = std::chrono::duration<double, std::milli>(t1 - t0).count();
    env->ReleaseStringUTFChars(jModelPath, path);

    if (!g_wctx) {
        WLOGe("whisper_init_from_file_with_params returned null");
        return JNI_FALSE;
    }
    WLOGi("Whisper model loaded OK in %.0f ms", load_ms);
    return JNI_TRUE;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_lingolocal_project_data_whisper_WhisperEngineAndroid_nativeFreeModel(
        JNIEnv * /*env*/, jobject /*thiz*/) {
    if (g_wctx) {
        whisper_free(g_wctx);
        g_wctx = nullptr;
        WLOGi("Whisper model freed");
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_org_lingolocal_project_data_whisper_WhisperEngineAndroid_nativeIsLoaded(
        JNIEnv * /*env*/, jobject /*thiz*/) {
    return g_wctx ? JNI_TRUE : JNI_FALSE;
}

// ---------------------------------------------------------------------------
// JNI: transcribe samples PCM f32 16kHz mono
// ---------------------------------------------------------------------------
extern "C"
JNIEXPORT jstring JNICALL
Java_org_lingolocal_project_data_whisper_WhisperEngineAndroid_nativeTranscribePcm(
        JNIEnv * env, jobject /*thiz*/,
        jfloatArray jSamples, jstring jLang, jboolean translate) {
    if (!g_wctx) {
        WLOGe("nativeTranscribePcm: model not loaded");
        return env->NewStringUTF("");
    }

    const jsize n_samples = env->GetArrayLength(jSamples);
    if (n_samples <= 0) {
        WLOGw("nativeTranscribePcm: empty samples");
        return env->NewStringUTF("");
    }

    // GetFloatArrayElements potrebbe copiare → usiamo Critical per zero-copy
    // dove possibile. Su Android moderno è quasi sempre zero-copy.
    jfloat * samples = env->GetFloatArrayElements(jSamples, nullptr);
    if (!samples) {
        WLOGe("GetFloatArrayElements returned null");
        return env->NewStringUTF("");
    }

    const char * lang_c = env->GetStringUTFChars(jLang, nullptr);
    std::string lang_str(lang_c ? lang_c : "auto");
    env->ReleaseStringUTFChars(jLang, lang_c);

    whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wparams.print_realtime   = false;
    wparams.print_progress   = false;
    wparams.print_timestamps = false;
    wparams.print_special    = false;
    wparams.translate        = translate == JNI_TRUE;
    wparams.language         = lang_str.c_str();
    wparams.n_threads        = g_wthreads;
    wparams.offset_ms        = 0;
    wparams.no_context       = true;
    wparams.single_segment   = false;
    wparams.suppress_blank   = true;
    wparams.suppress_nst     = true; // sopprime non-speech tokens
    // Temperature 0 per output deterministico/stabile.
    wparams.temperature      = 0.0f;
    wparams.temperature_inc  = 0.2f;
    wparams.entropy_thold    = 2.4f;
    wparams.logprob_thold    = -1.0f;

    WLOGi("Transcribing %d samples (%.1fs audio) lang=%s threads=%d",
          (int) n_samples, n_samples / 16000.0f, lang_str.c_str(), g_wthreads);

    const auto t0 = std::chrono::steady_clock::now();
    const int rc = whisper_full(g_wctx, wparams, samples, (int) n_samples);
    const auto t1 = std::chrono::steady_clock::now();
    const double ms = std::chrono::duration<double, std::milli>(t1 - t0).count();

    env->ReleaseFloatArrayElements(jSamples, samples, JNI_ABORT);

    if (rc != 0) {
        WLOGe("whisper_full failed with rc=%d", rc);
        return env->NewStringUTF("");
    }

    // Concatena tutti i segmenti in una stringa.
    std::string out;
    const int n_seg = whisper_full_n_segments(g_wctx);
    for (int i = 0; i < n_seg; ++i) {
        const char * seg = whisper_full_get_segment_text(g_wctx, i);
        if (seg) out += seg;
    }
    // Whisper spesso mette uno spazio iniziale.
    if (!out.empty() && out[0] == ' ') out.erase(0, 1);

    WLOGi("PERF whisper transcribe: %d segments, %.0f ms (%.2fx realtime)",
          n_seg, ms, (n_samples / 16000.0f * 1000.0f) / std::max(1.0, ms));

    return env->NewStringUTF(out.c_str());
}

// ---------------------------------------------------------------------------
// JNI: helper to read a 16-bit PCM WAV file and convert to float samples
// Compatibile con i file scritti dal nostro AudioRecorder Android.
// ---------------------------------------------------------------------------
extern "C"
JNIEXPORT jfloatArray JNICALL
Java_org_lingolocal_project_data_whisper_WhisperEngineAndroid_nativeReadWav16k(
        JNIEnv * env, jobject /*thiz*/, jstring jWavPath) {
    const char * path = env->GetStringUTFChars(jWavPath, nullptr);
    std::string wav_path(path);
    env->ReleaseStringUTFChars(jWavPath, path);

    std::ifstream f(wav_path, std::ios::binary);
    if (!f) {
        WLOGe("nativeReadWav16k: cannot open %s", wav_path.c_str());
        return nullptr;
    }

    // RIFF header (44 byte) — leggiamo skip e poi tutto il PCM 16-bit.
    char header[44];
    f.read(header, 44);
    if (!f) {
        WLOGe("nativeReadWav16k: file too small for WAV header");
        return nullptr;
    }
    if (std::memcmp(header, "RIFF", 4) != 0 || std::memcmp(header + 8, "WAVE", 4) != 0) {
        WLOGe("nativeReadWav16k: not a RIFF/WAVE file");
        return nullptr;
    }

    // Leggiamo i sample 16-bit signed little-endian fino a EOF.
    std::vector<int16_t> pcm16;
    pcm16.reserve(16000 * 30); // pre-alloca per 30s
    int16_t s;
    while (f.read(reinterpret_cast<char *>(&s), sizeof(int16_t))) {
        pcm16.push_back(s);
    }

    if (pcm16.empty()) {
        WLOGw("nativeReadWav16k: no PCM data");
        return env->NewFloatArray(0);
    }

    // Converti a float [-1, 1].
    jfloatArray out = env->NewFloatArray((jsize) pcm16.size());
    if (!out) {
        WLOGe("NewFloatArray failed");
        return nullptr;
    }
    std::vector<float> samples_f32(pcm16.size());
    const float inv_32768 = 1.0f / 32768.0f;
    for (size_t i = 0; i < pcm16.size(); ++i) {
        samples_f32[i] = (float) pcm16[i] * inv_32768;
    }
    env->SetFloatArrayRegion(out, 0, (jsize) samples_f32.size(), samples_f32.data());
    WLOGi("WAV loaded: %zu samples (%.1fs @ 16kHz)", samples_f32.size(), samples_f32.size() / 16000.0f);
    return out;
}
