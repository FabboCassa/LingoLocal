package org.lingolocal.project.presentation.modelmanager

import org.lingolocal.project.data.platform.DeviceHardwareInfo
import org.lingolocal.project.domain.model.DownloadProgress

/**
 * Rappresenta i metadati e lo stato di un modello AI nel Model Manager.
 */
/** Distingue i modelli LLM (generativi) dai modelli STT (Whisper). */
enum class ModelKind { LLM, WHISPER }

data class AiModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val sizeLabel: String,
    val fileName: String,
    val url: String,
    val kind: ModelKind = ModelKind.LLM,
    val isExternal: Boolean = false,
    val isDownloaded: Boolean = false,
    val downloadProgress: DownloadProgress = DownloadProgress.Idle,
    val isActive: Boolean = false,
    val isLoading: Boolean = false,
    val isRecommended: Boolean = false,
    val isPartial: Boolean = false,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val isDownloadingActive: Boolean = false
)

/**
 * Stato immutabile della UI per la gestione dei modelli AI.
 */
data class ModelManagerUiState(
    val models: List<AiModelInfo> = emptyList(),
    val errorMessage: String? = null,
    val hardwareInfo: DeviceHardwareInfo? = null,
    val modelsDirectory: String = ""
)
