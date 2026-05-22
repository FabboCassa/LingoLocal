package org.lingolocal.project.domain.usecase

import org.lingolocal.project.data.whisper.WhisperEngine
import org.lingolocal.project.util.logError
import org.lingolocal.project.util.logInfo

/**
 * Trascrive un file audio WAV (PCM 16-bit 16 kHz mono) usando Whisper on-device.
 *
 * Riceve il path del WAV (scritto da AudioRecorder) anziché i bytes per evitare
 * di tenere in RAM file potenzialmente grandi (es. 30s = 960 KB di PCM).
 *
 * Restituisce stringa vuota se:
 *  - Whisper non è caricato (l'utente deve scaricare il modello)
 *  - L'audio è vuoto / troppo breve
 *  - Whisper non rileva voce
 */
class TranscribeAudioUseCase(
    private val whisperEngine: WhisperEngine
) {
    suspend operator fun invoke(wavPath: String, targetLanguage: String): String {
        logInfo(TAG, "Esecuzione TranscribeAudio (path=$wavPath, lang=$targetLanguage)")
        if (!whisperEngine.isLoaded()) {
            logError(TAG, "Whisper non caricato — trascrizione impossibile", null)
            return ""
        }
        return try {
            whisperEngine.transcribeWav(wavPath, targetLanguage)
        } catch (t: Throwable) {
            logError(TAG, "Errore durante trascrizione", t)
            ""
        }
    }

    companion object {
        private const val TAG = "TranscribeAudioUseCase"
    }
}
