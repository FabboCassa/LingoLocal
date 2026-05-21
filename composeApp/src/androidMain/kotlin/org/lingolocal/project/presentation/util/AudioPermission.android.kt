package org.lingolocal.project.presentation.util

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun rememberAudioPermissionLauncher(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: (String) -> Unit
): AudioPermissionLauncher {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onPermissionGranted()
        } else {
            onPermissionDenied("Permesso microfono negato. Abilitalo nelle impostazioni per usare la voce.")
        }
    }

    return remember(permissionLauncher, context) {
        object : AudioPermissionLauncher {
            override fun hasPermission(): Boolean {
                return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            }

            override fun requestPermission() {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}
