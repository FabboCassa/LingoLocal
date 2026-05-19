package org.lingolocal.project.presentation.home

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Clock
import org.lingolocal.project.domain.usecase.GetQuizResultsUseCase
import org.lingolocal.project.domain.usecase.GetWelcomeMessageUseCase

/**
 * ScreenModel (ViewModel) per la schermata Home.
 * Espone lo stato della UI come StateFlow immutabile.
 * Non contiene logica di business: la delega interamente allo UseCase.
 */
class HomeScreenModel(
    private val getWelcomeMessageUseCase: GetWelcomeMessageUseCase,
    private val getQuizResultsUseCase: GetQuizResultsUseCase,
    private val clock: Clock
) : ScreenModel {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadWelcomeMessage()
        observeQuizResults()
    }

    private fun loadWelcomeMessage() {
        screenModelScope.launch {
            val welcome = getWelcomeMessageUseCase()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                welcomeMessage = welcome
            )
        }
    }

    private fun observeQuizResults() {
        screenModelScope.launch {
            getQuizResultsUseCase().collectLatest { results ->
                _uiState.value = _uiState.value.copy(
                    quizResults = results
                )
            }
        }
    }

    fun formatTimeAgo(createdAtEpoch: Long): String {
        val current = clock.now().toEpochMilliseconds()
        val diff = current - createdAtEpoch
        if (diff < 0) return "Adesso"
        val seconds = diff / 1000
        if (seconds < 60) return "Pochi secondi fa"
        val minutes = seconds / 60
        if (minutes < 60) return "$minutes min fa"
        val hours = minutes / 60
        if (hours < 24) return "$hours ore fa"
        val days = hours / 24
        return "$days giorni fa"
    }
}
