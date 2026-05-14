package org.lingolocal.project.data.platform

import android.content.Context
import java.io.File
import java.io.FileOutputStream

/**
 * Implementazione Android di FileStorage.
 * Utilizza il context Android per accedere alla directory interna dell'app.
 */
class AndroidFileStorage(
    private val context: Context
) : FileStorage {

    override fun getModelsDirectory(): String {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        return modelsDir.absolutePath
    }

    override fun fileExists(filePath: String): Boolean {
        return File(filePath).exists()
    }

    override fun getFileSize(filePath: String): Long {
        val file = File(filePath)
        return if (file.exists()) file.length() else 0L
    }

    override fun deleteFile(filePath: String): Boolean {
        return File(filePath).delete()
    }

    override suspend fun writeFile(
        filePath: String,
        writer: suspend (write: (ByteArray, Int, Int) -> Unit) -> Unit
    ) {
        val file = File(filePath)
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { fos ->
            writer { buffer, offset, length ->
                fos.write(buffer, offset, length)
            }
        }
    }
}
