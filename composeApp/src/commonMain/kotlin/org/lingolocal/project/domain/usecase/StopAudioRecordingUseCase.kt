package org.lingolocal.project.domain.usecase

import org.lingolocal.project.data.platform.AudioRecorder
import org.lingolocal.project.util.logInfo

/**
 * Caso d'uso per interrompere la registrazione audio e recuperare i byte registrati.
 */
class StopAudioRecordingUseCase(
    private val audioRecorder: AudioRecorder
) {
    suspend operator fun invoke(): ByteArray? {
        logInfo(TAG, "Esecuzione StopAudioRecording...")
        return audioRecorder.stopRecording()
    }

    companion object {
        private const val TAG = "StopAudioRecordingUseCase"
    }
}
