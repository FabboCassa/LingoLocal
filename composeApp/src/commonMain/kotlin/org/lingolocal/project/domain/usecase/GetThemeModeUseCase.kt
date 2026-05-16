package org.lingolocal.project.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import org.lingolocal.project.domain.model.ThemeMode
import org.lingolocal.project.domain.repository.SettingsRepository

class GetThemeModeUseCase(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): StateFlow<ThemeMode> = settingsRepository.themeMode
}
