package org.lingolocal.project.domain.usecase

import org.lingolocal.project.data.audio.SpeechToTextEngine
import org.lingolocal.project.util.logInfo

/**
 * Caso d'uso per convertire una registrazione vocale in testo tramite il motore STT locale.
 */
class TranscribeAudioUseCase(
    private val speechToTextEngine: SpeechToTextEngine
) {
    suspend operator fun invoke(audioBytes: ByteArray, targetLanguage: String): String {
        logInfo(TAG, "Esecuzione TranscribeAudio...")
        return speechToTextEngine.transcribe(audioBytes, targetLanguage)
    }

    companion object {
        private const val TAG = "TranscribeAudioUseCase"
    }
}
