package org.lingolocal.project.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import org.lingolocal.project.data.db.LingoDatabase
import org.lingolocal.project.data.db.toDomain
import org.lingolocal.project.domain.model.Flashcard
import org.lingolocal.project.domain.model.SrsState
import org.lingolocal.project.domain.repository.FlashcardRepository

class FlashcardRepositoryImpl(
    private val db: LingoDatabase,
    private val clock: Clock
) : FlashcardRepository {

    private val queries get() = db.schemaQueries

    private fun nowMillis(): Long = clock.now().toEpochMilliseconds()

    override fun observeByDeck(deckId: Long): Flow<List<Flashcard>> =
        queries.selectFlashcardsByDeck(deckId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun create(deckId: Long, front: String, back: String): Long = withContext(Dispatchers.IO) {
        db.transactionWithResult {
            val now = nowMillis()
            queries.insertFlashcard(
                deck_id = deckId,
                front = front,
                back = back,
                created_at = now,
                next_review_at = now
            )
            queries.lastInsertedId().executeAsOne()
        }
    }

    override suspend fun getById(id: Long): Flashcard? = withContext(Dispatchers.IO) {
        queries.selectFlashcardById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun updateContent(id: Long, front: String, back: String) {
        withContext(Dispatchers.IO) { queries.updateFlashcard(front = front, back = back, id = id) }
    }

    override suspend fun updateSrsState(id: Long, srs: SrsState) {
        withContext(Dispatchers.IO) {
            queries.updateFlashcardSrs(
                ease_factor = srs.easeFactor,
                interval_days = srs.intervalDays.toLong(),
                repetitions = srs.repetitions.toLong(),
                next_review_at = srs.nextReviewAt,
                id = id
            )
        }
    }

    override suspend fun delete(id: Long) {
        withContext(Dispatchers.IO) { queries.deleteFlashcard(id) }
    }

    override suspend fun countByDeck(deckId: Long): Long = withContext(Dispatchers.IO) {
        queries.countFlashcards(deckId).executeAsOne()
    }

    override suspend fun getDueCards(now: Long, limit: Long): List<Flashcard> = withContext(Dispatchers.IO) {
        queries.selectDueFlashcards(now = now, max_count = limit)
            .executeAsList()
            .map { it.toDomain() }
    }
}
