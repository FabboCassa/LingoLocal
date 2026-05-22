package org.lingolocal.project.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import org.lingolocal.project.domain.model.AppLanguage
import org.lingolocal.project.domain.repository.SettingsRepository

class GetAppLanguageUseCase(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): StateFlow<AppLanguage> = settingsRepository.appLanguage
}
