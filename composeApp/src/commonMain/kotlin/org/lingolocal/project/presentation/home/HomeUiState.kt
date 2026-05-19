package org.lingolocal.project.presentation.home

import org.lingolocal.project.domain.model.QuizResult

/**
 * Data class immutabile che rappresenta lo stato della UI della schermata Home.
 * Separata dal ScreenModel per chiarezza e testabilità.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val welcomeMessage: String = "",
    val quizResults: List<QuizResult> = emptyList()
)
