package org.lingolocal.project.presentation.home

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.lingolocal.project.domain.usecase.GetWelcomeMessageUseCase

/**
 * ScreenModel (ViewModel) per la schermata Home.
 * Espone lo stato della UI come StateFlow immutabile.
 * Non contiene logica di business: la delega interamente allo UseCase.
 */
class HomeScreenModel(
    private val getWelcomeMessageUseCase: GetWelcomeMessageUseCase
) : ScreenModel {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadWelcomeMessage()
    }

    private fun loadWelcomeMessage() {
        screenModelScope.launch {
            _uiState.value = HomeUiState(
                isLoading = false,
                welcomeMessage = getWelcomeMessageUseCase()
            )
        }
    }
}
