package org.lingolocal.project.data.repository

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.lingolocal.project.domain.model.AppLanguage
import org.lingolocal.project.domain.model.ThemeMode
import org.lingolocal.project.domain.repository.SettingsRepository

/**
 * Implementazione di [SettingsRepository] basata su multiplatform-settings.
 * Le preferenze sono persistite a livello di piattaforma
 * (SharedPreferences su Android, NSUserDefaults su iOS).
 */
class SettingsRepositoryImpl(
    private val settings: Settings
) : SettingsRepository {

    private val _themeMode = MutableStateFlow(loadThemeMode())
    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) {
        settings.putString(KEY_THEME_MODE, mode.name)
        _themeMode.value = mode
    }

    private fun loadThemeMode(): ThemeMode =
        ThemeMode.fromName(settings.getStringOrNull(KEY_THEME_MODE))

    private val _appLanguage = MutableStateFlow(loadAppLanguage())
    override val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    override fun setAppLanguage(language: AppLanguage) {
        settings.putString(KEY_APP_LANGUAGE, language.code)
        _appLanguage.value = language
    }

    private fun loadAppLanguage(): AppLanguage =
        AppLanguage.fromCode(settings.getStringOrNull(KEY_APP_LANGUAGE))

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_APP_LANGUAGE = "app_language"
    }
}
