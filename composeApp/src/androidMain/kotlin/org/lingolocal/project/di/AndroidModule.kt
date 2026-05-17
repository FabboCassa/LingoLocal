package org.lingolocal.project.di

import kotlin.time.Clock
import org.koin.dsl.module
import org.lingolocal.project.data.db.DatabaseDriverFactory
import org.lingolocal.project.data.platform.AndroidFileStorage
import org.lingolocal.project.data.platform.FileStorage

/**
 * Modulo Koin specifico per Android.
 * Fornisce le implementazioni platform-specific che richiedono Context Android.
 */
val androidModule = module {
    single<FileStorage> { AndroidFileStorage(get()) }
    single { DatabaseDriverFactory(get()) }
    single<Clock> { Clock.System }
}
