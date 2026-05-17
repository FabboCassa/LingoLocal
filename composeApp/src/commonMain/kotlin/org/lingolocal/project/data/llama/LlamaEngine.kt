package org.lingolocal.project.data.llama

import kotlinx.coroutines.flow.Flow

/**
 * Engine multipiattaforma per l'inferenza LLM tramite llama.cpp.
 *
 * Implementazioni:
 * - Android: JNI verso libllama.so compilata via NDK/CMake
 * - iOS: cinterop verso llama.xcframework (stub finché non viene buildato)
 *
 * Lo stato (modello caricato, contesto, sampler) è mantenuto internamente
 * dalle implementazioni native. Le chiamate sono pensate per essere effettuate
 * da Dispatchers.IO/Default.
 */
expect class LlamaEngine() {

    /**
     * Inizializza i backend GGML (caricamento librerie native). Idempotente:
     * chiamabile più volte senza effetti collaterali oltre il primo successo.
     */
    suspend fun init()

    /**
     * Carica un modello GGUF dal disco. Se un modello precedente è caricato,
     * viene scaricato prima del nuovo load.
     *
     * @param modelPath percorso assoluto al file .gguf
     * @param contextSize dimensione della context window in token (consigliato 1024-4096)
     * @param threads numero di thread CPU per l'inferenza
     * @return true se il modello è stato caricato con successo
     */
    suspend fun loadModel(modelPath: String, contextSize: Int = 2048, threads: Int = 4): Boolean

    /**
     * Genera tokens in streaming a partire da un prompt.
     * Ogni emissione del Flow è un frammento UTF-8 di testo generato.
     * Il Flow si completa naturalmente quando il modello emette EOG o
     * quando viene raggiunto [maxTokens].
     *
     * @param prompt testo di input completo (chat-template già applicato se necessario)
     * @param maxTokens numero massimo di token da generare
     */
    fun generate(prompt: String, maxTokens: Int = 256): Flow<String>

    /**
     * Libera modello, contesto, sampler e batch. L'engine può essere
     * riutilizzato con un nuovo [loadModel].
     */
    suspend fun unloadModel()

    /**
     * Indica se un modello è attualmente caricato in memoria.
     */
    fun isModelLoaded(): Boolean

    /**
     * Genera l'embedding del testo fornito usando lo stesso modello GGUF
     * caricato (pooling MEAN, vettore L2-normalizzato).
     *
     * Pensato per il flusso RAG (Task 2.3/2.4): un chunk → un vettore denso
     * di dimensione `n_embd` del modello attivo.
     *
     * @return FloatArray con l'embedding o null se nessun modello è caricato
     *         o se l'inferenza interna fallisce.
     */
    suspend fun embed(text: String): FloatArray?
}
