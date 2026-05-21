package org.lingolocal.project.presentation.modelmanager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import org.lingolocal.project.domain.model.DownloadProgress
import org.lingolocal.project.presentation.llamatest.FilePickerButton

/**
 * Schermata premium per il Model Manager di LingoLocal.
 * Offre un'esperienza utente raffinata con colori armoniosi, animazioni,
 * download in streaming con percentuali, RAM loading e supporto all'importazione di modelli GGUF esterni.
 */
class ModelManagerScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = getScreenModel<ModelManagerScreenModel>()
        val uiState by screenModel.uiState.collectAsState()

        // Forza un rinfresco all'apertura per allineare gli stati
        LaunchedEffect(Unit) {
            screenModel.refreshModelStates()
        }

        ModelManagerContent(
            uiState = uiState,
            onDownloadClick = screenModel::downloadModel,
            onDeleteClick = screenModel::deleteModel,
            onActivateClick = screenModel::activateModel,
            onFileSelected = screenModel::importExternalModel
        )
    }
}

@Composable
private fun ModelManagerContent(
    uiState: ModelManagerUiState,
    onDownloadClick: (AiModelInfo) -> Unit,
    onDeleteClick: (AiModelInfo) -> Unit,
    onActivateClick: (AiModelInfo) -> Unit,
    onFileSelected: (String) -> Unit
) {
    // Gradienti di background moderni per un look ultra-premium
    val gradientBg = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBg)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Header Schermata
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text(
                        text = "Gestione Modelli AI 🧠",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Configura l'intelligenza locale di LingoLocal. I modelli scaricati vengono eseguiti al 100% offline sul tuo dispositivo, garantendo privacy assoluta e zero costi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        lineHeight = 20.sp
                    )
                }
            }

            // 2. Dynamic Hardware Scan Banner
            val hw = uiState.hardwareInfo
            if (hw != null) {
                item {
                    val ramGbRounded = ((hw.totalRamGb * 10).toInt() / 10.0).toString()
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.5.dp,
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "📱",
                                fontSize = 28.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Rilevamento Hardware Dinamico ✨",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Dispositivo Rilevato: ${hw.deviceName}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Memoria RAM Fisica: $ramGbRounded GB RAM",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = hw.performanceTierLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Errore globale (se presente)
            if (uiState.errorMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // 3. Titolo sezione cataloghi
            item {
                Text(
                    text = "Modelli Disponibili nel Catalogo",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 4. Lista dei modelli del catalogo e importati
            items(uiState.models) { model ->
                ModelCard(
                    model = model,
                    onDownloadClick = { onDownloadClick(model) },
                    onDeleteClick = { onDeleteClick(model) },
                    onActivateClick = { onActivateClick(model) }
                )
            }

            // 5. Sezione Importazione Modello Esterno
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Importa Modello GGUF Esterno 📁",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Puoi caricare un modello GGUF scaricato da fonti esterne (es. HuggingFace). Verrà copiato nella cartella protetta dell'app per essere usato offline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        FilePickerButton(
                            onPathSelected = onFileSelected,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        )
                    }
                }
            }

            // 6. Storage & Disk Information Card
            if (uiState.modelsDirectory.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("💾", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Archiviazione Locale & Privacy",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "I modelli offline vengono scaricati nella memoria privata e protetta (sandbox) dell'applicazione:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = uiState.modelsDirectory,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "🔒 Sicurezza e Trasparenza:\n" +
                                        "• Questa cartella è inaccessibile ad altre app o file manager esterni per garantire sicurezza totale.\n" +
                                        "• Disinstallando un modello o eliminando un file parziale, lo spazio viene liberato fisicamente ed immediatamente dal tuo telefono.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelCard(
    model: AiModelInfo,
    onDownloadClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onActivateClick: () -> Unit
) {
    // Animazione di pulsazione soft per il modello attivo
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val borderBrush = when {
        model.isActive -> Brush.sweepGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary))
        model.isRecommended -> Brush.horizontalGradient(listOf(Color(0xFFF1C40F), Color(0xFFE67E22)))
        model.isExternal -> Brush.sweepGradient(listOf(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f), MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)))
        else -> Brush.sweepGradient(listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outlineVariant))
    }
    val borderWidth = when {
        model.isActive -> 2.dp
        model.isRecommended -> 2.dp
        else -> 1.dp
    }
    val containerColor = when {
        model.isActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        model.isRecommended -> Color(0xFFF1C40F).copy(alpha = 0.04f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = borderWidth,
                brush = borderBrush,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            if (model.isRecommended) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFF1C40F).copy(alpha = 0.15f),
                                    Color(0xFFE67E22).copy(alpha = 0.15f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(listOf(Color(0xFFF1C40F), Color(0xFFE67E22))),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⭐ CONSIGLIATO PER IL TUO TELEFONO",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color(0xFFD35400)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            // Intestazione Card: Nome Modello + Badge Dimensione/Esterno
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                
                // Badge Modello Esterno o Dimensione
                val badgeBg = if (model.isExternal) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
                val badgeColor = if (model.isExternal) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                val badgeText = if (model.isExternal) "Esterno" else model.sizeLabel

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Descrizione
            Text(
                text = model.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sezione Stato Download & Azioni
            when {
                // 1. STATO: Modello Attivo in RAM
                model.isActive -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge Attivo Pulsante
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF4CAF50).copy(alpha = pulseAlpha))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Modello Attivo in RAM",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF2E7D32)
                            )
                        }

                        // Bottone Rimuovi/Disinstalla
                        IconButton(onClick = onDeleteClick) {
                            Text(
                                text = "🗑️",
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                // 2. STATO: Caricamento in RAM in corso
                model.isLoading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Caricamento in RAM...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 3. STATO: Modello Scaricato (ma non attivo)
                model.isDownloaded -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bottone Attiva in RAM
                        Button(
                            onClick = onActivateClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Attiva in RAM 🧠", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))

                        // Bottone Disinstalla
                        OutlinedButton(
                            onClick = onDeleteClick,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.sweepGradient(listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error)))
                        ) {
                            Text("🗑️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Disinstalla", fontSize = 13.sp)
                        }
                    }
                }

                // 3.5 STATO: Download parziale interrotto (da riprendere)
                model.isPartial && !model.isDownloadingActive -> {
                    val bytesDownloaded = model.downloadedBytes
                    val totalBytes = model.totalBytes
                    val pct = if (totalBytes > 0L) (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

                    val downloadedMb = bytesDownloaded / (1024 * 1024)
                    val totalMb = totalBytes / (1024 * 1024)
                    val remainingMb = ((totalBytes - bytesDownloaded) / (1024 * 1024)).coerceAtLeast(0L)

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚠️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Download parziale interrotto: $downloadedMb MB di $totalMb MB (${(pct * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { pct },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.tertiary,
                            trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onDownloadClick,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Riprendi Download 🔄 ($remainingMb MB mancanti)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            OutlinedButton(
                                onClick = onDeleteClick,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.sweepGradient(listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error)))
                            ) {
                                Text("🗑️", fontSize = 14.sp)
                            }
                        }
                    }
                }

                // 4. STATO: Download in corso
                model.downloadProgress is DownloadProgress.Downloading -> {
                    val progress = model.downloadProgress as DownloadProgress.Downloading
                    val pct = progress.progressPercent
                    val bytesDownloaded = progress.bytesDownloaded
                    val totalBytes = progress.totalBytes

                    val downloadedMb = bytesDownloaded / (1024 * 1024)
                    val totalMb = totalBytes / (1024 * 1024)
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Scaricati $downloadedMb MB di $totalMb MB...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${(pct * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { pct },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    }
                }

                // 5. STATO: Errore Download
                model.downloadProgress is DownloadProgress.Error -> {
                    val errorMsg = (model.downloadProgress as DownloadProgress.Error).message
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Errore di download: $errorMsg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onDownloadClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Riprova Download", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 6. STATO: Non scaricato (Idle)
                else -> {
                    Button(
                        onClick = onDownloadClick,
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Scarica Modello 📥", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
