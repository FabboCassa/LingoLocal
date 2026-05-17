package org.lingolocal.project.domain.repository

import kotlinx.coroutines.flow.Flow
import org.lingolocal.project.domain.model.Deck

/**
 * Repository per i mazzi di flashcard.
 * Astrae il data layer (SQLDelight) dal domain.
 */
interface DeckRepository {

    /** Stream reattivo di tutti i deck, ordinati dal più recente. */
    fun observeAllDecks(): Flow<List<Deck>>

    /** Crea un nuovo deck. Restituisce l'id assegnato. */
    suspend fun createDeck(name: String, language: String): Long

    suspend fun getDeckById(id: Long): Deck?

    suspend fun deleteDeck(id: Long)
}
