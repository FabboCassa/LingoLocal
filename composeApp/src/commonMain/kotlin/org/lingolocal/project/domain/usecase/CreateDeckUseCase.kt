package org.lingolocal.project.domain.usecase

import org.lingolocal.project.domain.repository.DeckRepository

/**
 * Crea un nuovo mazzo. Restituisce l'id assegnato.
 */
class CreateDeckUseCase(private val repository: DeckRepository) {
    suspend operator fun invoke(name: String, language: String): Long =
        repository.createDeck(name.trim(), language)
}
