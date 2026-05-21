package org.lingolocal.project.domain.repository

import kotlinx.coroutines.flow.Flow
import org.lingolocal.project.domain.model.DownloadProgress

/**
 * Repository per il download e la gestione dei file modello AI.
 * Definito nel domain layer per inversione delle dipendenze.
 */
interface ModelRepository {
    /**
     * Scarica un file dalla URL specificata e lo salva nel file system interno.
     * Emette aggiornamenti di progresso tramite Flow.
     *
     * @param url URL del file da scaricare
     * @param fileName Nome del file di destinazione
     * @return Flow di DownloadProgress con gli aggiornamenti
     */
    fun downloadModel(url: String, fileName: String): Flow<DownloadProgress>

    /**
     * Verifica se un modello con il nome specificato è già stato scaricato.
     */
    fun isModelDownloaded(fileName: String): Boolean

    /**
     * Restituisce il path completo del file modello, o null se non esiste.
     */
    fun getModelPath(fileName: String): String?

    /**
     * Cancella un modello scaricato.
     */
    fun deleteModel(fileName: String): Boolean

    /**
     * Restituisce i byte correntemente scaricati di un file parziale.
     */
    fun getDownloadedBytes(fileName: String): Long

    /**
     * Verifica se esiste un download parziale/incompleto di un file modello.
     */
    fun isModelPartial(fileName: String): Boolean
}
