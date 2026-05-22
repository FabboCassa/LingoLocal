package org.lingolocal.project.data.whisper

/**
 * Bindings JNI di basso livello verso whisper.cpp.
 * Linka contro liblingolocal_llama.so (stessa libreria di llama.cpp).
 */
internal class WhisperEngineAndroid {

    external fun nativeLoadModel(modelPath: String, nThreads: Int): Boolean

    external fun nativeFreeModel()

    external fun nativeIsLoaded(): Boolean

    /**
     * Trascrive samples float32 PCM 16 kHz mono.
     * @param samples array float in [-1.0, 1.0]
     * @param language ISO code o "auto"
     * @param translate se true, traduce in inglese; se false, trascrive nella stessa lingua
     */
    external fun nativeTranscribePcm(samples: FloatArray, language: String, translate: Boolean): String

    /** Legge un file WAV PCM16 16 kHz mono e ritorna i sample come float [-1, 1]. */
    external fun nativeReadWav16k(wavPath: String): FloatArray?
}
