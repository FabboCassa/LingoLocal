package org.lingolocal.project.domain.usecase

import org.lingolocal.project.data.tts.TtsEngine
import org.lingolocal.project.util.logInfo

/**
 * Caso d'uso per sintetizzare vocalmente una risposta di testo tramite il motore TTS nativo.
 */
class SpeakTextUseCase(
    private val ttsEngine: TtsEngine
) {
    suspend operator fun invoke(text: String, languageCode: String = "en") {
        logInfo(TAG, "Esecuzione SpeakText: \"$text\" in $languageCode")
        ttsEngine.speak(text, languageCode)
    }

    suspend fun stop() {
        logInfo(TAG, "Esecuzione SpeakText STOP")
        ttsEngine.stop()
    }

    fun isSpeaking(): Boolean {
        return ttsEngine.isSpeaking()
    }

    companion object {
        private const val TAG = "SpeakTextUseCase"
    }
}
