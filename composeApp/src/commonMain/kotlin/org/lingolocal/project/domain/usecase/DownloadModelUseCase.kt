package org.lingolocal.project.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.lingolocal.project.domain.model.DownloadProgress
import org.lingolocal.project.domain.repository.ModelRepository

/**
 * Use case per il download di un modello AI.
 * Singola responsabilità: avviare il download e restituire il flusso di progressi.
 */
class DownloadModelUseCase(
    private val repository: ModelRepository
) {
    operator fun invoke(url: String, fileName: String): Flow<DownloadProgress> {
        return repository.downloadModel(url, fileName)
    }
}
