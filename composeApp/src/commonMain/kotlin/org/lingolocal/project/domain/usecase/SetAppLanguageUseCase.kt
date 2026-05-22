package org.lingolocal.project.domain.usecase

import org.lingolocal.project.domain.model.AppLanguage
import org.lingolocal.project.domain.repository.SettingsRepository

class SetAppLanguageUseCase(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(language: AppLanguage) {
        settingsRepository.setAppLanguage(language)
    }
}
