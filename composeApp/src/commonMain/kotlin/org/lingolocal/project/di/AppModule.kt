package org.lingolocal.project.di

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import kotlinx.datetime.Clock
import org.koin.dsl.module
import org.lingolocal.project.data.db.DatabaseDriverFactory
import org.lingolocal.project.data.db.LingoDatabase
import org.lingolocal.project.data.db.createLingoDatabase
import org.lingolocal.project.data.llama.LlamaEngine
import org.lingolocal.project.data.repository.DeckRepositoryImpl
import org.lingolocal.project.data.repository.FlashcardRepositoryImpl
import org.lingolocal.project.data.repository.LlamaRepositoryImpl
import org.lingolocal.project.data.repository.ModelRepositoryImpl
import org.lingolocal.project.data.repository.SettingsRepositoryImpl
import org.lingolocal.project.data.repository.StudySessionRepositoryImpl
import org.lingolocal.project.data.repository.VectorRepositoryImpl
import org.lingolocal.project.data.repository.WelcomeRepositoryImpl
import org.lingolocal.project.domain.repository.DeckRepository
import org.lingolocal.project.domain.repository.FlashcardRepository
import org.lingolocal.project.domain.repository.LlamaRepository
import org.lingolocal.project.domain.repository.ModelRepository
import org.lingolocal.project.domain.repository.SettingsRepository
import org.lingolocal.project.domain.repository.StudySessionRepository
import org.lingolocal.project.domain.repository.VectorRepository
import org.lingolocal.project.domain.repository.WelcomeRepository
import org.lingolocal.project.domain.usecase.CheckModelExistsUseCase
import org.lingolocal.project.domain.usecase.CreateDeckUseCase
import org.lingolocal.project.domain.usecase.CreateFlashcardUseCase
import org.lingolocal.project.domain.usecase.DeleteFlashcardUseCase
import org.lingolocal.project.domain.usecase.DownloadModelUseCase
import org.lingolocal.project.domain.usecase.GenerateTextUseCase
import org.lingolocal.project.domain.usecase.GetThemeModeUseCase
import org.lingolocal.project.domain.usecase.GetWelcomeMessageUseCase
import org.lingolocal.project.domain.usecase.InitializeLlamaUseCase
import org.lingolocal.project.domain.usecase.LoadLlamaModelUseCase
import org.lingolocal.project.domain.usecase.ObserveDecksUseCase
import org.lingolocal.project.domain.usecase.ObserveFlashcardsUseCase
import org.lingolocal.project.domain.usecase.SetThemeModeUseCase
import org.lingolocal.project.domain.usecase.UpdateFlashcardUseCase
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

    // Clock (testabile)
    single<Clock> { Clock.System }

    // Networking
    single { HttpClient() }

    // Database (SQLDelight) — DatabaseDriverFactory è fornito dai moduli platform-specific
    single<LingoDatabase> { createLingoDatabase(get<DatabaseDriverFactory>()) }

    // Data layer
    single<WelcomeRepository> { WelcomeRepositoryImpl() }
    single<ModelRepository> { ModelRepositoryImpl(get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<DeckRepository> { DeckRepositoryImpl(get(), get()) }
    single<FlashcardRepository> { FlashcardRepositoryImpl(get(), get()) }
    single<StudySessionRepository> { StudySessionRepositoryImpl(get()) }
    single<VectorRepository> { VectorRepositoryImpl(get(), get()) }
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
    factory { CreateDeckUseCase(get()) }
    factory { ObserveDecksUseCase(get()) }
    factory { CreateFlashcardUseCase(get()) }
    factory { ObserveFlashcardsUseCase(get()) }
    factory { UpdateFlashcardUseCase(get()) }
    factory { DeleteFlashcardUseCase(get()) }

    // Presentation layer
    factory { HomeScreenModel(get()) }
    factory { ModelManagerScreenModel(get(), get()) }
    factory { LlamaTestScreenModel(get(), get(), get(), get()) }
    factory { SettingsScreenModel(get(), get()) }
}
