package org.lingolocal.project.presentation.util

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun resizeAndCompressImage(
    imageBytes: ByteArray,
    maxDimension: Int,
    compressionQuality: Int
): ByteArray {
    try {
        if (imageBytes.isEmpty()) return imageBytes
        
        val nsData = imageBytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = imageBytes.size.toULong())
        }
        val image = UIImage.imageWithData(nsData) ?: return imageBytes
        
        val size = image.size
        val width = size.useContents { width }
        val height = size.useContents { height }
        
        if (width <= maxDimension && height <= maxDimension) {
            val jpegData = UIImageJPEGRepresentation(image, compressionQuality / 100.0) ?: return imageBytes
            return jpegData.toByteArray()
        }
        
        val ratio = width / height
        val (targetWidth, targetHeight) = if (width > height) {
            Pair(maxDimension.toDouble(), maxDimension.toDouble() / ratio)
        } else {
            Pair(maxDimension.toDouble() * ratio, maxDimension.toDouble())
        }
        
        UIGraphicsBeginImageContextWithOptions(
            size = CGSizeMake(targetWidth, targetHeight),
            opaque = false,
            scale = 1.0
        )
        image.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
        val resizedImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        
        if (resizedImage == null) return imageBytes
        
        val jpegData = UIImageJPEGRepresentation(resizedImage, compressionQuality / 100.0) ?: return imageBytes
        return jpegData.toByteArray()
    } catch (e: Exception) {
        return imageBytes
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    val byteArray = ByteArray(size)
    if (size > 0) {
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
    }
    return byteArray
}
