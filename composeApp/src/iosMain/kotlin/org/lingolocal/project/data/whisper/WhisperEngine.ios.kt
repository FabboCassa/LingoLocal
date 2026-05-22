package org.lingolocal.project.data.whisper

/**
 * Stub iOS: integrazione whisper.cpp via cinterop arriva in Task successiva.
 */
actual class WhisperEngine actual constructor() {

    private var loaded = false

    actual suspend fun loadModel(modelPath: String, threads: Int): Boolean {
        loaded = false
        return false
    }

    actual fun isLoaded(): Boolean = loaded

    actual suspend fun transcribeWav(wavPath: String, language: String): String = ""

    actual suspend fun unloadModel() {
        loaded = false
    }
}
