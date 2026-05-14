package org.lingolocal.project.domain.usecase

import org.lingolocal.project.domain.repository.WelcomeRepository

/**
 * Use case che recupera il messaggio di benvenuto.
 * Dimostra il flusso completo MVVM: View -> ViewModel -> UseCase -> Repository.
 */
class GetWelcomeMessageUseCase(
    private val repository: WelcomeRepository
) {
    operator fun invoke(): String {
        return repository.getWelcomeMessage()
    }
}
