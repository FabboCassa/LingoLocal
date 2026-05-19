package org.lingolocal.project.data.llama

/**
 * Wrapper JNI di basso livello verso `liblingolocal_llama.so`.
 * Carica la libreria nativa e dichiara i bridge externi.
 * Non usato direttamente dal codice common — è incapsulato da [LlamaEngine] (actual).
 */
internal class LlamaEngineAndroid {

    external fun nativeBackendInit(nativeLibDir: String)
    external fun nativeBackendFree()

    external fun nativeLoadModel(modelPath: String, nCtx: Int, nThreads: Int): Boolean
    external fun nativeFreeModel()

    external fun nativeBeginCompletion(prompt: String, nPredict: Int): Boolean
    external fun nativeBeginCompletionWithVision(prompt: String, imageBytes: ByteArray, nPredict: Int): Boolean

    /**
     * Restituisce il prossimo frammento di testo generato, o null quando
     * la generazione deve terminare (EOG o budget esaurito).
     */
    external fun nativeNextToken(): String?

    /**
     * Genera l'embedding pooled (MEAN) del testo fornito riusando lo stesso
     * modello caricato. Restituisce un FloatArray L2-normalizzato di lunghezza
     * `n_embd`, oppure null se il modello non è caricato o la decode fallisce.
     */
    external fun nativeEmbed(text: String): FloatArray?

    companion object {
        @Volatile
        private var loaded: Boolean = false

        fun ensureLibraryLoaded() {
            if (loaded) return
            synchronized(this) {
                if (!loaded) {
                    System.loadLibrary("lingolocal_llama")
                    loaded = true
                }
            }
        }
    }
}
