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

/**
 * Test Task 2.3: pipeline completa testo → chunk → embedding → vector store.
 *
 * Usa un `FakeLlamaRepository` che produce embedding deterministici (hash del
 * testo) per evitare di dover caricare un modello GGUF reale durante gli unit
 * test. Verifica che:
 *   - 3 paragrafi vengano spezzati in più chunk
 *   - ogni chunk con embedding valido finisca nel vector store
 *   - una ricerca semantica recuperi il chunk corrispondente alla query
 *   - eventuali failure dell'embedding non blocchino l'intera ingestione
 */
class IndexTextUseCaseTest {

    private class FakeLlamaRepository(
        private val dim: Int = 16,
        private val failOnText: String? = null
    ) : LlamaRepository {
        override suspend fun initialize() {}
        override suspend fun loadModel(modelPath: String, contextSize: Int, threads: Int) = true
        override fun isReady() = true
        override fun generate(prompt: String, imageBytes: ByteArray?, maxTokens: Int): Flow<String> = flowOf("")
        override fun generateChat(systemPrompt: String, userMessage: String, maxTokens: Int): Flow<String> = flowOf("")
        override suspend fun unloadModel() {}

        override suspend fun embed(text: String): FloatArray? {
            if (text == failOnText) return null
            // Embedding deterministico per testo: seed da hashCode -> vector stabile.
            val rng = Random(seed = text.hashCode().toLong())
            val v = FloatArray(dim) { rng.nextFloat() * 2f - 1f }
            // Normalizza L2 per coerenza con il bridge JNI reale.
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
    fun `tre paragrafi producono chunk e embeddings persistiti nel vector store`() = runTest {
        val db = newDb()
        val vectorRepo = VectorRepositoryImpl(db, fakeClock(1_700_000_000_000L))
        val llama = FakeLlamaRepository()
        val useCase = IndexTextUseCase(
            chunker = TextChunkingUseCase(),
            generateEmbedding = GenerateEmbeddingUseCase(llama),
            vectorRepository = vectorRepo
        )

        val p1 = (1..6).joinToString(" ") { "Frase iniziale numero $it del primo paragrafo." }
        val p2 = (1..6).joinToString(" ") { "Frase numero $it del secondo paragrafo." }
        val p3 = (1..6).joinToString(" ") { "Frase finale numero $it del terzo paragrafo." }
        val text = "$p1\n\n$p2\n\n$p3"

        val result = useCase(sourceId = "doc_test", text = text, maxTokensPerChunk = 30)

        assertTrue("attesi >=2 chunk creati, ottenuti ${result.chunksCreated}", result.chunksCreated >= 2)
        assertEquals(result.chunksCreated, result.chunksIndexed)
        assertEquals(0, result.failedChunks)

        val stored = vectorRepo.getAllChunks()
        assertEquals(result.chunksIndexed, stored.size)
        assertTrue(stored.all { it.sourceId == "doc_test" })
        assertTrue(stored.all { it.embedding.isNotEmpty() })

        // Search: usando l'embedding di uno dei chunk salvati, il top-1 deve essere lo stesso chunk.
        val target = stored.first()
        val top = vectorRepo.searchSimilar(target.embedding, topK = 1)
        assertEquals(1, top.size)
        assertEquals(target.id, top[0].chunk.id)
    }

    @Test
    fun `chunk con embedding fallito non blocca la pipeline`() = runTest {
        val db = newDb()
        val vectorRepo = VectorRepositoryImpl(db, fakeClock(0))
        // Costruiamo un testo dove sappiamo che esisterà un chunk pari a "fallisci."
        val text = "fallisci. Questa frase invece deve essere indicizzata correttamente."
        val llama = FakeLlamaRepository(failOnText = "fallisci.")

        val useCase = IndexTextUseCase(
            chunker = TextChunkingUseCase(),
            generateEmbedding = GenerateEmbeddingUseCase(llama),
            vectorRepository = vectorRepo
        )
        // maxTokens=2 forza split per frase, così "fallisci." diventa un chunk a sé.
        val result = useCase(sourceId = "doc", text = text, maxTokensPerChunk = 2)

        assertTrue(result.chunksCreated >= 2)
        assertTrue("almeno un chunk indicizzato", result.chunksIndexed >= 1)
        assertEquals(1, result.failedChunks)
        assertEquals(result.chunksIndexed, vectorRepo.getAllChunks().size)
    }
}
