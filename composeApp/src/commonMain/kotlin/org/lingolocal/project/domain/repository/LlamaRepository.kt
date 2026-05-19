package org.lingolocal.project.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository per l'inferenza tramite LLM locale (llama.cpp).
 * Astrae i dettagli platform-specific (JNI Android / cinterop iOS) dal domain.
 */
interface LlamaRepository {

    /** Inizializza i backend nativi (idempotente). */
    suspend fun initialize()

    /** Carica un modello GGUF. Ritorna true se l'operazione è andata a buon fine. */
    suspend fun loadModel(modelPath: String, contextSize: Int = 2048, threads: Int = 4): Boolean

    /** True se un modello è attualmente in memoria. */
    fun isReady(): Boolean

    /**
     * Avvia una generazione di testo. Emette frammenti UTF-8 in streaming;
     * il Flow termina quando il modello emette EOG o si esaurisce [maxTokens].
     */
    fun generate(prompt: String, imageBytes: ByteArray? = null, maxTokens: Int = 256): Flow<String>

    /** Libera il modello dalla memoria. */
    suspend fun unloadModel()

    /**
     * Calcola l'embedding pooled del testo (FloatArray L2-normalizzato).
     * Restituisce null se il modello non è caricato o la generazione fallisce.
     */
    suspend fun embed(text: String): FloatArray?
}
