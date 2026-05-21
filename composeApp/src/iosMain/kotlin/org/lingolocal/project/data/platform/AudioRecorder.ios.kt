package org.lingolocal.project.data.platform

import org.lingolocal.project.util.logInfo
import org.lingolocal.project.util.logError
import platform.Foundation.*

@Suppress("unused")
actual class AudioRecorder actual constructor() {

    private var isRecording = false
    private var outputFilePath: String? = null

    actual fun hasPermission(): Boolean {
        // Ritorna true di default o controlla AVAudioSession.
        // Nello stub iOS teniamo true per semplificare il testing del flusso
        return true
    }

    actual suspend fun requestPermission(): Boolean {
        return true
    }

    actual suspend fun startRecording(outputFilePath: String, targetLanguage: String) {
        try {
            this.outputFilePath = outputFilePath
            isRecording = true
            logInfo(TAG, "Registratore iOS avviato con lingua $targetLanguage: $outputFilePath")
        } catch (t: Throwable) {
            logError(TAG, "Errore avvio registrazione iOS", t)
        }
    }

    actual fun getLastTranscription(): String {
        return ""
    }

    actual suspend fun stopRecording(): ByteArray? {
        try {
            if (!isRecording) return null
            isRecording = false
            logInfo(TAG, "Registratore iOS fermato.")
            // Ritorna un mock audio byte array per il simulatore iOS
            return ByteArray(1024)
        } catch (t: Throwable) {
            logError(TAG, "Errore interruzione registrazione iOS", t)
            return null
        }
    }

    actual fun getAmplitude(): Float {
        // Se sta registrando, oscilla un po' per mostrare l'animazione della linea d'onda
        return if (isRecording) {
            (0.1f + 0.6f * kotlin.math.sin((platform.Foundation.NSDate().timeIntervalSince1970 * 1000.0) / 200.0).toFloat().coerceIn(0f, 1f))
        } else {
            0.0f
        }
    }

    actual fun release() {
        isRecording = false
    }

    private companion object {
        private const val TAG = "AudioRecorderIos"
    }
}
