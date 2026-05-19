package org.lingolocal.project.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.lingolocal.project.data.db.LingoDatabase

class QuizRepositoryTest {

    private fun fakeClock(epochMillis: Long): Clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(epochMillis)
    }

    private fun createInMemoryDb(): LingoDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LingoDatabase.Schema.create(driver)
        driver.execute(identifier = null, sql = "PRAGMA foreign_keys = ON;", parameters = 0)
        return LingoDatabase(driver)
    }

    @Test
    fun `saveResult inserisce correttamente i dati del quiz e restituisce l'id`() = runTest {
        val db = createInMemoryDb()
        val epoch = 1_700_000_000_000L
        val clock = fakeClock(epoch)
        val repo = QuizRepositoryImpl(db, clock)

        val id = repo.saveResult(
            title = "Test Quiz",
            questionsCount = 5,
            correctAnswers = 4
        )

        assertTrue(id > 0)

        val results = repo.getAllResults().first()
        assertEquals(1, results.size)
        val item = results.first()
        assertEquals(id, item.id)
        assertEquals("Test Quiz", item.title)
        assertEquals(5, item.questionsCount)
        assertEquals(4, item.correctAnswers)
        assertEquals(epoch, item.createdAt)
    }

    @Test
    fun `getAllResults restituisce i risultati ordinati per data decrescente`() = runTest {
        val db = createInMemoryDb()
        val clock1 = fakeClock(1_000_000L)
        val repo1 = QuizRepositoryImpl(db, clock1)
        repo1.saveResult("Primo Quiz (Vecchio)", 5, 2)

        val clock2 = fakeClock(2_000_000L)
        val repo2 = QuizRepositoryImpl(db, clock2)
        repo2.saveResult("Secondo Quiz (Nuovo)", 10, 8)

        val results = repo2.getAllResults().first()
        assertEquals(2, results.size)

        // Il primo deve essere il più recente (Secondo Quiz)
        assertEquals("Secondo Quiz (Nuovo)", results[0].title)
        assertEquals(2_000_000L, results[0].createdAt)

        // Il secondo deve essere il più vecchio (Primo Quiz)
        assertEquals("Primo Quiz (Vecchio)", results[1].title)
        assertEquals(1_000_000L, results[1].createdAt)
    }
}
