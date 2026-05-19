package org.lingolocal.project.presentation.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

actual fun resizeAndCompressImage(
    imageBytes: ByteArray,
    maxDimension: Int,
    compressionQuality: Int
): ByteArray {
    try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
        }
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options) ?: return imageBytes
        
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxDimension && height <= maxDimension) {
            // Nessun ridimensionamento necessario, ma comprimiamo comunque con la qualità richiesta per ridurre il peso
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, outputStream)
            val compressedBytes = outputStream.toByteArray()
            bitmap.recycle()
            return compressedBytes
        }
        
        val ratio = width.toFloat() / height.toFloat()
        val (targetWidth, targetHeight) = if (width > height) {
            Pair(maxDimension, (maxDimension / ratio).toInt())
        } else {
            Pair((maxDimension * ratio).toInt(), maxDimension)
        }
        
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, outputStream)
        val processedBytes = outputStream.toByteArray()
        
        bitmap.recycle()
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        
        return processedBytes
    } catch (e: Exception) {
        e.printStackTrace()
        return imageBytes
    }
}
