package org.lingolocal.project.di

import org.koin.dsl.module
import org.lingolocal.project.data.db.DatabaseDriverFactory

/**
 * Modulo Koin specifico per iOS.
 * Fornisce le implementazioni platform-specific (driver SQLite nativo).
 */
val iosModule = module {
    single { DatabaseDriverFactory() }
}
