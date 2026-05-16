package org.lingolocal.project.data.repository

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
    }
}
