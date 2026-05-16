package org.lingolocal.project.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.lingolocal.project.domain.model.ThemeMode

/**
 * Repository per le preferenze utente persistenti.
 * Espone i valori come StateFlow per reattività completa nella UI.
 */
interface SettingsRepository {
    val themeMode: StateFlow<ThemeMode>
    fun setThemeMode(mode: ThemeMode)
}
