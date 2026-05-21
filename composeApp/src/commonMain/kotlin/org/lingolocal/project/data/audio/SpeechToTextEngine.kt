package org.lingolocal.project.data.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lingolocal.project.data.platform.AudioRecorder
import org.lingolocal.project.util.logInfo

/**
 * Motore di trascrizione Audio-to-Text (Speech-To-Text).
 * Restituisce la trascrizione catturata dal motore STT nativo della piattaforma
 * (Android SpeechRecognizer / iOS SFSpeechRecognizer). Se non disponibile,
 * ritorna stringa vuota — il chiamante decide come notificare l'utente.
 */
class SpeechToTextEngine(
    private val audioRecorder: AudioRecorder
) {

    suspend fun transcribe(audioBytes: ByteArray, targetLanguage: String): String = withContext(Dispatchers.Default) {
        logInfo(TAG, "Avvio trascrizione locale su ${audioBytes.size} byte in lingua: $targetLanguage")

        val nativeText = audioRecorder.getLastTranscription().trim()
        if (nativeText.isNotEmpty()) {
            logInfo(TAG, "Trascrizione nativa: \"$nativeText\"")
            return@withContext nativeText
        }

        logInfo(TAG, "Nessuna trascrizione nativa disponibile. Ritorno stringa vuota.")
        ""
    }

    companion object {
        private const val TAG = "SpeechToTextEngine"
    }
}
