package org.lingolocal.project.domain.usecase

import org.lingolocal.project.domain.repository.ModelRepository

/**
 * Caso d'uso per eliminare un modello AI dal disco locale.
 */
class DeleteModelUseCase(
    private val repository: ModelRepository
) {
    operator fun invoke(fileName: String): Boolean {
        return repository.deleteModel(fileName)
    }
}
