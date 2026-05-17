package org.lingolocal.project.di

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlin.time.Clock
import kotlin.time.Instant
import org.koin.dsl.module
import org.lingolocal.project.data.db.DatabaseDriverFactory
import platform.posix.CLOCK_REALTIME
import platform.posix.clock_gettime
import platform.posix.timespec

/**
 * Modulo Koin specifico per iOS.
 * Fornisce le implementazioni platform-specific (driver SQLite nativo).
 */
val iosModule = module {
    single { DatabaseDriverFactory() }
    single<Clock> { IosClock }
}

@OptIn(ExperimentalForeignApi::class)
private object IosClock : Clock {
    override fun now(): Instant = memScoped {
        val ts = alloc<timespec>()
        clock_gettime(CLOCK_REALTIME.toUInt(), ts.ptr)
        Instant.fromEpochSeconds(ts.tv_sec.toLong(), ts.tv_nsec.toLong())
    }
}
