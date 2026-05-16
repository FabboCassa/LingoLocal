package org.lingolocal.project.presentation.llamatest

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.lingolocal.project.domain.repository.LlamaRepository
import org.lingolocal.project.domain.usecase.GenerateTextUseCase
import org.lingolocal.project.domain.usecase.InitializeLlamaUseCase
import org.lingolocal.project.domain.usecase.LoadLlamaModelUseCase
import org.lingolocal.project.util.logDebug
import org.lingolocal.project.util.logError

/**
 * ScreenModel per la schermata di test inferenza LLM.
 * Coordina init backend → load modello → generate streaming.
 */
class LlamaTestScreenModel(
    private val repository: LlamaRepository,
    private val initialize: InitializeLlamaUseCase,
    private val loadModel: LoadLlamaModelUseCase,
    private val generate: GenerateTextUseCase
) : ScreenModel {

    private val _uiState = MutableStateFlow(LlamaTestUiState())
    val uiState: StateFlow<LlamaTestUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null

    private val exceptionHandler = CoroutineExceptionHandler { _, t ->
        logError(TAG, "Unhandled coroutine error", t)
        _uiState.value = _uiState.value.copy(
            phase = LlamaTestUiState.Phase.Error,
            errorMessage = t.message ?: "Errore sconosciuto"
        )
    }

    fun onModelPathChange(path: String) {
        _uiState.value = _uiState.value.copy(modelPathInput = path)
    }

    fun onPromptChange(prompt: String) {
        _uiState.value = _uiState.value.copy(promptInput = prompt)
    }

    fun onLoadClick() {
        val path = _uiState.value.modelPathInput.trim()
        if (path.isEmpty()) return
        screenModelScope.launch(exceptionHandler) {
            _uiState.value = _uiState.value.copy(
                phase = LlamaTestUiState.Phase.Initializing,
                errorMessage = null
            )
            initialize()

            _uiState.value = _uiState.value.copy(phase = LlamaTestUiState.Phase.Loading)
            val ok = loadModel(path)
            logDebug(TAG, "loadModel($path) → $ok")
            _uiState.value = _uiState.value.copy(
                phase = if (ok) LlamaTestUiState.Phase.Loaded else LlamaTestUiState.Phase.LoadFailed
            )
        }
    }

    fun onGenerateClick() {
        if (!repository.isReady()) return
        val prompt = _uiState.value.promptInput
        if (prompt.isBlank()) return

        generationJob?.cancel()
        generationJob = screenModelScope.launch(exceptionHandler) {
            generate(prompt)
                .onStart {
                    _uiState.value = _uiState.value.copy(
                        phase = LlamaTestUiState.Phase.Generating,
                        generatedText = "",
                        errorMessage = null
                    )
                }
                .onCompletion { cause ->
                    val newPhase = when {
                        cause != null -> LlamaTestUiState.Phase.Error
                        else -> LlamaTestUiState.Phase.Loaded
                    }
                    _uiState.value = _uiState.value.copy(
                        phase = newPhase,
                        errorMessage = cause?.message
                    )
                }
                .collect { piece ->
                    _uiState.value = _uiState.value.copy(
                        generatedText = _uiState.value.generatedText + piece
                    )
                }
        }
    }

    fun onUnloadClick() {
        screenModelScope.launch(exceptionHandler) {
            generationJob?.cancel()
            repository.unloadModel()
            _uiState.value = LlamaTestUiState(modelPathInput = _uiState.value.modelPathInput)
        }
    }

    private companion object {
        private const val TAG = "LlamaTestScreenModel"
    }
}
