package org.lingolocal.project.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.lingolocal.project.data.db.LingoDatabase
import org.lingolocal.project.domain.model.SrsState

/**
 * Test Task 2.1: CRUD su flashcard + persistenza locale.
 *
 * Usa un DB SQLite in-memory tramite JdbcSqliteDriver (JVM).
 * Verifica:
 *   - Create deck + flashcard
 *   - Read by id e per deck
 *   - Update contenuto
 *   - Update stato SRS
 *   - Delete
 */
class FlashcardCrudTest {

    private fun fakeClock(epochMillis: Long): Clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(epochMillis)
    }

    private fun createInMemoryDb(): LingoDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LingoDatabase.Schema.create(driver)
        // Allinea il setup di test alla configurazione applicativa (FK enforced).
        driver.execute(identifier = null, sql = "PRAGMA foreign_keys = ON;", parameters = 0)
        return LingoDatabase(driver)
    }

    @Test
    fun `crud completo su flashcard persiste i dati`() = runTest {
        val db = createInMemoryDb()
        val clock = fakeClock(epochMillis = 1_700_000_000_000L)

        val deckRepo = DeckRepositoryImpl(db, clock)
        val flashRepo = FlashcardRepositoryImpl(db, clock)

        // CREATE deck + flashcard
        val deckId = deckRepo.createDeck(name = "Spagnolo Base", language = "es")
        val cardId = flashRepo.create(deckId = deckId, front = "Hola", back = "Ciao")

        // READ by id
        val created = flashRepo.getById(cardId)
        assertNotNull("flashcard appena creata deve esistere", created)
        assertEquals("Hola", created!!.front)
        assertEquals("Ciao", created.back)
        assertEquals(deckId, created.deckId)
        assertEquals(SrsState.DEFAULT_EASE_FACTOR, created.srs.easeFactor, 0.001)
        assertEquals(0, created.srs.repetitions)

        // READ by deck
        val cardsInDeck = flashRepo.observeByDeck(deckId).first()
        assertEquals(1, cardsInDeck.size)

        // COUNT
        assertEquals(1L, flashRepo.countByDeck(deckId))

        // UPDATE content
        flashRepo.updateContent(cardId, front = "Hola amigo", back = "Ciao amico")
        val updated = flashRepo.getById(cardId)!!
        assertEquals("Hola amigo", updated.front)
        assertEquals("Ciao amico", updated.back)

        // UPDATE SRS
        val newSrs = SrsState(
            easeFactor = 2.3,
            intervalDays = 5,
            repetitions = 2,
            nextReviewAt = 1_700_500_000_000L
        )
        flashRepo.updateSrsState(cardId, newSrs)
        val afterSrs = flashRepo.getById(cardId)!!
        assertEquals(2.3, afterSrs.srs.easeFactor, 0.001)
        assertEquals(5, afterSrs.srs.intervalDays)
        assertEquals(2, afterSrs.srs.repetitions)
        assertEquals(1_700_500_000_000L, afterSrs.srs.nextReviewAt)

        // DELETE
        flashRepo.delete(cardId)
        assertNull("dopo delete, lookup deve restituire null", flashRepo.getById(cardId))
        assertEquals(0L, flashRepo.countByDeck(deckId))
    }

    @Test
    fun `eliminare un deck rimuove le sue flashcard a cascata`() = runTest {
        val db = createInMemoryDb()
        val clock = fakeClock(epochMillis = 1_700_000_000_000L)
        val deckRepo = DeckRepositoryImpl(db, clock)
        val flashRepo = FlashcardRepositoryImpl(db, clock)

        val deckId = deckRepo.createDeck("Test", "en")
        flashRepo.create(deckId, "a", "b")
        flashRepo.create(deckId, "c", "d")
        assertEquals(2L, flashRepo.countByDeck(deckId))

        deckRepo.deleteDeck(deckId)
        assertEquals(0L, flashRepo.countByDeck(deckId))
    }

    @Test
    fun `selectDueFlashcards restituisce solo le card schedulate`() = runTest {
        val db = createInMemoryDb()
        val now = 1_700_000_000_000L
        val deckRepo = DeckRepositoryImpl(db, fakeClock(now))
        val flashRepo = FlashcardRepositoryImpl(db, fakeClock(now))

        val deckId = deckRepo.createDeck("Test", "en")
        val card1 = flashRepo.create(deckId, "a", "b") // due ora (next_review_at = now)
        val card2 = flashRepo.create(deckId, "c", "d")
        // posticipa card2 nel futuro
        flashRepo.updateSrsState(
            card2,
            SrsState(2.5, 7, 1, nextReviewAt = now + 7L * 24 * 3600 * 1000)
        )

        val due = flashRepo.getDueCards(now = now, limit = 10)
        assertEquals(1, due.size)
        assertEquals(card1, due[0].id)
    }
}
