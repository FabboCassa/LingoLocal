package org.lingolocal.project.di

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import org.koin.dsl.module
import org.lingolocal.project.data.llama.LlamaEngine
import org.lingolocal.project.data.repository.LlamaRepositoryImpl
import org.lingolocal.project.data.repository.ModelRepositoryImpl
import org.lingolocal.project.data.repository.SettingsRepositoryImpl
import org.lingolocal.project.data.repository.WelcomeRepositoryImpl
import org.lingolocal.project.domain.repository.LlamaRepository
import org.lingolocal.project.domain.repository.ModelRepository
import org.lingolocal.project.domain.repository.SettingsRepository
import org.lingolocal.project.domain.repository.WelcomeRepository
import org.lingolocal.project.domain.usecase.CheckModelExistsUseCase
import org.lingolocal.project.domain.usecase.DownloadModelUseCase
import org.lingolocal.project.domain.usecase.GenerateTextUseCase
import org.lingolocal.project.domain.usecase.GetThemeModeUseCase
import org.lingolocal.project.domain.usecase.GetWelcomeMessageUseCase
import org.lingolocal.project.domain.usecase.InitializeLlamaUseCase
import org.lingolocal.project.domain.usecase.LoadLlamaModelUseCase
import org.lingolocal.project.domain.usecase.SetThemeModeUseCase
import org.lingolocal.project.presentation.home.HomeScreenModel
import org.lingolocal.project.presentation.llamatest.LlamaTestScreenModel
import org.lingolocal.project.presentation.modelmanager.ModelManagerScreenModel
import org.lingolocal.project.presentation.settings.SettingsScreenModel

/**
 * Modulo Koin principale dell'applicazione.
 * Raggruppa tutti i binding di dependency injection.
 * Man mano che l'app cresce, verranno creati moduli separati per dominio
 * (es. aiModule, databaseModule, ecc.) e composti qui.
 */
val appModule = module {
    // Persistence (multiplatform-settings-no-arg auto-resolve per piattaforma)
    single<Settings> { Settings() }

    // Networking
    single { HttpClient() }

    // Data layer
    single<WelcomeRepository> { WelcomeRepositoryImpl() }
    single<ModelRepository> { ModelRepositoryImpl(get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    // LlamaEngine ha actual cross-platform (Android JNI, iOS stub) con costruttore no-args
    single { LlamaEngine() }
    single<LlamaRepository> { LlamaRepositoryImpl(get()) }

    // Domain layer
    factory { GetWelcomeMessageUseCase(get()) }
    factory { DownloadModelUseCase(get()) }
    factory { CheckModelExistsUseCase(get()) }
    factory { InitializeLlamaUseCase(get()) }
    factory { LoadLlamaModelUseCase(get()) }
    factory { GenerateTextUseCase(get()) }
    factory { GetThemeModeUseCase(get()) }
    factory { SetThemeModeUseCase(get()) }

    // Presentation layer
    factory { HomeScreenModel(get()) }
    factory { ModelManagerScreenModel(get(), get()) }
    factory { LlamaTestScreenModel(get(), get(), get(), get()) }
    factory { SettingsScreenModel(get(), get()) }
}
