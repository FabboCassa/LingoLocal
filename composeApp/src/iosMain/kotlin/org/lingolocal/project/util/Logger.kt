package org.lingolocal.project.util

import platform.Foundation.NSLog

/**
 * Implementazione iOS del Logger.
 * Utilizza NSLog per stampare i log sulla console.
 */
actual fun logDebug(tag: String, message: String) {
    NSLog("DEBUG [$tag] $message")
}

actual fun logError(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) {
        NSLog("ERROR [$tag] $message - ${throwable.message}")
        throwable.printStackTrace()
    } else {
        NSLog("ERROR [$tag] $message")
    }
}

actual fun logWarn(tag: String, message: String) {
    NSLog("WARN [$tag] $message")
}

actual fun logInfo(tag: String, message: String) {
    NSLog("INFO [$tag] $message")
}
