package org.lingolocal.project.presentation.llamatest

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
actual fun FilePickerButton(
    onPathSelected: (String) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCopying by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            isCopying = true
            val path = copyUriToModelsDir(context.contentResolver, uri, context.filesDir)
            isCopying = false
            if (path != null) onPathSelected(path)
        }
    }

    Button(
        onClick = { launcher.launch(arrayOf("*/*")) },
        enabled = !isCopying,
        modifier = modifier
    ) {
        Text(if (isCopying) "Copiando…" else "Sfoglia")
    }
}

private suspend fun copyUriToModelsDir(
    resolver: ContentResolver,
    uri: Uri,
    filesDir: File
): String? = withContext(Dispatchers.IO) {
    val fileName = queryFileName(resolver, uri) ?: "model.gguf"
    val modelsDir = File(filesDir, "models").also { it.mkdirs() }
    val dest = File(modelsDir, fileName)
    if (dest.exists()) return@withContext dest.absolutePath
    try {
        resolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { input.copyTo(it) }
        }
        dest.absolutePath
    } catch (e: Exception) {
        null
    }
}

private fun queryFileName(resolver: ContentResolver, uri: Uri): String? {
    resolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) return cursor.getString(idx)
        }
    }
    return uri.lastPathSegment
}
