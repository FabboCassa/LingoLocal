package org.lingolocal.project.domain.usecase

import org.lingolocal.project.domain.repository.LlamaRepository

/**
 * Carica un modello GGUF già presente nel file system locale.
 * Restituisce true in caso di successo.
 */
class LoadLlamaModelUseCase(
    private val repository: LlamaRepository
) {
    suspend operator fun invoke(
        modelPath: String,
        contextSize: Int = 2048,
        threads: Int = 4
    ): Boolean = repository.loadModel(modelPath, contextSize, threads)
}
