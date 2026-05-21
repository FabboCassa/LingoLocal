package org.lingolocal.project.domain.usecase

import org.lingolocal.project.data.platform.AudioRecorder
import org.lingolocal.project.util.logInfo

/**
 * Caso d'uso per avviare la registrazione audio dal microfono.
 */
class StartAudioRecordingUseCase(
    private val audioRecorder: AudioRecorder
) {
    suspend operator fun invoke(outputFilePath: String, targetLanguage: String) {
        logInfo(TAG, "Esecuzione StartAudioRecording: $outputFilePath, targetLanguage=$targetLanguage")
        audioRecorder.startRecording(outputFilePath, targetLanguage)
    }

    companion object {
        private const val TAG = "StartAudioRecordingUseCase"
    }
}
