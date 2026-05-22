package org.lingolocal.project.presentation.settings

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lingolocal.project.domain.model.AppLanguage
import org.lingolocal.project.domain.model.ThemeMode
import org.lingolocal.project.domain.usecase.GetThemeModeUseCase
import org.lingolocal.project.domain.usecase.SetThemeModeUseCase
import org.lingolocal.project.domain.usecase.GetAppLanguageUseCase
import org.lingolocal.project.domain.usecase.SetAppLanguageUseCase

class SettingsScreenModel(
    private val getThemeMode: GetThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val getAppLanguage: GetAppLanguageUseCase,
    private val setAppLanguage: SetAppLanguageUseCase
) : ScreenModel {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            themeMode = getThemeMode().value,
            appLanguage = getAppLanguage().value
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        screenModelScope.launch {
            getThemeMode().collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        screenModelScope.launch {
            getAppLanguage().collect { lang ->
                _uiState.update { it.copy(appLanguage = lang) }
            }
        }
    }

    fun onThemeSelected(mode: ThemeMode) {
        setThemeMode(mode)
    }

    fun onLanguageSelected(language: AppLanguage) {
        setAppLanguage(language)
    }
}
