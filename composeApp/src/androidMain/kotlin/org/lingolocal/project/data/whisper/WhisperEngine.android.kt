package org.lingolocal.project.data.whisper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.lingolocal.project.data.llama.LlamaEngineAndroid
import org.lingolocal.project.util.logError
import org.lingolocal.project.util.logInfo

actual class WhisperEngine actual constructor() {

    // Carichiamo la libreria nativa (stesso .so di llama).
    private val native = WhisperEngineAndroid()

    // Serializza tutte le chiamate native: whisper_full non è rientrante
    // sullo stesso contesto globale.
    private val mutex = Mutex()

    @Volatile
    private var loaded: Boolean = false

    init {
        // La libreria viene caricata via LlamaEngineAndroid (stesso file .so).
        LlamaEngineAndroid.ensureLibraryLoaded()
    }

    actual suspend fun loadModel(modelPath: String, threads: Int): Boolean = mutex.withLock {
        withContext(Dispatchers.IO) {
            logInfo(TAG, "Loading whisper model: $modelPath (threads=$threads)")
            val ok = try {
                native.nativeLoadModel(modelPath, threads)
            } catch (t: Throwable) {
                logError(TAG, "nativeLoadModel threw", t)
                false
            }
            loaded = ok
            if (ok) logInfo(TAG, "Whisper model loaded") else logError(TAG, "Whisper load failed", null)
            ok
        }
    }

    actual fun isLoaded(): Boolean = loaded

    actual suspend fun transcribeWav(wavPath: String, language: String): String = mutex.withLock {
        if (!loaded) {
            logError(TAG, "transcribeWav called without a loaded model", IllegalStateException())
            return@withLock ""
        }
        withContext(Dispatchers.Default) {
            try {
                val samples = native.nativeReadWav16k(wavPath)
                if (samples == null || samples.isEmpty()) {
                    logError(TAG, "WAV read returned empty samples", null)
                    return@withContext ""
                }
                native.nativeTranscribePcm(samples, language, /*translate=*/ false).trim()
            } catch (t: Throwable) {
                logError(TAG, "nativeTranscribePcm threw", t)
                ""
            }
        }
    }

    actual suspend fun unloadModel() = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (loaded) {
                try {
                    native.nativeFreeModel()
                } catch (t: Throwable) {
                    logError(TAG, "nativeFreeModel threw", t)
                }
                loaded = false
                logInfo(TAG, "Whisper model unloaded")
            }
        }
    }

    private companion object {
        private const val TAG = "WhisperEngine"
    }
}
