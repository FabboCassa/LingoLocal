package org.lingolocal.project.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.lingolocal.project.util.logError
import org.lingolocal.project.util.logInfo
import java.util.Locale

actual class TtsEngine actual constructor() : KoinComponent {

    private val context: Context by inject()
    private var tts: TextToSpeech? = null
    private val initDeferred = CompletableDeferred<Boolean>()

    @Volatile
    private var isSpeakingActive = false

    init {
        logInfo(TAG, "Inizializzazione TextToSpeech nativo Android...")
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                logInfo(TAG, "TextToSpeech inizializzato con successo.")
                setupUtteranceListener()
                initDeferred.complete(true)
            } else {
                logError(TAG, "Inizializzazione TextToSpeech fallita con codice: $status", null)
                initDeferred.complete(false)
            }
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeakingActive = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeakingActive = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isSpeakingActive = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                isSpeakingActive = false
                logError(TAG, "Errore nella riproduzione dell'utterance: $utteranceId, codice errore: $errorCode", null)
            }
        })
    }

    actual suspend fun speak(text: String, languageCode: String) {
        withContext(Dispatchers.IO) {
            try {
                val ready = initDeferred.await()
                if (!ready) {
                    logError(TAG, "Impossibile riprodurre: TTS non pronto", null)
                    return@withContext
                }

                val ttsInstance = tts ?: return@withContext
                val locale = when (languageCode.lowercase()) {
                    "it" -> Locale.ITALIAN
                    "es" -> Locale("es", "ES")
                    "fr" -> Locale.FRENCH
                    "de" -> Locale.GERMAN
                    else -> Locale.ENGLISH
                }

                val langResult = ttsInstance.setLanguage(locale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    logError(TAG, "Lingua non supportata dal TTS Android: $languageCode (locale: $locale)", null)
                    ttsInstance.setLanguage(Locale.ENGLISH) // Fallback ad inglese
                }

                logInfo(TAG, "TTS Speak: \"$text\" in lingua: $languageCode")
                isSpeakingActive = true
                
                val params = android.os.Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "lingolocal_tts_${System.currentTimeMillis()}")
                }
                
                ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, params, params.getString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID))
            } catch (e: Exception) {
                logError(TAG, "Errore durante la chiamata speak()", e)
                isSpeakingActive = false
            }
        }
    }

    actual suspend fun stop() {
        withContext(Dispatchers.IO) {
            try {
                if (initDeferred.isCompleted && initDeferred.getCompleted()) {
                    tts?.stop()
                }
                isSpeakingActive = false
            } catch (e: Exception) {
                logError(TAG, "Errore durante lo stop del TTS", e)
            }
        }
    }

    actual fun isSpeaking(): Boolean {
        return isSpeakingActive || tts?.isSpeaking == true
    }

    actual fun shutdown() {
        try {
            tts?.shutdown()
            tts = null
            logInfo(TAG, "TextToSpeech dismesso e spento.")
        } catch (e: Exception) {
            logError(TAG, "Errore durante il shutdown del TTS", e)
        }
    }

    private companion object {
        private const val TAG = "TtsEngineAndroid"
    }
}
