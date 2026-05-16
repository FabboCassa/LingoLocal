package org.lingolocal.project.domain.usecase

import org.lingolocal.project.domain.repository.LlamaRepository

/**
 * Inizializza i backend nativi llama.cpp. Da chiamare una volta
 * (idempotente) prima di qualsiasi load/generate.
 */
class InitializeLlamaUseCase(
    private val repository: LlamaRepository
) {
    suspend operator fun invoke() = repository.initialize()
}
