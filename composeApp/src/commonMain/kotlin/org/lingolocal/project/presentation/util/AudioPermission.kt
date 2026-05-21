package org.lingolocal.project.presentation.util

import androidx.compose.runtime.Composable

/**
 * Interface to manage microphone permissions across platforms dynamically in Compose UI.
 */
interface AudioPermissionLauncher {
    /**
     * Checks if the microphone permission is already granted.
     */
    fun hasPermission(): Boolean

    /**
     * Triggers the system's dynamic runtime permission request dialog.
     */
    fun requestPermission()
}

/**
 * Composable expectation to remember and configure an AudioPermissionLauncher.
 */
@Composable
expect fun rememberAudioPermissionLauncher(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: (String) -> Unit
): AudioPermissionLauncher
