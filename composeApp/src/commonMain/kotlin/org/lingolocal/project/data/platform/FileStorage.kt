package org.lingolocal.project.data.platform

/**
 * Interfaccia multipiattaforma per l'accesso al file system interno.
 * Ogni piattaforma (Android/iOS) fornirà la propria implementazione
 * tramite injection Koin.
 */
interface FileStorage {
    /**
     * Restituisce il path assoluto della directory interna dell'app
     * dove salvare i modelli AI scaricati.
     */
    fun getModelsDirectory(): String

    /**
     * Verifica se un file esiste nel path specificato.
     */
    fun fileExists(filePath: String): Boolean

    /**
     * Restituisce la dimensione in byte del file, o 0 se non esiste.
     */
    fun getFileSize(filePath: String): Long

    /**
     * Cancella un file dal path specificato.
     * Restituisce true se eliminato con successo.
     */
    fun deleteFile(filePath: String): Boolean

    /**
     * Scrive dati in un file tramite una callback.
     * La callback riceve una funzione (ByteArray, offset, length) -> Unit
     * per scrivere chunk di dati progressivamente.
     *
     * @param filePath Path del file da creare/sovrascrivere
     * @param writer Lambda che riceve la funzione di scrittura
     */
    suspend fun writeFile(
        filePath: String,
        writer: suspend (write: (ByteArray, Int, Int) -> Unit) -> Unit
    )
}
