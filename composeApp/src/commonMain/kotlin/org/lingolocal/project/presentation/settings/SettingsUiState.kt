package org.lingolocal.project.presentation.settings

import org.lingolocal.project.domain.model.AppLanguage
import org.lingolocal.project.domain.model.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.ITALIANO
)
