package org.lingolocal.project.domain.repository

import org.lingolocal.project.domain.model.ScoredChunk
import org.lingolocal.project.domain.model.TextChunk

/**
 * Repository per il vector storage usato dal motore RAG.
 *
 * Astrae l'implementazione (SQLite via SQLDelight + cosine similarity in-memory)
 * dal domain. Tutte le operazioni sono `suspend`: l'I/O su DB e il calcolo
 * vettoriale vengono eseguiti su un dispatcher I/O dall'implementazione.
 */
interface VectorRepository {

    /**
     * Inserisce un nuovo chunk indicizzato. Restituisce l'id assegnato da SQLite.
     */
    suspend fun insertChunk(
        sourceId: String,
        content: String,
        embedding: FloatArray
    ): Long

    /**
     * Restituisce tutti i chunk presenti nello store.
     * Usato per inspection/debug; il search reale passa da [searchSimilar].
     */
    suspend fun getAllChunks(): List<TextChunk>

    /**
     * Rimuove tutti i chunk associati alla sorgente indicata
     * (es. quando l'utente elimina un documento importato).
     */
    suspend fun deleteBySourceId(sourceId: String)

    /**
     * Ricerca semantica: calcola la cosine similarity tra [queryEmbedding] e
     * ogni chunk presente nello store, restituendo i [topK] risultati ordinati
     * per score decrescente.
     *
     * Se il vector store è vuoto restituisce lista vuota.
     * Se la dimensionalità di un chunk non combacia con la query, quel chunk
     * viene scartato (può capitare cambiando modello di embedding).
     */
    suspend fun searchSimilar(
        queryEmbedding: FloatArray,
        topK: Int
    ): List<ScoredChunk>
}
