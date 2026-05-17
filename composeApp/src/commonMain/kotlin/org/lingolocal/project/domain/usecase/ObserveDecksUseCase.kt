package org.lingolocal.project.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.lingolocal.project.domain.model.Deck
import org.lingolocal.project.domain.repository.DeckRepository

class ObserveDecksUseCase(private val repository: DeckRepository) {
    operator fun invoke(): Flow<List<Deck>> = repository.observeAllDecks()
}
