package org.lingolocal.project.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.lingolocal.project.domain.repository.LlamaRepository

/**
 * Genera testo in streaming a partire da un prompt utente.
 * Il Flow emette frammenti UTF-8 fino al token di fine generazione.
 */
class GenerateTextUseCase(
    private val repository: LlamaRepository
) {
    operator fun invoke(prompt: String, maxTokens: Int = 256): Flow<String> =
        repository.generate(prompt, maxTokens)
}
