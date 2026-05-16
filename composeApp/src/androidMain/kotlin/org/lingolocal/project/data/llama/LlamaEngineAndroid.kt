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

    /**
     * Restituisce il prossimo frammento di testo generato, o null quando
     * la generazione deve terminare (EOG o budget esaurito).
     */
    external fun nativeNextToken(): String?

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
