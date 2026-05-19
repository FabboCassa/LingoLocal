package org.lingolocal.project.data.repository

import kotlinx.coroutines.flow.Flow
import org.lingolocal.project.data.llama.LlamaEngine
import org.lingolocal.project.domain.repository.LlamaRepository

/**
 * Implementazione concreta del [LlamaRepository] che delega all'engine
 * native ([LlamaEngine]) ottenuto via DI. Nessuna logica di business qui:
 * solo passaggio di parametri e mapping di errori.
 */
class LlamaRepositoryImpl(
    private val engine: LlamaEngine
) : LlamaRepository {

    override suspend fun initialize() = engine.init()

    override suspend fun loadModel(modelPath: String, contextSize: Int, threads: Int): Boolean =
        engine.loadModel(modelPath, contextSize, threads)

    override fun isReady(): Boolean = engine.isModelLoaded()

    override fun generate(prompt: String, imageBytes: ByteArray?, maxTokens: Int): Flow<String> =
        engine.generate(prompt, imageBytes, maxTokens)

    override suspend fun unloadModel() = engine.unloadModel()

    override suspend fun embed(text: String): FloatArray? = engine.embed(text)
}
