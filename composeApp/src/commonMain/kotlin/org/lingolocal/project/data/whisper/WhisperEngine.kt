package org.lingolocal.project.data.whisper

/**
 * Engine multipiattaforma per Speech-To-Text via whisper.cpp (on-device).
 *
 * Lifecycle:
 *  - [loadModel] carica il file .bin GGML quantizzato in RAM (~75-290 MB)
 *  - [transcribeWav] trascrive un file WAV PCM16 16 kHz mono e ritorna testo
 *    con punteggiatura naturale
 *  - [unloadModel] libera la RAM
 *
 * Pensato per essere caricato lazy alla prima voice chat della sessione e
 * scaricato all'uscita per non occupare RAM in background.
 */
expect class WhisperEngine() {

    suspend fun loadModel(modelPath: String, threads: Int = 4): Boolean

    /** True se un modello è caricato in RAM. */
    fun isLoaded(): Boolean

    /**
     * Trascrive un file WAV (PCM 16-bit 16 kHz mono) in testo.
     * @param wavPath path assoluto al file WAV
     * @param language codice ISO ("it", "en", "es", "fr", "de") o "auto"
     * @return testo trascritto con punteggiatura, oppure stringa vuota se errore
     */
    suspend fun transcribeWav(wavPath: String, language: String = "auto"): String

    suspend fun unloadModel()
}
