package org.lingolocal.project.presentation.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import org.lingolocal.project.util.logError
import org.lingolocal.project.util.logInfo
import java.io.File

private const val TAG = "ImagePickerAndroid"

@Composable
actual fun rememberImagePicker(
    onImagePicked: (ByteArray?) -> Unit,
    onPermissionDenied: (String) -> Unit
): ImagePickerLauncher {
    val context = LocalContext.current

    // File temporaneo e Uri per contenere lo scatto a piena risoluzione
    val (tempFile, photoUri) = remember(context) {
        try {
            val file = File(context.cacheDir, "temp_camera_capture.jpg")
            if (file.exists()) file.delete()
            file.createNewFile()
            val uri = FileProvider.getUriForFile(
                context,
                "org.lingolocal.project.fileprovider",
                file
            )
            file to uri
        } catch (e: Exception) {
            logError(TAG, "Impossibile creare il file temporaneo per lo scatto", e)
            null to null
        }
    }

    // Launcher per la Galleria (Permissionless)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bytes = readBytesFromUri(context, uri)
            onImagePicked(bytes)
        } else {
            onImagePicked(null)
        }
    }

    // Launcher per scattare la foto
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempFile != null && tempFile.exists() && tempFile.length() > 0) {
            val bytes = tempFile.readBytes()
            logInfo(TAG, "Foto acquisita con successo: ${bytes.size} byte")
            onImagePicked(bytes)
        } else {
            onImagePicked(null)
        }
    }

    // Launcher per richiedere il permesso della Fotocamera
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (photoUri != null) {
                cameraLauncher.launch(photoUri)
            } else {
                onPermissionDenied("Impossibile avviare la fotocamera (errore inizializzazione file)")
            }
        } else {
            onPermissionDenied("Permesso fotocamera negato")
        }
    }

    return remember(galleryLauncher, cameraLauncher, permissionLauncher, photoUri) {
        object : ImagePickerLauncher {
            override fun launchGallery() {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }

            override fun launchCamera() {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    if (photoUri != null) {
                        cameraLauncher.launch(photoUri)
                    } else {
                        onPermissionDenied("Impossibile avviare la fotocamera (errore inizializzazione file)")
                    }
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }
}

private fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        }
    } catch (e: Exception) {
        logError(TAG, "Errore nella lettura dei byte dall'Uri della galleria: $uri", e)
        null
    }
}
