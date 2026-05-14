package org.lingolocal.project.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.contentLength
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.lingolocal.project.data.platform.FileStorage
import org.lingolocal.project.domain.model.DownloadProgress
import org.lingolocal.project.domain.repository.ModelRepository
import org.lingolocal.project.util.logDebug
import org.lingolocal.project.util.logError
import org.lingolocal.project.util.logInfo

/**
 * Implementazione concreta del ModelRepository.
 * Utilizza Ktor per il download HTTP e FileStorage per la persistenza.
 * Tutta la logica I/O è spostata su Dispatchers.IO.
 */
class ModelRepositoryImpl(
    private val httpClient: HttpClient,
    private val fileStorage: FileStorage
) : ModelRepository {

    companion object {
        private const val TAG = "ModelRepositoryImpl"
    }

    override fun downloadModel(url: String, fileName: String): Flow<DownloadProgress> = flow {
        try {
            val destPath = "${fileStorage.getModelsDirectory()}/$fileName"
            logDebug(TAG, "Avvio download: url=$url, destPath=$destPath")

            emit(DownloadProgress.Downloading(0, -1))

            logDebug(TAG, "Invio richiesta HTTP GET...")
            val response = httpClient.get(url)
            val totalBytes = response.contentLength() ?: -1L
            logDebug(TAG, "Risposta ricevuta. Content-Length: $totalBytes")

            emit(DownloadProgress.Downloading(0, totalBytes))

            logDebug(TAG, "Lettura body in bytes...")
            val bytes = response.bodyAsBytes()
            logDebug(TAG, "Body letto: ${bytes.size} bytes")

            emit(DownloadProgress.Downloading(bytes.size.toLong(), totalBytes))

            logDebug(TAG, "Scrittura su disco: $destPath")
            fileStorage.writeFile(destPath) { write ->
                write(bytes, 0, bytes.size)
            }
            logDebug(TAG, "Scrittura completata")

            emit(DownloadProgress.Completed(destPath))
            logInfo(TAG, "Download completato con successo: $destPath")
        } catch (e: Exception) {
            logError(TAG, "Errore durante il download", e)
            emit(DownloadProgress.Error(e.message ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)

    override fun isModelDownloaded(fileName: String): Boolean {
        val path = "${fileStorage.getModelsDirectory()}/$fileName"
        val exists = fileStorage.fileExists(path)
        logDebug(TAG, "isModelDownloaded($fileName): $exists")
        return exists
    }

    override fun getModelPath(fileName: String): String? {
        val path = "${fileStorage.getModelsDirectory()}/$fileName"
        return if (fileStorage.fileExists(path)) path else null
    }

    override fun deleteModel(fileName: String): Boolean {
        val path = "${fileStorage.getModelsDirectory()}/$fileName"
        logDebug(TAG, "deleteModel: $path")
        return fileStorage.deleteFile(path)
    }
}
