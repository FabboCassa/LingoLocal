package org.lingolocal.project.domain.repository

import org.lingolocal.project.domain.model.StudySession

/**
 * Repository per le sessioni di studio (analytics).
 */
interface StudySessionRepository {

    /** Avvia una nuova sessione. [deckId] è opzionale (null = review globale). */
    suspend fun startSession(deckId: Long?, startedAt: Long): Long

    /** Chiude la sessione registrando i contatori finali. */
    suspend fun closeSession(id: Long, endedAt: Long, cardsReviewed: Int, cardsCorrect: Int)

    suspend fun recentSessions(limit: Long): List<StudySession>
}
