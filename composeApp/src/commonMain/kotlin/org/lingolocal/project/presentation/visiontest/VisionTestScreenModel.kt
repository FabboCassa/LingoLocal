package org.lingolocal.project.presentation.visiontest

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lingolocal.project.domain.usecase.PreprocessImageUseCase
import kotlin.math.roundToInt

data class VisionTestUiState(
    val imageBytes: ByteArray? = null,
    val processedImageBytes: ByteArray? = null,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val originalSizeText: String? = null,
    val processedSizeText: String? = null
)

class VisionTestScreenModel(
    private val preprocessImageUseCase: PreprocessImageUseCase
) : ScreenModel {
    private val _uiState = MutableStateFlow(VisionTestUiState())
    val uiState: StateFlow<VisionTestUiState> = _uiState.asStateFlow()

    fun onImagePicked(bytes: ByteArray?) {
        if (bytes == null) return

        val origSizeText = formatSize(bytes.size)
        _uiState.update { state ->
            state.copy(
                imageBytes = bytes,
                originalSizeText = origSizeText,
                processedImageBytes = null,
                processedSizeText = null,
                isProcessing = true,
                errorMessage = null
            )
        }

        screenModelScope.launch {
            try {
                // Esegue il pre-processamento nativo in background (Default dispatcher gestito nello Use Case)
                val processed = preprocessImageUseCase(bytes)
                val procSizeText = formatSize(processed.size)
                _uiState.update { state ->
                    state.copy(
                        processedImageBytes = processed,
                        processedSizeText = procSizeText,
                        isProcessing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        errorMessage = "Errore durante l'elaborazione dell'immagine: ${e.message}",
                        isProcessing = false
                    )
                }
            }
        }
    }

    fun onPermissionDenied(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun clearImage() {
        _uiState.update { VisionTestUiState() }
    }
}

private fun formatSize(bytesSize: Int): String {
    val sizeKb = (bytesSize / 1024.0 * 10.0).roundToInt() / 10.0
    return if (sizeKb > 1024) {
        val sizeMb = (sizeKb / 1024.0 * 10.0).roundToInt() / 10.0
        "$sizeMb MB"
    } else {
        "$sizeKb KB"
    }
}
