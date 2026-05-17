package org.lingolocal.project.domain.usecase

import org.lingolocal.project.domain.repository.FlashcardRepository

/**
 * Crea una nuova flashcard in un deck. La validazione minima (campi non vuoti)
 * è qui per impedire scritture inutili nel DB.
 */
class CreateFlashcardUseCase(private val repository: FlashcardRepository) {
    suspend operator fun invoke(deckId: Long, front: String, back: String): Result<Long> {
        val frontTrim = front.trim()
        val backTrim = back.trim()
        if (frontTrim.isEmpty() || backTrim.isEmpty()) {
            return Result.failure(IllegalArgumentException("front/back cannot be empty"))
        }
        return runCatching { repository.create(deckId, frontTrim, backTrim) }
    }
}
