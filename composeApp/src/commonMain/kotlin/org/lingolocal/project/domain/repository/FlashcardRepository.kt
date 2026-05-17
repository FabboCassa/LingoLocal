package org.lingolocal.project.domain.repository

import kotlinx.coroutines.flow.Flow
import org.lingolocal.project.domain.model.Flashcard
import org.lingolocal.project.domain.model.SrsState

/**
 * Repository per le flashcard.
 * - CRUD base
 * - Query per le card "due" (per la modalità study)
 * - Update isolato dello stato SRS
 */
interface FlashcardRepository {

    fun observeByDeck(deckId: Long): Flow<List<Flashcard>>

    suspend fun create(deckId: Long, front: String, back: String): Long

    suspend fun getById(id: Long): Flashcard?

    suspend fun updateContent(id: Long, front: String, back: String)

    suspend fun updateSrsState(id: Long, srs: SrsState)

    suspend fun delete(id: Long)

    suspend fun countByDeck(deckId: Long): Long

    /**
     * Card con [Flashcard.srs.nextReviewAt] <= now, ordinate per data crescente.
     * @param limit numero massimo di card da restituire (per controllare batch size)
     */
    suspend fun getDueCards(now: Long, limit: Long): List<Flashcard>
}
