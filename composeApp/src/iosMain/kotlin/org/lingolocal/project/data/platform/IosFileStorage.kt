package org.lingolocal.project.data.platform

import platform.Foundation.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

/**
 * Implementazione iOS di FileStorage per KMP.
 * Utilizza NSFileManager per gestire i file internamente alla sandbox iOS.
 */
@OptIn(ExperimentalForeignApi::class)
class IosFileStorage : FileStorage {

    override fun getModelsDirectory(): String {
        val paths = NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
        val documentsUrl = paths.firstOrNull() as? NSURL
        val modelsPath = (documentsUrl?.path ?: "") + "/models"
        
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(modelsPath)) {
            fileManager.createDirectoryAtPath(modelsPath, withIntermediateDirectories = true, attributes = null, error = null)
        }
        return modelsPath
    }

    override fun fileExists(filePath: String): Boolean {
        return NSFileManager.defaultManager.fileExistsAtPath(filePath)
    }

    override fun getFileSize(filePath: String): Long {
        val fileManager = NSFileManager.defaultManager
        val attributes = fileManager.attributesOfItemAtPath(filePath, error = null)
        return (attributes?.get(NSFileSize) as? NSNumber)?.longValue ?: 0L
    }

    override fun deleteFile(filePath: String): Boolean {
        return NSFileManager.defaultManager.removeItemAtPath(filePath, error = null)
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun writeFile(
        filePath: String,
        append: Boolean,
        writer: suspend (write: (ByteArray, Int, Int) -> Unit) -> Unit
    ) {
        val allBytes = mutableListOf<Byte>()
        if (append && fileExists(filePath)) {
            val fileManager = NSFileManager.defaultManager
            val data = fileManager.contentsAtPath(filePath)
            if (data != null) {
                val bytes = ByteArray(data.length.toInt())
                if (bytes.isNotEmpty()) {
                    data.bytes?.let { ptr ->
                        bytes.usePinned { pinned ->
                            platform.posix.memcpy(pinned.addressOf(0), ptr, data.length)
                        }
                    }
                    allBytes.addAll(bytes.toList())
                }
            }
        }

        writer { buffer, offset, length ->
            for (i in offset until (offset + length)) {
                allBytes.add(buffer[i])
            }
        }
        val byteArray = allBytes.toByteArray()
        if (byteArray.isEmpty()) {
            NSData().writeToFile(filePath, atomically = true)
            return
        }
        val nsData = byteArray.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), byteArray.size.toULong())
        }
        nsData.writeToFile(filePath, atomically = true)
    }
}
