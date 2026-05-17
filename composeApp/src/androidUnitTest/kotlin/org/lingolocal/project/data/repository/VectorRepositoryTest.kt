package org.lingolocal.project.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.lingolocal.project.data.db.LingoDatabase

/**
 * Test Task 2.2: Vector storage + cosine similarity in-memory.
 *
 * Verifica:
 *   - inserimento di 10 chunk con vettori casuali persiste i BLOB
 *   - searchSimilar restituisce i top-K ordinati per score decrescente
 *   - deleteBySourceId rimuove tutti i chunk della sorgente
 *   - round-trip BLOB <-> FloatArray non perde informazione (a meno di -0f)
 */
class VectorRepositoryTest {

    private fun fakeClock(epochMillis: Long): Clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(epochMillis)
    }

    private fun createInMemoryDb(): LingoDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LingoDatabase.Schema.create(driver)
        driver.execute(identifier = null, sql = "PRAGMA foreign_keys = ON;", parameters = 0)
        return LingoDatabase(driver)
    }

    private fun randomVector(dim: Int, rng: Random): FloatArray =
        FloatArray(dim) { rng.nextFloat() * 2f - 1f }

    @Test
    fun `top-K restituisce chunk ordinati per similarity decrescente`() = runTest {
        val db = createInMemoryDb()
        val repo = VectorRepositoryImpl(db, fakeClock(1_700_000_000_000L))
        val rng = Random(seed = 42)
        val dim = 16

        // Insert 10 chunk con vettori casuali
        repeat(10) { i ->
            repo.insertChunk(
                sourceId = "doc_test",
                content = "chunk #$i",
                embedding = randomVector(dim, rng)
            )
        }
        assertEquals(10, repo.getAllChunks().size)

        // Query: usa lo stesso vettore di uno dei chunk per garantire un match perfetto
        val target = repo.getAllChunks().first()
        val results = repo.searchSimilar(queryEmbedding = target.embedding, topK = 3)

        assertEquals("topK=3 deve restituire esattamente 3 risultati", 3, results.size)

        // Il primo risultato è il chunk stesso, score == 1.0 (entro tolleranza float)
        assertEquals(target.id, results[0].chunk.id)
        assertEquals(1.0f, results[0].score, 1e-4f)

        // Score strettamente decrescenti
        assertTrue(
            "score devono essere decrescenti: ${results.map { it.score }}",
            results[0].score >= results[1].score && results[1].score >= results[2].score
        )
    }

    @Test
    fun `searchSimilar su store vuoto restituisce lista vuota`() = runTest {
        val db = createInMemoryDb()
        val repo = VectorRepositoryImpl(db, fakeClock(0))
        val res = repo.searchSimilar(queryEmbedding = FloatArray(8) { 0.1f }, topK = 5)
        assertTrue(res.isEmpty())
    }

    @Test
    fun `deleteBySourceId rimuove solo i chunk della sorgente indicata`() = runTest {
        val db = createInMemoryDb()
        val repo = VectorRepositoryImpl(db, fakeClock(1_700_000_000_000L))
        val rng = Random(1)
        val dim = 8

        repeat(3) { repo.insertChunk("doc_A", "a$it", randomVector(dim, rng)) }
        repeat(2) { repo.insertChunk("doc_B", "b$it", randomVector(dim, rng)) }
        assertEquals(5, repo.getAllChunks().size)

        repo.deleteBySourceId("doc_A")
        val remaining = repo.getAllChunks()
        assertEquals(2, remaining.size)
        assertTrue(remaining.all { it.sourceId == "doc_B" })
    }

    @Test
    fun `round-trip BLOB conserva i valori del FloatArray`() = runTest {
        val db = createInMemoryDb()
        val repo = VectorRepositoryImpl(db, fakeClock(1_700_000_000_000L))

        val original = floatArrayOf(0.0f, 1.5f, -2.25f, 3.1415927f, -0.0001f, 100f)
        val id = repo.insertChunk("doc", "x", original)

        val loaded = repo.getAllChunks().first { it.id == id }.embedding
        assertEquals(original.size, loaded.size)
        for (i in original.indices) {
            assertEquals(original[i], loaded[i], 0f)
        }
    }
}
