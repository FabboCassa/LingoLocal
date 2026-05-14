package org.lingolocal.project.di

import io.ktor.client.HttpClient
import org.koin.dsl.module
import org.lingolocal.project.data.repository.ModelRepositoryImpl
import org.lingolocal.project.data.repository.WelcomeRepositoryImpl
import org.lingolocal.project.domain.repository.ModelRepository
import org.lingolocal.project.domain.repository.WelcomeRepository
import org.lingolocal.project.domain.usecase.CheckModelExistsUseCase
import org.lingolocal.project.domain.usecase.DownloadModelUseCase
import org.lingolocal.project.domain.usecase.GetWelcomeMessageUseCase
import org.lingolocal.project.presentation.home.HomeScreenModel
import org.lingolocal.project.presentation.modelmanager.ModelManagerScreenModel

/**
 * Modulo Koin principale dell'applicazione.
 * Raggruppa tutti i binding di dependency injection.
 * Man mano che l'app cresce, verranno creati moduli separati per dominio
 * (es. aiModule, databaseModule, ecc.) e composti qui.
 */
val appModule = module {
    // Networking
    single { HttpClient() }

    // Data layer
    single<WelcomeRepository> { WelcomeRepositoryImpl() }
    single<ModelRepository> { ModelRepositoryImpl(get(), get()) }

    // Domain layer
    factory { GetWelcomeMessageUseCase(get()) }
    factory { DownloadModelUseCase(get()) }
    factory { CheckModelExistsUseCase(get()) }

    // Presentation layer
    factory { HomeScreenModel(get()) }
    factory { ModelManagerScreenModel(get(), get()) }
}
