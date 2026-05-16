package org.lingolocal.project.presentation.settings

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.lingolocal.project.domain.model.ThemeMode
import org.lingolocal.project.domain.usecase.GetThemeModeUseCase
import org.lingolocal.project.domain.usecase.SetThemeModeUseCase

class SettingsScreenModel(
    private val getThemeMode: GetThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase
) : ScreenModel {

    private val _uiState = MutableStateFlow(SettingsUiState(themeMode = getThemeMode().value))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        screenModelScope.launch {
            getThemeMode().collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
    }

    fun onThemeSelected(mode: ThemeMode) {
        setThemeMode(mode)
    }
}
