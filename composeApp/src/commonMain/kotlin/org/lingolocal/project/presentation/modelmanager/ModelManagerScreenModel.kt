package org.lingolocal.project.presentation.modelmanager

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.lingolocal.project.domain.model.DownloadProgress
import org.lingolocal.project.domain.usecase.CheckModelExistsUseCase
import org.lingolocal.project.domain.usecase.DownloadModelUseCase
import org.lingolocal.project.util.logDebug
import org.lingolocal.project.util.logError

/**
 * ScreenModel per la gestione del download dei modelli AI.
 * Espone lo stato di download come StateFlow e coordina gli use case.
 */
class ModelManagerScreenModel(
    private val downloadModelUseCase: DownloadModelUseCase,
    private val checkModelExistsUseCase: CheckModelExistsUseCase
) : ScreenModel {

    companion object {
        private const val TAG = "ModelManagerScreenModel"
        private const val TEST_DOWNLOAD_URL = "https://httpbin.org/bytes/102400"
        private const val TEST_FILE_NAME = "test_model.bin"
    }

    private val _uiState = MutableStateFlow(ModelManagerUiState())
    val uiState: StateFlow<ModelManagerUiState> = _uiState.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        logError(TAG, "Eccezione non gestita nella coroutine", throwable)
        _uiState.value = _uiState.value.copy(
            downloadProgress = DownloadProgress.Error(
                throwable.message ?: "Errore sconosciuto"
            )
        )
    }

    init {
        checkExistingModel()
    }

    private fun checkExistingModel() {
        val exists = checkModelExistsUseCase(TEST_FILE_NAME)
        logDebug(TAG, "Modello già esistente: $exists")
        _uiState.value = _uiState.value.copy(isModelAvailable = exists)
    }

    fun startDownload() {
        logDebug(TAG, "startDownload() chiamato")
        _uiState.value = _uiState.value.copy(
            downloadProgress = DownloadProgress.Downloading(0, -1)
        )
        screenModelScope.launch(exceptionHandler) {
            logDebug(TAG, "Coroutine avviata, inizio collect del flow...")
            downloadModelUseCase(TEST_DOWNLOAD_URL, TEST_FILE_NAME).collect { progress ->
                logDebug(TAG, "Progresso ricevuto: $progress")
                _uiState.value = _uiState.value.copy(
                    downloadProgress = progress,
                    isModelAvailable = progress is DownloadProgress.Completed
                )
            }
            logDebug(TAG, "Collect completato")
        }
    }
}
