package org.lingolocal.project.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import lingolocal.composeapp.generated.resources.Res
import lingolocal.composeapp.generated.resources.settings_dev_section
import lingolocal.composeapp.generated.resources.settings_open_llama_test
import lingolocal.composeapp.generated.resources.settings_theme_dark
import lingolocal.composeapp.generated.resources.settings_theme_label
import lingolocal.composeapp.generated.resources.settings_theme_light
import lingolocal.composeapp.generated.resources.settings_theme_section
import lingolocal.composeapp.generated.resources.settings_theme_system
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.lingolocal.project.domain.model.ThemeMode
import org.lingolocal.project.presentation.llamatest.LlamaTestScreen
import org.lingolocal.project.presentation.theme.LingoIcons

/**
 * Schermata Impostazioni.
 * - Sezione "Aspetto": selezione tema (Sistema / Chiaro / Scuro).
 * - Sezione "Sviluppo": accesso allo schermo di test inferenza LLM.
 */
class SettingsScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = getScreenModel<SettingsScreenModel>()
        val uiState by screenModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        SettingsContent(
            uiState = uiState,
            onThemeSelected = screenModel::onThemeSelected,
            onOpenLlamaTest = { navigator.push(LlamaTestScreen()) }
        )
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onThemeSelected: (ThemeMode) -> Unit,
    onOpenLlamaTest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionHeader(Res.string.settings_theme_section)
        ThemeOption(ThemeMode.SYSTEM, uiState.themeMode, Res.string.settings_theme_system, onThemeSelected)
        ThemeOption(ThemeMode.LIGHT, uiState.themeMode, Res.string.settings_theme_light, onThemeSelected)
        ThemeOption(ThemeMode.DARK, uiState.themeMode, Res.string.settings_theme_dark, onThemeSelected)

        Spacer(modifier = Modifier.size(16.dp))

        SectionHeader(Res.string.settings_dev_section)
        ListItem(
            headlineContent = { Text(stringResource(Res.string.settings_open_llama_test)) },
            leadingContent = {
                Icon(
                    imageVector = LingoIcons.Science,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingContent = {
                Icon(
                    imageVector = LingoIcons.KeyboardArrowRight,
                    contentDescription = null
                )
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenLlamaTest)
        )
    }
}

@Composable
private fun SectionHeader(label: StringResource) {
    Text(
        text = stringResource(label),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun ThemeOption(
    mode: ThemeMode,
    selected: ThemeMode,
    label: StringResource,
    onSelected: (ThemeMode) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected(mode) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        RadioButton(
            selected = mode == selected,
            onClick = { onSelected(mode) }
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
