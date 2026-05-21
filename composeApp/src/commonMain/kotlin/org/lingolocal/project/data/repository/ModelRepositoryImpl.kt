package org.lingolocal.project.data.repository

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
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
    private val fileStorage: FileStorage,
    private val settings: Settings
) : ModelRepository {

    companion object {
        private const val TAG = "ModelRepositoryImpl"
    }

    override fun downloadModel(url: String, fileName: String): Flow<DownloadProgress> = flow {
        try {
            val destPath = "${fileStorage.getModelsDirectory()}/$fileName"
            logDebug(TAG, "Avvio download: url=$url, destPath=$destPath")

            // Controlla se c'è un file parziale per riprendere il download
            val existingBytes = if (fileStorage.fileExists(destPath) && !settings.getBoolean("completed_$fileName", false)) {
                fileStorage.getFileSize(destPath)
            } else {
                0L
            }

            emit(DownloadProgress.Downloading(existingBytes, -1))

            logDebug(TAG, "Invio richiesta HTTP GET con streaming (existingBytes=$existingBytes)...")
            httpClient.prepareGet(url) {
                if (existingBytes > 0) {
                    header(io.ktor.http.HttpHeaders.Range, "bytes=$existingBytes-")
                }
            }.execute { response ->
                val responseLength = response.contentLength() ?: -1L
                val isPartial = response.status == io.ktor.http.HttpStatusCode.PartialContent
                
                val totalBytes = if (existingBytes > 0 && isPartial) {
                    responseLength + existingBytes
                } else {
                    responseLength
                }

                logDebug(TAG, "Risposta ricevuta. Status=${response.status}, Content-Length: $responseLength, totalBytes: $totalBytes")

                val useAppend = existingBytes > 0 && isPartial
                val startOffset = if (useAppend) existingBytes else 0L

                emit(DownloadProgress.Downloading(startOffset, totalBytes))

                val channel = response.body<io.ktor.utils.io.ByteReadChannel>()
                val buffer = ByteArray(8192)
                var bytesRead = startOffset

                logDebug(TAG, "Scrittura in corso su disco (append=$useAppend): $destPath")
                fileStorage.writeFile(destPath, append = useAppend) { write ->
                    while (!channel.isClosedForRead) {
                        val read = channel.readAvailable(buffer, 0, buffer.size)
                        if (read <= 0) break
                        write(buffer, 0, read)
                        bytesRead += read
                        emit(DownloadProgress.Downloading(bytesRead, totalBytes))
                    }
                }

                if (totalBytes > 0 && bytesRead < totalBytes) {
                    throw IllegalStateException("Download incompleto: scaricati solo $bytesRead di $totalBytes byte.")
                }
            }

            settings.putBoolean("completed_$fileName", true)
            emit(DownloadProgress.Completed(destPath))
            logInfo(TAG, "Download completato con successo e registrato completed: $destPath")
        } catch (e: Exception) {
            logError(TAG, "Errore durante il download", e)
            emit(DownloadProgress.Error(e.message ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)

    override fun isModelDownloaded(fileName: String): Boolean {
        val path = "${fileStorage.getModelsDirectory()}/$fileName"
        val exists = fileStorage.fileExists(path)
        val completed = settings.getBoolean("completed_$fileName", false)
        logDebug(TAG, "isModelDownloaded($fileName): exists=$exists, completed=$completed")
        return exists && completed
    }

    override fun getModelPath(fileName: String): String? {
        val path = "${fileStorage.getModelsDirectory()}/$fileName"
        return if (isModelDownloaded(fileName)) path else null
    }

    override fun deleteModel(fileName: String): Boolean {
        val path = "${fileStorage.getModelsDirectory()}/$fileName"
        logDebug(TAG, "deleteModel: $path")
        val deleted = fileStorage.deleteFile(path)
        if (deleted) {
            settings.remove("completed_$fileName")
            logInfo(TAG, "File $fileName rimosso con successo e rimosso il flag completed.")
        }
        return deleted
    }

    override fun getDownloadedBytes(fileName: String): Long {
        val path = "${fileStorage.getModelsDirectory()}/$fileName"
        return fileStorage.getFileSize(path)
    }

    override fun isModelPartial(fileName: String): Boolean {
        val path = "${fileStorage.getModelsDirectory()}/$fileName"
        val exists = fileStorage.fileExists(path)
        val completed = settings.getBoolean("completed_$fileName", false)
        return exists && !completed
    }
}
