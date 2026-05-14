package org.lingolocal.project.domain.usecase

import org.lingolocal.project.domain.repository.ModelRepository

/**
 * Use case per verificare se un modello è già presente in locale.
 */
class CheckModelExistsUseCase(
    private val repository: ModelRepository
) {
    operator fun invoke(fileName: String): Boolean {
        return repository.isModelDownloaded(fileName)
    }
}
