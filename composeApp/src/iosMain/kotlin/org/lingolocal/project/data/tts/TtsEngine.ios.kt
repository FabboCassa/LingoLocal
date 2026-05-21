package org.lingolocal.project.data.tts

import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechSynthesisVoice
import org.lingolocal.project.util.logInfo
import org.lingolocal.project.util.logError

@Suppress("unused")
actual class TtsEngine actual constructor() {

    private val synthesizer = AVSpeechSynthesizer()

    actual suspend fun speak(text: String, languageCode: String) {
        try {
            if (text.isBlank()) return

            synthesizer.stopSpeakingAtBoundary(platform.AVFAudio.AVSpeechBoundary.AVSpeechBoundaryImmediate)

            val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
            
            // Trova la voce adatta
            val voiceCode = when (languageCode.lowercase()) {
                "it" -> "it-IT"
                "es" -> "es-ES"
                "fr" -> "fr-FR"
                "de" -> "de-DE"
                else -> "en-US"
            }
            
            utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(voiceCode)
            utterance.rate = 0.5f // Velocità naturale di conversazione
            
            logInfo(TAG, "iOS TTS: speak \"$text\" in $voiceCode")
            synthesizer.speakUtterance(utterance)
        } catch (t: Throwable) {
            logError(TAG, "Errore nella riproduzione vocale su iOS", t)
        }
    }

    actual suspend fun stop() {
        try {
            synthesizer.stopSpeakingAtBoundary(platform.AVFAudio.AVSpeechBoundary.AVSpeechBoundaryImmediate)
        } catch (t: Throwable) {
            logError(TAG, "Errore durante lo stop del TTS iOS", t)
        }
    }

    actual fun isSpeaking(): Boolean {
        return synthesizer.isSpeaking()
    }

    actual fun shutdown() {
        try {
            synthesizer.stopSpeakingAtBoundary(platform.AVFAudio.AVSpeechBoundary.AVSpeechBoundaryImmediate)
        } catch (t: Throwable) {
            logError(TAG, "Errore durante lo spegnimento del TTS iOS", t)
        }
    }

    private companion object {
        private const val TAG = "TtsEngineIos"
    }
}
