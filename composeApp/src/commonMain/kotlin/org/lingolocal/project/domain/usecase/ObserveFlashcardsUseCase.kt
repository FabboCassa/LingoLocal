package org.lingolocal.project.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.lingolocal.project.domain.model.Flashcard
import org.lingolocal.project.domain.repository.FlashcardRepository

class ObserveFlashcardsUseCase(private val repository: FlashcardRepository) {
    operator fun invoke(deckId: Long): Flow<List<Flashcard>> = repository.observeByDeck(deckId)
}
