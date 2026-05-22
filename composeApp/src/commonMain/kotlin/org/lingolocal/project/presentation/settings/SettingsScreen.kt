package org.lingolocal.project.presentation.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import lingolocal.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.lingolocal.project.domain.model.AppLanguage
import org.lingolocal.project.domain.model.ThemeMode
import org.lingolocal.project.presentation.theme.LingoIcons

/**
 * Schermata Impostazioni.
 * - Sezione "Aspetto": selezione tema (Sistema / Chiaro / Scuro) con accordion.
 * - Sezione "Lingua": selezione lingua dell'app con accordion.
 */
class SettingsScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = getScreenModel<SettingsScreenModel>()
        val uiState by screenModel.uiState.collectAsState()

        SettingsContent(
            uiState = uiState,
            onThemeSelected = screenModel::onThemeSelected,
            onLanguageSelected = screenModel::onLanguageSelected
        )
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onThemeSelected: (ThemeMode) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    var themeExpanded by remember { mutableStateOf(false) }
    var langExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sezione Aspetto (Tema)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(Res.string.settings_theme_section)
            
            val themeLabel = when (uiState.themeMode) {
                ThemeMode.SYSTEM -> stringResource(Res.string.settings_theme_system)
                ThemeMode.LIGHT -> stringResource(Res.string.settings_theme_light)
                ThemeMode.DARK -> stringResource(Res.string.settings_theme_dark)
            }

            SettingsAccordionCard(
                title = stringResource(Res.string.settings_theme_label),
                selectedValueLabel = themeLabel,
                isExpanded = themeExpanded,
                onHeaderClick = {
                    themeExpanded = !themeExpanded
                    if (themeExpanded) langExpanded = false // Chiude l'altro per ordine
                }
            ) {
                AccordionOptionRow(
                    isSelected = uiState.themeMode == ThemeMode.SYSTEM,
                    label = stringResource(Res.string.settings_theme_system),
                    onClick = {
                        onThemeSelected(ThemeMode.SYSTEM)
                        themeExpanded = false
                    }
                )
                AccordionOptionRow(
                    isSelected = uiState.themeMode == ThemeMode.LIGHT,
                    label = stringResource(Res.string.settings_theme_light),
                    onClick = {
                        onThemeSelected(ThemeMode.LIGHT)
                        themeExpanded = false
                    }
                )
                AccordionOptionRow(
                    isSelected = uiState.themeMode == ThemeMode.DARK,
                    label = stringResource(Res.string.settings_theme_dark),
                    onClick = {
                        onThemeSelected(ThemeMode.DARK)
                        themeExpanded = false
                    }
                )
            }
        }

        // Sezione Lingua
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(Res.string.settings_language_section)

            val langLabel = when (uiState.appLanguage) {
                AppLanguage.ITALIANO -> stringResource(Res.string.settings_lang_it)
                AppLanguage.ENGLISH -> stringResource(Res.string.settings_lang_en)
                AppLanguage.ESPANOL -> stringResource(Res.string.settings_lang_es)
                AppLanguage.FRANCAIS -> stringResource(Res.string.settings_lang_fr)
                AppLanguage.DEUTSCH -> stringResource(Res.string.settings_lang_de)
            }

            SettingsAccordionCard(
                title = stringResource(Res.string.settings_language_label),
                selectedValueLabel = langLabel,
                isExpanded = langExpanded,
                onHeaderClick = {
                    langExpanded = !langExpanded
                    if (langExpanded) themeExpanded = false // Chiude l'altro per ordine
                }
            ) {
                AccordionOptionRow(
                    isSelected = uiState.appLanguage == AppLanguage.ITALIANO,
                    label = stringResource(Res.string.settings_lang_it),
                    onClick = {
                        onLanguageSelected(AppLanguage.ITALIANO)
                        langExpanded = false
                    }
                )
                AccordionOptionRow(
                    isSelected = uiState.appLanguage == AppLanguage.ENGLISH,
                    label = stringResource(Res.string.settings_lang_en),
                    onClick = {
                        onLanguageSelected(AppLanguage.ENGLISH)
                        langExpanded = false
                    }
                )
                AccordionOptionRow(
                    isSelected = uiState.appLanguage == AppLanguage.ESPANOL,
                    label = stringResource(Res.string.settings_lang_es),
                    onClick = {
                        onLanguageSelected(AppLanguage.ESPANOL)
                        langExpanded = false
                    }
                )
                AccordionOptionRow(
                    isSelected = uiState.appLanguage == AppLanguage.FRANCAIS,
                    label = stringResource(Res.string.settings_lang_fr),
                    onClick = {
                        onLanguageSelected(AppLanguage.FRANCAIS)
                        langExpanded = false
                    }
                )
                AccordionOptionRow(
                    isSelected = uiState.appLanguage == AppLanguage.DEUTSCH,
                    label = stringResource(Res.string.settings_lang_de),
                    onClick = {
                        onLanguageSelected(AppLanguage.DEUTSCH)
                        langExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(label: StringResource) {
    Text(
        text = stringResource(label),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun SettingsAccordionCard(
    title: String,
    selectedValueLabel: String,
    isExpanded: Boolean,
    onHeaderClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(
                width = 1.dp,
                color = if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHeaderClick() }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = selectedValueLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = LingoIcons.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotationAngle),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    content()
                }
            }
        }
    }
}

@Composable
private fun AccordionOptionRow(
    isSelected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        if (isSelected) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = LingoIcons.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
