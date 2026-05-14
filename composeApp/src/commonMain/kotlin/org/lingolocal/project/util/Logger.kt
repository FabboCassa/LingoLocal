package org.lingolocal.project.util

/**
 * Utility di logging multipiattaforma.
 * Ogni piattaforma fornirà la sua implementazione (Android -> Log.d/e/w).
 */
expect fun logDebug(tag: String, message: String)
expect fun logError(tag: String, message: String, throwable: Throwable? = null)
expect fun logWarn(tag: String, message: String)
expect fun logInfo(tag: String, message: String)
