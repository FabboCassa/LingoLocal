package org.lingolocal.project.presentation.modelmanager

import org.lingolocal.project.domain.model.DownloadProgress

/**
 * Stato immutabile della UI per la schermata di gestione modelli.
 */
data class ModelManagerUiState(
    val downloadProgress: DownloadProgress = DownloadProgress.Idle,
    val isModelAvailable: Boolean = false
)
