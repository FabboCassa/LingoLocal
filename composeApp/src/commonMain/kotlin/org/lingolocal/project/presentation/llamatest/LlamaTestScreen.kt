package org.lingolocal.project.presentation.llamatest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import lingolocal.composeapp.generated.resources.Res
import lingolocal.composeapp.generated.resources.llama_generate_button
import lingolocal.composeapp.generated.resources.llama_load_button
import lingolocal.composeapp.generated.resources.llama_model_path_hint
import lingolocal.composeapp.generated.resources.llama_prompt_hint
import lingolocal.composeapp.generated.resources.llama_status_error
import lingolocal.composeapp.generated.resources.llama_status_generating
import lingolocal.composeapp.generated.resources.llama_status_idle
import lingolocal.composeapp.generated.resources.llama_status_initializing
import lingolocal.composeapp.generated.resources.llama_status_load_failed
import lingolocal.composeapp.generated.resources.llama_status_loaded
import lingolocal.composeapp.generated.resources.llama_status_loading
import lingolocal.composeapp.generated.resources.llama_test_title
import lingolocal.composeapp.generated.resources.llama_unload_button
import org.jetbrains.compose.resources.stringResource

/**
 * Schermata di test per la pipeline di inferenza LLM.
 * Permette di caricare un GGUF, inviare un prompt e visualizzare
 * il testo generato in streaming token-per-token.
 */
class LlamaTestScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = getScreenModel<LlamaTestScreenModel>()
        val uiState by screenModel.uiState.collectAsState()

        LlamaTestContent(
            uiState = uiState,
            onModelPathChange = screenModel::onModelPathChange,
            onPromptChange = screenModel::onPromptChange,
            onLoadClick = screenModel::onLoadClick,
            onGenerateClick = screenModel::onGenerateClick,
            onUnloadClick = screenModel::onUnloadClick
        )
    }
}

@Composable
private fun LlamaTestContent(
    uiState: LlamaTestUiState,
    onModelPathChange: (String) -> Unit,
    onPromptChange: (String) -> Unit,
    onLoadClick: () -> Unit,
    onGenerateClick: () -> Unit,
    onUnloadClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(Res.string.llama_test_title),
            style = MaterialTheme.typography.headlineSmall
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = uiState.modelPathInput,
                onValueChange = onModelPathChange,
                label = { Text(stringResource(Res.string.llama_model_path_hint)) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            FilePickerButton(
                onPathSelected = onModelPathChange,
                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onLoadClick,
                enabled = uiState.isLoadEnabled && uiState.modelPathInput.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(Res.string.llama_load_button))
            }
            OutlinedButton(
                onClick = onUnloadClick,
                enabled = uiState.isUnloadEnabled,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(Res.string.llama_unload_button))
            }
        }

        StatusBanner(uiState)

        OutlinedTextField(
            value = uiState.promptInput,
            onValueChange = onPromptChange,
            label = { Text(stringResource(Res.string.llama_prompt_hint)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp)
        )

        Button(
            onClick = onGenerateClick,
            enabled = uiState.isGenerateEnabled && uiState.promptInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.llama_generate_button))
        }

        if (uiState.phase == LlamaTestUiState.Phase.Generating ||
            uiState.phase == LlamaTestUiState.Phase.Loading ||
            uiState.phase == LlamaTestUiState.Phase.Initializing
        ) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (uiState.generatedText.isNotEmpty()) {
            Spacer(Modifier.fillMaxWidth())
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = uiState.generatedText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusBanner(uiState: LlamaTestUiState) {
    val (text, color) = when (uiState.phase) {
        LlamaTestUiState.Phase.Idle ->
            stringResource(Res.string.llama_status_idle) to MaterialTheme.colorScheme.onSurfaceVariant
        LlamaTestUiState.Phase.Initializing ->
            stringResource(Res.string.llama_status_initializing) to MaterialTheme.colorScheme.primary
        LlamaTestUiState.Phase.Loading ->
            stringResource(Res.string.llama_status_loading) to MaterialTheme.colorScheme.primary
        LlamaTestUiState.Phase.Loaded ->
            stringResource(Res.string.llama_status_loaded) to MaterialTheme.colorScheme.primary
        LlamaTestUiState.Phase.Generating ->
            stringResource(Res.string.llama_status_generating) to MaterialTheme.colorScheme.primary
        LlamaTestUiState.Phase.LoadFailed ->
            stringResource(Res.string.llama_status_load_failed) to MaterialTheme.colorScheme.error
        LlamaTestUiState.Phase.Error ->
            stringResource(
                Res.string.llama_status_error,
                uiState.errorMessage ?: "?"
            ) to MaterialTheme.colorScheme.error
    }
    Text(text = text, color = color, style = MaterialTheme.typography.bodyMedium)
}
