package org.lingolocal.project.presentation.vision

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import lingolocal.composeapp.generated.resources.Res
import lingolocal.composeapp.generated.resources.*
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource
import org.lingolocal.project.domain.model.VisionResult
import org.lingolocal.project.presentation.theme.LingoIcons
import org.lingolocal.project.presentation.util.rememberImagePicker

class VisionAcquisitionScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = getScreenModel<VisionAcquisitionScreenModel>()
        val uiState by screenModel.uiState.collectAsState()
        val previewBytes = uiState.processedImageBytes ?: uiState.imageBytes

        val imagePickerLauncher = rememberImagePicker(
            onImagePicked = screenModel::onImagePicked,
            onPermissionDenied = screenModel::onPermissionDenied
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.vision_acq_title), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                imageVector = LingoIcons.ArrowBack,
                                contentDescription = stringResource(Res.string.common_back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                )
                            )
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header card
                    if (uiState.extractedResult == null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = LingoIcons.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = stringResource(Res.string.vision_acq_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Main image selection / view finder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (previewBytes != null) {
                            val bitmap = remember(previewBytes) {
                                try {
                                    previewBytes.decodeToImageBitmap()
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = stringResource(Res.string.common_preview),
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Scanning laser animation
                                if (uiState.isAnalyzing) {
                                    val infiniteTransition = rememberInfiniteTransition()
                                    val laserY by infiniteTransition.animateFloat(
                                        initialValue = 0f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(2000, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        )
                                    )
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val y = size.height * laserY
                                        drawLine(
                                            color = Color(0xFF00FFCC),
                                            start = androidx.compose.ui.geometry.Offset(0f, y),
                                            end = androidx.compose.ui.geometry.Offset(size.width, y),
                                            strokeWidth = 4f
                                        )
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0xFF00FFCC).copy(alpha = 0.15f),
                                                    Color.Transparent
                                                )
                                            ),
                                            topLeft = androidx.compose.ui.geometry.Offset(0f, y - 40f),
                                            size = androidx.compose.ui.geometry.Size(size.width, 40f)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Mock native camera viewfinder overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val sizePx = 24.dp.toPx()
                                    val strokeWidth = 3.dp.toPx()
                                    val cornerColor = Color(0xFF00FFCC)

                                    // Top-Left corner
                                    drawPath(
                                        path = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(0f, sizePx)
                                            lineTo(0f, 0f)
                                            lineTo(sizePx, 0f)
                                        },
                                        color = cornerColor,
                                        style = Stroke(strokeWidth)
                                    )
                                    // Top-Right corner
                                    drawPath(
                                        path = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(size.width - sizePx, 0f)
                                            lineTo(size.width, 0f)
                                            lineTo(size.width, sizePx)
                                        },
                                        color = cornerColor,
                                        style = Stroke(strokeWidth)
                                    )
                                    // Bottom-Left corner
                                    drawPath(
                                        path = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(0f, size.height - sizePx)
                                            lineTo(0f, size.height)
                                            lineTo(sizePx, size.height)
                                        },
                                        color = cornerColor,
                                        style = Stroke(strokeWidth)
                                    )
                                    // Bottom-Right corner
                                    drawPath(
                                        path = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(size.width - sizePx, size.height)
                                            lineTo(size.width, size.height)
                                            lineTo(size.width, size.height - sizePx)
                                        },
                                        color = cornerColor,
                                        style = Stroke(strokeWidth)
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = LingoIcons.Camera,
                                        contentDescription = null,
                                        tint = Color(0xFF00FFCC).copy(alpha = 0.8f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = stringResource(Res.string.vision_acq_scan_hint),
                                        color = Color.White.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        // Image compression loading spinner
                        if (uiState.isProcessingImage) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color(0xFF00FFCC))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(stringResource(Res.string.vision_acq_compressing_image), color = Color.White)
                                }
                            }
                        }
                    }

                    // Input actions and options
                    if (previewBytes != null && !uiState.isAnalyzing && uiState.extractedResult == null) {
                        // Options Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Original vs Compressed weights
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(stringResource(Res.string.vision_acq_file_original, uiState.originalSizeText ?: ""), style = MaterialTheme.typography.bodySmall)
                                    Text(stringResource(Res.string.vision_acq_file_optimized, uiState.processedSizeText ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }

                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                // Document type selector
                                Text(
                                    text = stringResource(Res.string.vision_acq_document_type),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val docTypes = listOf(
                                        "Ricevuta" to Res.string.vision_acq_type_receipt,
                                        "Grammatica" to Res.string.vision_acq_type_grammar,
                                        "Generale" to Res.string.vision_acq_type_general
                                    )
                                    docTypes.forEach { (typeKey, resource) ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { screenModel.onDocumentTypeSelected(typeKey) }
                                                .padding(vertical = 4.dp)
                                        ) {
                                            RadioButton(
                                                selected = uiState.documentType == typeKey,
                                                onClick = { screenModel.onDocumentTypeSelected(typeKey) }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(resource), style = MaterialTheme.typography.bodyLarge)
                                        }
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                // Deck selector
                                Text(
                                    text = stringResource(Res.string.vision_acq_select_deck),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                var expanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    val selectedDeck = uiState.availableDecks.find { it.id == uiState.selectedDeckId }
                                    OutlinedButton(
                                        onClick = { expanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = selectedDeck?.name ?: stringResource(Res.string.vision_acq_default_deck_label),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Icon(
                                                imageVector = LingoIcons.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        uiState.availableDecks.forEach { deck ->
                                            DropdownMenuItem(
                                                text = { Text(deck.name) },
                                                onClick = {
                                                    screenModel.selectDeck(deck.id ?: 0L)
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // CTA Button
                        Button(
                            onClick = { screenModel.onStartAnalysis() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = LingoIcons.PlayArrow,
                                    contentDescription = null
                                )
                                Text(
                                    text = stringResource(Res.string.vision_acq_start_analysis),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    // Sources if empty
                    if (previewBytes == null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = { imagePickerLauncher.launchCamera() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = LingoIcons.Camera, contentDescription = null)
                                    Text(stringResource(Res.string.vision_acq_camera), fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { imagePickerLauncher.launchGallery() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = LingoIcons.PhotoLibrary, contentDescription = null)
                                    Text(stringResource(Res.string.vision_acq_gallery), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Errors
                    uiState.errorMessage?.let { err ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // RESULTS TAB VIEW
                    uiState.extractedResult?.let { result ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Navigation Tabs
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val tabs = listOf(
                                    Res.string.vision_acq_tab_text,
                                    Res.string.vision_acq_tab_vocab,
                                    Res.string.vision_acq_tab_details
                                )
                                tabs.forEachIndexed { index, tabRes ->
                                    Button(
                                        onClick = { screenModel.onTabSelected(index) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (uiState.selectedTab == index) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            contentColor = if (uiState.selectedTab == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        elevation = null
                                    ) {
                                        Text(
                                            text = stringResource(tabRes),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // Dynamic Tab Content
                            when (uiState.selectedTab) {
                                0 -> TextAndTranslationTab(result)
                                1 -> VocabularyTab(result, uiState.savedWordIndices, screenModel::onSaveWordAsFlashcard)
                                2 -> DetailsTab(result)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Reset/Capture another button
                            OutlinedButton(
                                onClick = { screenModel.clearState() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = LingoIcons.Delete, contentDescription = null)
                                    Text(stringResource(Res.string.vision_acq_scan_another_doc), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Blur loading overlay
                if (uiState.isAnalyzing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .blur(if (uiState.isAnalyzing) 8.dp else 0.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Dummy box to host the blur content and make sure the background underneath is visually blurred
                    }

                    // Content panel (not blurred, on top of blur overlay)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .wrapContentHeight(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.5.dp,
                                    modifier = Modifier.size(56.dp)
                                )

                                Text(
                                    text = stringResource(Res.string.vision_acq_processing_local_ai),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                uiState.analysisStep?.let { step ->
                                    Text(
                                        text = step,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Streaming placeholder
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = uiState.rawResultJson ?: stringResource(Res.string.vision_acq_waiting_for_data),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            maxLines = 5,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Text(
                                    text = stringResource(Res.string.vision_acq_do_not_turn_off),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TextAndTranslationTab(result: VisionResult) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Original text card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(Res.string.vision_acq_original_text) + " (${result.lingua_originale.uppercase()})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = result.testo_estratto,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 22.sp
                )
            }
        }

        // Translation card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(Res.string.vision_acq_translated_text) + " (IT)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = result.traduzione,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
private fun VocabularyTab(
    result: VisionResult,
    savedIndices: Set<Int>,
    onSaveWord: (Int) -> Unit
) {
    val wordList = result.vocaboli_chiave
    if (wordList.isNullOrEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.vision_acq_no_vocab_extracted), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        wordList.forEachIndexed { index, word ->
            val isSaved = index in savedIndices
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSaved) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSaved) Color(0xFFC8E6C9) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = word.originale,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = word.pronuncia,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = word.traduzione,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    IconButton(
                        onClick = { if (!isSaved) onSaveWord(index) },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isSaved) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (isSaved) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = if (isSaved) LingoIcons.Check else LingoIcons.PlayArrow, // playarrow serves as fallback/action icon
                            contentDescription = stringResource(Res.string.common_save),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailsTab(result: VisionResult) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section topic if any
        result.argomento?.let { topic ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
            ) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.vision_acq_argument), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(topic)
                }
            }
        }

        // Entities (Receipts)
        val entities = result.entita
        if (!entities.isNullOrEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(Res.string.vision_acq_entities),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    entities.forEach { entity ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(entity.chiave, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(entity.valore, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Grammar rules
        val rules = result.regoleGrammaticali
        if (!rules.isNullOrEmpty()) {
            rules.forEach { rule ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = rule.regola,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Text(
                            text = rule.dettaglio,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        if (entities.isNullOrEmpty() && rules.isNullOrEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.vision_acq_no_extra_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
