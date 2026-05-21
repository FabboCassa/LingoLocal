package org.lingolocal.project.data.platform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
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
import java.io.File

actual class AudioRecorder actual constructor() : KoinComponent {

    private val context: Context by inject()
    private var mediaRecorder: MediaRecorder? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private var currentFilePath: String? = null
    private var isRecording = false

    @Volatile
    private var lastTranscription = ""

    @Volatile
    private var rmsAmplitude = 0.0f

    // Sincronizzazione: onResults / onError arrivano async dopo stopListening()
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
        // Avvia prima la registrazione del file audio di fallback su IO
        withContext(Dispatchers.IO) {
            try {
                if (!hasPermission()) {
                    logError(TAG, "Impossibile registrare: permesso microfono mancante", null)
                    return@withContext
                }

                if (isRecording) {
                    stopRecording()
                }

                currentFilePath = outputFilePath
                lastTranscription = ""
                rmsAmplitude = 0.0f
                resultsDeferred = CompletableDeferred()
                
                val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }

                recorder.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(96000)
                    setAudioSamplingRate(16000)
                    setAudioChannels(1)
                    setOutputFile(outputFilePath)
                    prepare()
                    start()
                }

                mediaRecorder = recorder
                isRecording = true
                logInfo(TAG, "Registrazione audio di fallback avviata: $outputFilePath")
            } catch (e: Exception) {
                logError(TAG, "Errore avvio MediaRecorder", e)
                mediaRecorder = null
            }
        }

        // Avvia lo SpeechRecognizer nativo sul Main Thread (obbligatorio per Android SpeechRecognizer)
        withContext(Dispatchers.Main) {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {
                                logInfo(TAG, "SpeechRecognizer pronto per l'ascolto")
                            }

                            override fun onBeginningOfSpeech() {
                                logInfo(TAG, "SpeechRecognizer inizio rilevamento voce")
                            }

                            override fun onRmsChanged(rmsdB: Float) {
                                // rmsdB spazia tipicamente da -2 a 10 dB. Normalizziamo in 0.0f - 1.0f.
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
                                // Sblocca chi sta aspettando il risultato: meglio stringa vuota
                                // piuttosto che fallback inventato. Usa partial se esiste.
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
                        })

                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            
                            val localeStr = when (targetLanguage.lowercase()) {
                                "it" -> "it-IT"
                                "es" -> "es-ES"
                                "fr" -> "fr-FR"
                                "de" -> "de-DE"
                                else -> "en-US"
                            }
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeStr)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeStr)
                            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, localeStr)
                            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        }

                        startListening(intent)
                    }
                } else {
                    logError(TAG, "SpeechRecognizer non disponibile sul dispositivo", null)
                }
            } catch (e: Exception) {
                logError(TAG, "Errore inizializzazione SpeechRecognizer nativo", e)
            }
        }
    }

    actual fun getLastTranscription(): String {
        return lastTranscription
    }

    actual suspend fun stopRecording(): ByteArray? {
        // Stop SpeechRecognizer sul Main Thread, MA NON distruggerlo subito:
        // onResults/onError arrivano async dopo stopListening().
        withContext(Dispatchers.Main) {
            try {
                speechRecognizer?.stopListening()
                logInfo(TAG, "SpeechRecognizer stopListening() chiamato, attendo onResults...")
            } catch (e: Exception) {
                logError(TAG, "Errore durante lo stop dello SpeechRecognizer", e)
                resultsDeferred?.complete(lastTranscription)
            }
        }

        // Aspetta fino a 3s che onResults/onError invochino complete(...)
        val deferred = resultsDeferred
        if (deferred != null) {
            val finalText = withTimeoutOrNull(3000L) { deferred.await() }
            if (finalText == null) {
                logInfo(TAG, "Timeout 3s in attesa di onResults. Uso ultima parziale='${lastTranscription}'")
            }
        }

        // Ora possiamo distruggere il recognizer
        withContext(Dispatchers.Main) {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
                resultsDeferred = null
            } catch (e: Exception) {
                logError(TAG, "Errore durante destroy SpeechRecognizer", e)
            }
        }

        // Ferma il MediaRecorder su IO e restituisce i byte
        return withContext(Dispatchers.IO) {
            try {
                if (!isRecording || mediaRecorder == null) {
                    return@withContext null
                }

                val recorder = mediaRecorder!!
                recorder.stop()
                recorder.reset()
                recorder.release()
                mediaRecorder = null
                isRecording = false

                val path = currentFilePath
                if (path != null) {
                    val file = File(path)
                    if (file.exists()) {
                        val bytes = file.readBytes()
                        logInfo(TAG, "MediaRecorder completato: ${bytes.size} byte salvati")
                        return@withContext bytes
                    }
                }
                null
            } catch (e: Exception) {
                logError(TAG, "Errore durante lo stop del MediaRecorder", e)
                isRecording = false
                mediaRecorder = null
                null
            }
        }
    }

    actual fun getAmplitude(): Float {
        return if (isRecording) {
            // Se lo SpeechRecognizer fornisce ampiezza RMS valida, usiamo quella
            if (rmsAmplitude > 0.0f) {
                rmsAmplitude
            } else {
                // Altrimenti fallback su MediaRecorder
                try {
                    val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                    val norm = maxAmp.toFloat() / 32767.0f
                    norm.coerceIn(0.0f, 1.0f)
                } catch (e: Exception) {
                    0.0f
                }
            }
        } else {
            0.0f
        }
    }

    actual fun release() {
        try {
            isRecording = false
            mediaRecorder?.release()
            mediaRecorder = null
            
            speechRecognizer?.destroy()
            speechRecognizer = null
            logInfo(TAG, "Risorse AudioRecorder nativo completamente rilasciate.")
        } catch (e: Exception) {
            logError(TAG, "Errore durante la release dell'AudioRecorder", e)
        }
    }

    private companion object {
        private const val TAG = "AudioRecorderAndroid"
    }
}
