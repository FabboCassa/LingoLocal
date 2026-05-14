package org.lingolocal.project.presentation.home

/**
 * Data class immutabile che rappresenta lo stato della UI della schermata Home.
 * Separata dal ScreenModel per chiarezza e testabilità.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val welcomeMessage: String = ""
)
