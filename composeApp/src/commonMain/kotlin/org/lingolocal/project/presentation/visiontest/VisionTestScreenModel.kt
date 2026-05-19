package org.lingolocal.project.presentation.visiontest

import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

data class VisionTestUiState(
    val imageBytes: ByteArray? = null,
    val errorMessage: String? = null,
    val sizeText: String? = null
)

class VisionTestScreenModel : ScreenModel {
    private val _uiState = MutableStateFlow(VisionTestUiState())
    val uiState: StateFlow<VisionTestUiState> = _uiState.asStateFlow()

    fun onImagePicked(bytes: ByteArray?) {
        _uiState.update { state ->
            if (bytes != null) {
                val sizeKb = (bytes.size / 1024.0 * 10.0).roundToInt() / 10.0
                val sizeText = if (sizeKb > 1024) {
                    val sizeMb = (sizeKb / 1024.0 * 10.0).roundToInt() / 10.0
                    "$sizeMb MB"
                } else {
                    "$sizeKb KB"
                }
                state.copy(
                    imageBytes = bytes,
                    errorMessage = null,
                    sizeText = sizeText
                )
            } else {
                // Selezione annullata, manteniamo lo stato precedente o lo puliamo se era vuoto
                state
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
