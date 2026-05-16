package org.lingolocal.project.domain.usecase

import org.lingolocal.project.domain.model.ThemeMode
import org.lingolocal.project.domain.repository.SettingsRepository

class SetThemeModeUseCase(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
    }
}
