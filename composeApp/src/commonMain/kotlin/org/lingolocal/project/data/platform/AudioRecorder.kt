package org.lingolocal.project.data.platform

/**
 * Registratore audio multipiattaforma per catturare input vocali dal microfono.
 * Salva l'audio in formato compatibile PCM/WAV (16kHz, mono, 16-bit)
 * ed espone i livelli di ampiezza in tempo reale per le animazioni della UI.
 */
expect class AudioRecorder() {

    /**
     * Ritorna true se l'app possiede i permessi per utilizzare il microfono.
     */
    fun hasPermission(): Boolean

    /**
     * Richiede asincronamente i permessi per registrare l'audio dal microfono.
     * @return true se il permesso viene concesso.
     */
    suspend fun requestPermission(): Boolean

    /**
     * Avvia la registrazione audio salvando i campioni nel file specificato.
     * Passa la lingua corrente per guidare il motore Speech-to-Text nativo.
     */
    suspend fun startRecording(outputFilePath: String, targetLanguage: String)

    /**
     * Interrompe la registrazione corrente e ritorna il contenuto audio come array di byte.
     * Ritorna null se la registrazione non era attiva o se si verifica un errore.
     */
    suspend fun stopRecording(): ByteArray?

    /**
     * Ritorna l'ultima trascrizione offline generata (se supportata dalla piattaforma).
     */
    fun getLastTranscription(): String

    /**
     * Ottiene l'ampiezza dell'input audio corrente (normalizzata tra 0.0f e 1.0f).
     * Utile per pilotare animazioni grafiche (Voice Wave Visualizer).
     */
    fun getAmplitude(): Float

    /**
     * Rilascia le risorse hardware allocate per la registrazione.
     */
    fun release()
}
