package org.lingolocal.project.data.platform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.lingolocal.project.util.logError
import org.lingolocal.project.util.logInfo

/**
 * Cattura voce dell'utente usando ESCLUSIVAMENTE SpeechRecognizer Android (on-device).
 *
 * NOTA tecnica importante: Android non permette a due processi/sorgenti di leggere
 * dal microfono contemporaneamente. In passato qui giravano MediaRecorder e
 * SpeechRecognizer in parallelo: MediaRecorder vinceva la contesa del mic e
 * SpeechRecognizer riceveva 0 byte → ERROR_NO_MATCH istantaneo. Ora il MediaRecorder
 * è stato rimosso e il microfono è dedicato a SpeechRecognizer.
 */
actual class AudioRecorder actual constructor() : KoinComponent {

    private val context: Context by inject()
    private var speechRecognizer: SpeechRecognizer? = null

    @Volatile
    private var isRecording = false

    @Volatile
    private var lastTranscription = ""

    @Volatile
    private var rmsAmplitude = 0.0f

    // onResults / onError arrivano async dopo stopListening(): qui sincronizziamo.
    private var resultsDeferred: CompletableDeferred<String>? = null

    actual fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    actual suspend fun requestPermission(): Boolean {
        return hasPermission()
    }

    actual suspend fun startRecording(outputFilePath: String, targetLanguage: String) {
        // SpeechRecognizer DEVE essere creato e avviato sul Main Thread.
        withContext(Dispatchers.Main) {
            try {
                if (!hasPermission()) {
                    logError(TAG, "Impossibile registrare: permesso microfono mancante", null)
                    return@withContext
                }

                if (isRecording) {
                    cleanupRecognizer()
                }

                lastTranscription = ""
                rmsAmplitude = 0.0f
                resultsDeferred = CompletableDeferred()

                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    logError(TAG, "SpeechRecognizer non disponibile sul dispositivo", null)
                    return@withContext
                }

                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                recognizer.setRecognitionListener(buildListener())

                val localeStr = when (targetLanguage.lowercase()) {
                    "it" -> "it-IT"
                    "es" -> "es-ES"
                    "fr" -> "fr-FR"
                    "de" -> "de-DE"
                    else -> "en-US"
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeStr)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeStr)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, localeStr)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    // Preferenza per il riconoscimento offline (richiede il language pack installato).
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    // Soglie più tolleranti: l'utente sta tenendo premuto il microfono,
                    // quindi non vogliamo che SpeechRecognizer si chiuda da solo dopo 1s di silenzio.
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
                }

                speechRecognizer = recognizer
                isRecording = true
                recognizer.startListening(intent)
                logInfo(TAG, "SpeechRecognizer avviato (locale=$localeStr, preferOffline=true)")
            } catch (e: Exception) {
                logError(TAG, "Errore inizializzazione SpeechRecognizer nativo", e)
                cleanupRecognizer()
            }
        }
    }

    actual fun getLastTranscription(): String {
        return lastTranscription
    }

    actual suspend fun stopRecording(): ByteArray? {
        // Stop SpeechRecognizer sul Main Thread (NON destroy: onResults arriva dopo).
        withContext(Dispatchers.Main) {
            try {
                speechRecognizer?.stopListening()
                logInfo(TAG, "SpeechRecognizer stopListening() chiamato, attendo onResults...")
            } catch (e: Exception) {
                logError(TAG, "Errore durante lo stop dello SpeechRecognizer", e)
                resultsDeferred?.complete(lastTranscription)
            }
        }

        // Aspetta fino a 3s che onResults/onError invochino complete(...).
        val deferred = resultsDeferred
        if (deferred != null) {
            val finalText = withTimeoutOrNull(3000L) { deferred.await() }
            if (finalText == null) {
                logInfo(TAG, "Timeout 3s in attesa di onResults. Uso ultima parziale='${lastTranscription}'")
            }
        }

        withContext(Dispatchers.Main) {
            cleanupRecognizer()
        }

        // I bytes non servono più al pipeline (la trascrizione arriva da SpeechRecognizer),
        // ma il chiamante usa `bytes.isEmpty()` come "registrazione fallita". Ritorniamo un
        // marker non-vuoto se abbiamo ascoltato per davvero.
        return if (lastTranscription.isNotEmpty()) ByteArray(1) else ByteArray(0)
    }

    actual fun getAmplitude(): Float {
        return if (isRecording) rmsAmplitude else 0.0f
    }

    actual fun release() {
        cleanupRecognizer()
        logInfo(TAG, "Risorse AudioRecorder nativo completamente rilasciate.")
    }

    private fun cleanupRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            logError(TAG, "Errore destroy SpeechRecognizer", e)
        }
        speechRecognizer = null
        isRecording = false
        resultsDeferred = null
    }

    private fun buildListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            logInfo(TAG, "SpeechRecognizer pronto per l'ascolto")
        }

        override fun onBeginningOfSpeech() {
            logInfo(TAG, "SpeechRecognizer inizio rilevamento voce")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // rmsdB tipicamente -2..10 dB. Normalizziamo in 0.0..1.0.
            val norm = ((rmsdB + 2.0f) / 12.0f).coerceIn(0.0f, 1.0f)
            rmsAmplitude = norm
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            logInfo(TAG, "SpeechRecognizer fine rilevamento voce")
        }

        override fun onError(error: Int) {
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Errore audio"
                SpeechRecognizer.ERROR_CLIENT -> "Errore client"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permessi insufficienti"
                SpeechRecognizer.ERROR_NETWORK -> "Errore di rete"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout di rete"
                SpeechRecognizer.ERROR_NO_MATCH -> "Nessun riscontro trovato"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Servizio occupato"
                SpeechRecognizer.ERROR_SERVER -> "Errore del server"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Nessun parlato rilevato"
                else -> "Errore sconosciuto ($error)"
            }
            logError(TAG, "Errore SpeechRecognizer: $message", null)
            resultsDeferred?.complete(lastTranscription)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                lastTranscription = matches[0]
                logInfo(TAG, "SpeechRecognizer Risultati finali: $lastTranscription")
            }
            resultsDeferred?.complete(lastTranscription)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                lastTranscription = matches[0]
                logInfo(TAG, "SpeechRecognizer Risultati parziali: $lastTranscription")
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private companion object {
        private const val TAG = "AudioRecorderAndroid"
    }
}
