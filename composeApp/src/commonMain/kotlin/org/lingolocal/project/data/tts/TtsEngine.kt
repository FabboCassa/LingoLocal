package org.lingolocal.project.data.tts

/**
 * Engine multipiattaforma per la sintesi vocale locale (Text-To-Speech).
 * Implementato tramite motori nativi su Android (android.speech.tts.TextToSpeech)
 * e iOS (AVSpeechSynthesizer) per garantire funzionamento locale offline.
 */
expect class TtsEngine() {

    /**
     * Riproduce vocalmente il testo specificato.
     * 
     * @param text Il testo da pronunciare.
     * @param languageCode Codice lingua ISO (es. "it", "en", "es").
     */
    suspend fun speak(text: String, languageCode: String = "en")

    /**
     * Interrompe immediatamente la riproduzione vocale attiva.
     */
    suspend fun stop()

    /**
     * Ritorna true se il motore sta attualmente riproducendo un audio.
     */
    fun isSpeaking(): Boolean

    /**
     * Rilascia le risorse allocate dal motore nativo.
     */
    fun shutdown()
}
