package org.lingolocal.project.data.llama

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.lingolocal.project.util.logDebug
import org.lingolocal.project.util.logError
import org.lingolocal.project.util.logInfo

actual class LlamaEngine actual constructor() : KoinComponent {

    private val context: Context by inject()
    private val native = LlamaEngineAndroid()

    /**
     * Serializza tutte le chiamate native: llama.cpp con stato globale
     * non è thread-safe per operazioni concorrenti.
     */
    private val mutex = Mutex()

    @Volatile
    private var backendInitialized: Boolean = false

    @Volatile
    private var modelLoaded: Boolean = false

    actual suspend fun init() = mutex.withLock {
        if (backendInitialized) return@withLock
        withContext(Dispatchers.IO) {
            LlamaEngineAndroid.ensureLibraryLoaded()
            val libDir = context.applicationInfo.nativeLibraryDir
            logInfo(TAG, "Init backend (libDir=$libDir)")
            native.nativeBackendInit(libDir)
            backendInitialized = true
        }
    }

    actual suspend fun loadModel(modelPath: String, contextSize: Int, threads: Int): Boolean =
        mutex.withLock {
            if (!backendInitialized) {
                logError(TAG, "loadModel called before init()", IllegalStateException())
                return@withLock false
            }
            withContext(Dispatchers.IO) {
                logInfo(TAG, "Loading model: $modelPath (ctx=$contextSize, threads=$threads)")
                val ok = try {
                    native.nativeLoadModel(modelPath, contextSize, threads)
                } catch (t: Throwable) {
                    logError(TAG, "nativeLoadModel threw", t)
                    false
                }
                modelLoaded = ok
                if (ok) logInfo(TAG, "Model loaded") else logError(TAG, "Model load failed", null)
                ok
            }
        }

    actual fun generate(prompt: String, maxTokens: Int): Flow<String> = flow {
        if (!modelLoaded) {
            logError(TAG, "generate called without a loaded model", IllegalStateException())
            return@flow
        }
        mutex.withLock {
            val ok = native.nativeBeginCompletion(prompt, maxTokens)
            if (!ok) {
                logError(TAG, "nativeBeginCompletion failed", null)
                return@withLock
            }
            logDebug(TAG, "Generation started (maxTokens=$maxTokens)")
            while (true) {
                val piece = native.nativeNextToken() ?: break
                if (piece.isNotEmpty()) emit(piece)
            }
            logDebug(TAG, "Generation done")
        }
    }.flowOn(Dispatchers.Default)

    actual suspend fun unloadModel() = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (modelLoaded) {
                native.nativeFreeModel()
                modelLoaded = false
                logInfo(TAG, "Model unloaded")
            }
        }
    }

    actual fun isModelLoaded(): Boolean = modelLoaded

    private companion object {
        private const val TAG = "LlamaEngine"
    }
}
