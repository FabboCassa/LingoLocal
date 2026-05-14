package org.lingolocal.project.presentation.modelmanager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import lingolocal.composeapp.generated.resources.Res
import lingolocal.composeapp.generated.resources.download_model
import lingolocal.composeapp.generated.resources.download_complete
import lingolocal.composeapp.generated.resources.download_error
import lingolocal.composeapp.generated.resources.download_progress
import lingolocal.composeapp.generated.resources.download_ready
import lingolocal.composeapp.generated.resources.model_already_available
import org.jetbrains.compose.resources.stringResource
import org.lingolocal.project.domain.model.DownloadProgress

/**
 * Schermata per il download e la gestione dei modelli AI.
 * View pura: nessuna logica, solo rendering basato sullo stato.
 */
class ModelManagerScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = getScreenModel<ModelManagerScreenModel>()
        val uiState by screenModel.uiState.collectAsState()

        ModelManagerContent(
            uiState = uiState,
            onDownloadClick = screenModel::startDownload
        )
    }
}

@Composable
private fun ModelManagerContent(
    uiState: ModelManagerUiState,
    onDownloadClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val progress = uiState.downloadProgress) {
                is DownloadProgress.Idle -> {
                    if (uiState.isModelAvailable) {
                        Text(
                            text = stringResource(Res.string.model_already_available),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.download_ready),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(onClick = onDownloadClick) {
                            Text(stringResource(Res.string.download_model))
                        }
                    }
                }

                is DownloadProgress.Downloading -> {
                    Text(
                        text = stringResource(
                            Res.string.download_progress,
                            (progress.progressPercent * 100).toInt()
                        ),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress.progressPercent },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is DownloadProgress.Completed -> {
                    Text(
                        text = stringResource(Res.string.download_complete),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is DownloadProgress.Error -> {
                    Text(
                        text = stringResource(Res.string.download_error, progress.message),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onDownloadClick) {
                        Text(stringResource(Res.string.download_model))
                    }
                }
            }
        }
    }
}
