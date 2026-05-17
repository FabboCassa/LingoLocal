package org.lingolocal.project.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lingolocal.project.data.db.LingoDatabase
import org.lingolocal.project.data.db.toDomain
import org.lingolocal.project.domain.model.StudySession
import org.lingolocal.project.domain.repository.StudySessionRepository

class StudySessionRepositoryImpl(
    private val db: LingoDatabase
) : StudySessionRepository {

    private val queries get() = db.schemaQueries

    override suspend fun startSession(deckId: Long?, startedAt: Long): Long = withContext(Dispatchers.IO) {
        db.transactionWithResult {
            queries.insertStudySession(deck_id = deckId, started_at = startedAt)
            queries.lastInsertedId().executeAsOne()
        }
    }

    override suspend fun closeSession(id: Long, endedAt: Long, cardsReviewed: Int, cardsCorrect: Int) {
        withContext(Dispatchers.IO) {
            queries.closeStudySession(
                ended_at = endedAt,
                cards_reviewed = cardsReviewed.toLong(),
                cards_correct = cardsCorrect.toLong(),
                id = id
            )
        }
    }

    override suspend fun recentSessions(limit: Long): List<StudySession> = withContext(Dispatchers.IO) {
        queries.selectRecentSessions(limit).executeAsList().map { it.toDomain() }
    }
}
