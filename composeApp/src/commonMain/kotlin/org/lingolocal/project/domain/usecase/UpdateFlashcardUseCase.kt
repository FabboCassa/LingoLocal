package org.lingolocal.project.domain.usecase

import org.lingolocal.project.domain.repository.FlashcardRepository

class UpdateFlashcardUseCase(private val repository: FlashcardRepository) {
    suspend operator fun invoke(id: Long, front: String, back: String): Result<Unit> {
        val frontTrim = front.trim()
        val backTrim = back.trim()
        if (frontTrim.isEmpty() || backTrim.isEmpty()) {
            return Result.failure(IllegalArgumentException("front/back cannot be empty"))
        }
        return runCatching { repository.updateContent(id, frontTrim, backTrim) }
    }
}
