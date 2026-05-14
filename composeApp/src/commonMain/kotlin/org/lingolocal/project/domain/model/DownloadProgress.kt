package org.lingolocal.project.domain.model

/**
 * Rappresenta lo stato di avanzamento di un download.
 * Sealed class per gestire in modo type-safe tutti i possibili stati.
 */
sealed class DownloadProgress {
    /** Download non ancora avviato. */
    data object Idle : DownloadProgress()

    /** Download in corso con progressione percentuale. */
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : DownloadProgress() {
        val progressPercent: Float
            get() = if (totalBytes > 0) {
                (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
            } else 0f
    }

    /** Download completato con successo. */
    data class Completed(val filePath: String) : DownloadProgress()

    /** Download fallito. */
    data class Error(val message: String) : DownloadProgress()
}
