package org.lingolocal.project.domain.usecase

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.lingolocal.project.data.db.LingoDatabase
import org.lingolocal.project.data.repository.VectorRepositoryImpl
import org.lingolocal.project.domain.repository.LlamaRepository

class RagQueryUseCaseTest {

    private class FakeLlamaRepository(private val dim: Int = 16) : LlamaRepository {
        override suspend fun initialize() {}
        override suspend fun loadModel(modelPath: String, contextSize: Int, threads: Int) = true
        override fun isReady() = true
        override fun generate(prompt: String, imageBytes: ByteArray?, maxTokens: Int): Flow<String> = flowOf("")
        override fun generateChat(systemPrompt: String, userMessage: String, maxTokens: Int): Flow<String> = flowOf("")
        override suspend fun unloadModel() {}

        override suspend fun embed(text: String): FloatArray? {
            if (text == "fail") return null
            // Deterministic embedding
            val rng = Random(seed = text.hashCode().toLong())
            val v = FloatArray(dim) { rng.nextFloat() * 2f - 1f }
            var norm = 0.0
            for (x in v) norm += x * x
            val inv = if (norm > 0) (1.0 / kotlin.math.sqrt(norm)).toFloat() else 1f
            for (i in v.indices) v[i] = v[i] * inv
            return v
        }
    }

    private fun fakeClock(epochMillis: Long): Clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(epochMillis)
    }

    private fun newDb(): LingoDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LingoDatabase.Schema.create(driver)
        return LingoDatabase(driver)
    }

    @Test
    fun `ritorna contesto vuoto su query vuota o se embedding fallisce`() = runTest {
        val db = newDb()
        val vectorRepo = VectorRepositoryImpl(db, fakeClock(0))
        val llama = FakeLlamaRepository()
        val generateEmbedding = GenerateEmbeddingUseCase(llama)
        val useCase = RagQueryUseCase(generateEmbedding, vectorRepo)

        val resBlank = useCase("")
        assertTrue(resBlank.context.isEmpty())
        assertTrue(resBlank.scoredChunks.isEmpty())

        val resFail = useCase("fail")
        assertTrue(resFail.context.isEmpty())
        assertTrue(resFail.scoredChunks.isEmpty())
    }

    @Test
    fun `ritorna i topK chunk corretti e li concatena nel contesto`() = runTest {
        val db = newDb()
        val vectorRepo = VectorRepositoryImpl(db, fakeClock(1_700_000_000_000L))
        val llama = FakeLlamaRepository()
        val generateEmbedding = GenerateEmbeddingUseCase(llama)
        val useCase = RagQueryUseCase(generateEmbedding, vectorRepo)

        // Inseriamo alcuni chunk di testo nel repository vettoriale
        vectorRepo.insertChunk("doc_1", "Questo è un testo sui gatti.", llama.embed("gatti")!!)
        vectorRepo.insertChunk("doc_1", "Questo è un testo sui cani.", llama.embed("cani")!!)
        vectorRepo.insertChunk("doc_1", "La mela è un frutto gustoso.", llama.embed("mela")!!)

        // Eseguiamo la ricerca per "cani" con topK = 1
        val result = useCase("cani", topK = 1)
        assertEquals(1, result.scoredChunks.size)
        assertEquals("Questo è un testo sui cani.", result.scoredChunks[0].chunk.content)
        assertEquals("Questo è un testo sui cani.", result.context)

        // Eseguiamo con topK = 2
        val resultTop2 = useCase("cani", topK = 2)
        assertEquals(2, resultTop2.scoredChunks.size)
        // Il primo dovrebbe essere sui cani (score più alto), poi sui gatti o mela.
        assertEquals("Questo è un testo sui cani.", resultTop2.scoredChunks[0].chunk.content)
        assertTrue(resultTop2.context.contains("Questo è un testo sui cani."))
        assertTrue(resultTop2.context.contains("\n\n"))
    }
}
