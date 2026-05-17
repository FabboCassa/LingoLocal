package org.lingolocal.project.domain.usecase

import org.lingolocal.project.domain.repository.FlashcardRepository

class DeleteFlashcardUseCase(private val repository: FlashcardRepository) {
    suspend operator fun invoke(id: Long): Result<Unit> =
        runCatching { repository.delete(id) }
}
