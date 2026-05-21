package org.lingolocal.project.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberAudioPermissionLauncher(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: (String) -> Unit
): AudioPermissionLauncher {
    return remember {
        object : AudioPermissionLauncher {
            override fun hasPermission(): Boolean = true
            override fun requestPermission() {
                onPermissionGranted()
            }
        }
    }
}
