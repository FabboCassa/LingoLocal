package org.lingolocal.project.di

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlin.time.Clock
import kotlin.time.Instant
import org.koin.dsl.module
import org.lingolocal.project.data.db.DatabaseDriverFactory
import org.lingolocal.project.data.platform.AudioRecorder
import org.lingolocal.project.data.platform.FileStorage
import org.lingolocal.project.data.platform.IosFileStorage
import org.lingolocal.project.data.tts.TtsEngine
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
    single { AudioRecorder() }
    single { TtsEngine() }
    single<FileStorage> { IosFileStorage() }
    single<org.lingolocal.project.data.platform.DeviceHardwareResolver> { org.lingolocal.project.data.platform.IosDeviceHardwareResolver() }
}

@OptIn(ExperimentalForeignApi::class)
private object IosClock : Clock {
    override fun now(): Instant = memScoped {
        val ts = alloc<timespec>()
        clock_gettime(CLOCK_REALTIME.toUInt(), ts.ptr)
        Instant.fromEpochSeconds(ts.tv_sec, ts.tv_nsec)
    }
}
