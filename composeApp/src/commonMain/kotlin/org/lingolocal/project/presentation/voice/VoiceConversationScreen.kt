package org.lingolocal.project.presentation.voice

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import lingolocal.composeapp.generated.resources.Res
import lingolocal.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.lingolocal.project.presentation.theme.LingoIcons
import org.lingolocal.project.presentation.util.rememberAudioPermissionLauncher

class VoiceConversationScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = getScreenModel<VoiceConversationScreenModel>()
        val uiState by screenModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        val audioPermissionLauncher = rememberAudioPermissionLauncher(
            onPermissionGranted = {
                // Permesso concesso, possiamo rimuovere eventuali messaggi d'errore passati
            },
            onPermissionDenied = { errMsg ->
                screenModel.onPermissionDenied(errMsg)
            }
        )

        // Richiede il permesso del microfono appena si entra nella schermata, come richiesto
        LaunchedEffect(Unit) {
            if (!audioPermissionLauncher.hasPermission()) {
                audioPermissionLauncher.requestPermission()
            }
        }

        VoiceConversationContent(
            uiState = uiState,
            onBackClick = { navigator.pop() },
            onStartRecording = {
                if (audioPermissionLauncher.hasPermission()) {
                    screenModel.startRecording()
                } else {
                    audioPermissionLauncher.requestPermission()
                }
            },
            onStopRecording = { screenModel.stopRecording() },
            onLanguageChange = { screenModel.changeLanguage(it) },
            onDownloadWhisper = { screenModel.downloadWhisperModel() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceConversationContent(
    uiState: VoiceConversationUiState,
    onBackClick: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onLanguageChange: (String) -> Unit,
    onDownloadWhisper: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Fa scorrere automaticamente la lista dei messaggi all'ultimo elemento quando si aggiunge un messaggio
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.voice_practice_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = LingoIcons.ArrowBack,
                            contentDescription = stringResource(Res.string.common_back)
                        )
                    }
                },
                actions = {
                    // Selettore Lingua Studio
                    var showLangMenu by remember { mutableStateOf(false) }
                    val currentLangName = when (uiState.studyLanguage.lowercase()) {
                        "it" -> "Italiano (IT)"
                        "es" -> "Español (ES)"
                        "fr" -> "Français (FR)"
                        "de" -> "Deutsch (DE)"
                        else -> "English (EN)"
                    }

                    Box {
                        TextButton(
                            onClick = { showLangMenu = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(currentLangName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        DropdownMenu(
                            expanded = showLangMenu,
                            onDismissRequest = { showLangMenu = false }
                        ) {
                            val langs = listOf(
                                "en" to "English (EN)",
                                "it" to "Italiano (IT)",
                                "es" to "Español (ES)",
                                "fr" to "Français (FR)",
                                "de" to "Deutsch (DE)"
                            )
                            langs.forEach { (code, name) ->
                                DropdownMenuItem(
                                    text = { Text(name, fontSize = 14.sp) },
                                    onClick = {
                                        onLanguageChange(code)
                                        showLangMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        )
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // 1. Spiegazione Didascalica
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = LingoIcons.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(Res.string.voice_practice_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            // 1.5 Banner Whisper: visibile finché il modello voce non è caricato.
            // Permette download + load del modello STT direttamente dalla chat,
            // senza obbligare l'utente a passare dal Model Manager.
            if (uiState.whisperMissing || uiState.whisperDownloadProgress != null || uiState.isWhisperLoading) {
                WhisperDownloadCard(
                    progress = uiState.whisperDownloadProgress,
                    isLoadingInRam = uiState.isWhisperLoading,
                    onDownloadClick = onDownloadWhisper
                )
            }

            // 2. Transcripts Chat Area (Scrolled)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            ) {
                if (uiState.messages.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.voice_practice_empty_transcript),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.messages) { message ->
                            ChatBubble(message = message)
                        }
                    }
                }
            }

            // 3. Status indicator & Error message
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = uiState.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = if (uiState.isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (uiState.isRecording) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Voice Wave Visualizer and Hold-to-Talk Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Sfondo ad onde concentriche quando registra, canta o pensa
                VoiceWaveVisualizer(
                    amplitude = uiState.micAmplitude,
                    isRecording = uiState.isRecording,
                    isProcessing = uiState.isTranscribing || uiState.isAnalyzing,
                    isSpeaking = uiState.isSpeaking
                )

                // Pulsante rotondo "Tieni Premuto per Parlare"
                val buttonBgColor = when {
                    uiState.isRecording -> MaterialTheme.colorScheme.error
                    uiState.isTranscribing || uiState.isAnalyzing -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    uiState.isSpeaking -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.primary
                }

                val buttonScale by animateFloatAsState(
                    targetValue = if (uiState.isRecording) 1.25f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                )

                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .shadow(12.dp, CircleShape)
                        .clip(CircleShape)
                        .background(buttonBgColor)
                        .border(4.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                        .pointerInput(uiState.isTranscribing, uiState.isAnalyzing) {
                            // Se sta già elaborando, blocchiamo le interazioni del microfono
                            if (uiState.isTranscribing || uiState.isAnalyzing) return@pointerInput
                            
                            detectTapGestures(
                                onPress = {
                                    try {
                                        onStartRecording()
                                        awaitRelease()
                                    } finally {
                                        onStopRecording()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when {
                        uiState.isRecording -> LingoIcons.Stop
                        uiState.isTranscribing || uiState.isAnalyzing -> LingoIcons.Settings // Simbolo loading rotante
                        uiState.isSpeaking -> LingoIcons.PlayArrow
                        else -> LingoIcons.Mic
                    }
                    
                    // Rotazione continua se sta calcolando
                    if (uiState.isTranscribing || uiState.isAnalyzing) {
                        val infiniteTransition = rememberInfiniteTransition()
                        val angle by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            )
                        )
                        Icon(
                            imageVector = LingoIcons.Settings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        // Icona Statica del microfono o stop
                        Icon(
                            imageVector = icon,
                            contentDescription = stringResource(Res.string.voice_practice_hold_to_talk),
                            tint = if (uiState.isSpeaking) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = if (uiState.isRecording) stringResource(Res.string.voice_practice_release_to_send) else stringResource(Res.string.voice_practice_hold_to_talk),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (uiState.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun ChatBubble(message: VoiceMessage) {
    val isTutor = message.sender == Sender.TUTOR
    val alignment = if (isTutor) Alignment.Start else Alignment.End
    
    val bubbleColor = if (isTutor) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
    }
    
    val textColor = if (isTutor) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    val bubbleShape = if (isTutor) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        // Mittente
        Text(
            text = if (isTutor) stringResource(Res.string.voice_practice_tutor_name) else stringResource(Res.string.voice_practice_user_name),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
        // Corpo del messaggio
        Box(
            modifier = Modifier
                .clip(bubbleShape)
                .background(bubbleColor)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun VoiceWaveVisualizer(
    amplitude: Float,
    isRecording: Boolean,
    isProcessing: Boolean,
    isSpeaking: Boolean
) {
    // Colore primario del visualizzatore basato sullo stato attivo
    val waveColor = when {
        isRecording -> MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
        isProcessing -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
        isSpeaking -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
    }

    // Animazioni ad onde concentriche infinite
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val baseRadius = 60.dp.toPx()

        // Ampiezza d'onda dinamica data dalla decibel
        val extraRadius = amplitude * 80.dp.toPx()

        if (isRecording || isSpeaking || isProcessing) {
            // Cerchio pulsante 1
            drawCircle(
                color = waveColor,
                radius = (baseRadius + extraRadius) * (if (isProcessing) 1f else pulseScale1),
                center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                style = Stroke(width = 3.dp.toPx())
            )
            // Cerchio pulsante 2
            drawCircle(
                color = waveColor.copy(alpha = waveColor.alpha * 0.5f),
                radius = (baseRadius + extraRadius) * (if (isProcessing) 1f else pulseScale2),
                center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                style = Stroke(width = 2.dp.toPx())
            )
        } else {
            // Cerchio statico rilassato
            drawCircle(
                color = waveColor,
                radius = baseRadius,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

/**
 * Card prominente che invita l'utente a scaricare il modello voce (Whisper Tiny, 32 MB).
 * Tre stati:
 *  - Idle (whisperMissing): mostra bottone "Scarica modello voce"
 *  - Downloading: mostra progress bar lineare con percentuale
 *  - Loading in RAM: mostra spinner indeterminato con etichetta
 */
@Composable
private fun WhisperDownloadCard(
    progress: Float?,
    isLoadingInRam: Boolean,
    onDownloadClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.20f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.error.copy(alpha = 0.30f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🎙️", fontSize = 22.sp)
                Column {
                    Text(
                        text = "Modello voce richiesto",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Per trascrivere ciò che dici serve Whisper Tiny (32 MB, multilingua, offline).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            when {
                isLoadingInRam -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Caricamento in RAM...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                progress != null -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "Download in corso: ${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    Button(
                        onClick = onDownloadClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Scarica modello voce (32 MB)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
