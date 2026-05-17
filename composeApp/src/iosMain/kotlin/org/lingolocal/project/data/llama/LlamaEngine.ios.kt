package org.lingolocal.project.data.llama

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Stub iOS per LlamaEngine.
 *
 * Task 1.4 (Android first): l'integrazione iOS arriverà in Task 1.4b
 * via cinterop verso `llama.xcframework` (Metal). Per ora questa actual
 * mantiene la compilazione verde sul target iOS senza fornire inferenza.
 */
actual class LlamaEngine actual constructor() {

    private var loaded = false

    actual suspend fun init() {
        // no-op: backend non disponibile in questo stub
    }

    actual suspend fun loadModel(modelPath: String, contextSize: Int, threads: Int): Boolean {
        loaded = false
        return false
    }

    actual fun generate(prompt: String, maxTokens: Int): Flow<String> = flow {
        emit("[iOS LlamaEngine non ancora implementato — Task 1.4b]")
    }

    actual suspend fun unloadModel() {
        loaded = false
    }

    actual fun isModelLoaded(): Boolean = loaded

    actual suspend fun embed(text: String): FloatArray? = null
}
