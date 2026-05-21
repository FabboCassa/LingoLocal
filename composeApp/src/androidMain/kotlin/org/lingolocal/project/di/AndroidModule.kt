package org.lingolocal.project.di

import kotlin.time.Clock
import org.koin.dsl.module
import org.lingolocal.project.data.db.DatabaseDriverFactory
import org.lingolocal.project.data.platform.AndroidFileStorage
import org.lingolocal.project.data.platform.AudioRecorder
import org.lingolocal.project.data.platform.FileStorage
import org.lingolocal.project.data.tts.TtsEngine

/**
 * Modulo Koin specifico per Android.
 * Fornisce le implementazioni platform-specific che richiedono Context Android.
 */
val androidModule = module {
    single<FileStorage> { AndroidFileStorage(get()) }
    single { DatabaseDriverFactory(get()) }
    single<Clock> { Clock.System }
    single { AudioRecorder() }
    single { TtsEngine() }
    single<org.lingolocal.project.data.platform.DeviceHardwareResolver> { org.lingolocal.project.data.platform.AndroidDeviceHardwareResolver(get()) }
}
