package org.lingolocal.project.presentation.textmanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import org.lingolocal.project.presentation.theme.LingoIcons
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import lingolocal.composeapp.generated.resources.Res
import lingolocal.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.lingolocal.project.domain.model.QuizFocus

class TextRAGScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = getScreenModel<TextRAGScreenModel>()
        val uiState by screenModel.uiState.collectAsState()

        // Effettua un controllo all'ingresso
        LaunchedEffect(Unit) {
            screenModel.checkLlmStatus()
            screenModel.loadDocuments()
        }

        // Naviga al quiz se è stato generato con successo
        LaunchedEffect(uiState.generatedQuiz) {
            uiState.generatedQuiz?.let { quiz ->
                navigator.push(QuizScreen(quiz))
                screenModel.clearGeneratedQuiz()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.text_rag_title), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(imageVector = LingoIcons.ArrowBack, contentDescription = "Indietro")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                        )
                    )
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // Sottotitolo introduttivo
                    item {
                        Text(
                            text = stringResource(Res.string.text_rag_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    // Sezione Carica Nuovo Materiale
                    item {
                        AddMaterialSection(
                            titleInput = uiState.titleInput,
                            textInput = uiState.textInput,
                            isIndexing = uiState.isIndexing,
                            onTitleChanged = screenModel::onTitleChanged,
                            onTextChanged = screenModel::onTextChanged,
                            onIndexClick = {
                                screenModel.indexText()
                            }
                        )
                    }

                    // Sezione Materiali Didattici Indicizzati
                    item {
                        Text(
                            text = stringResource(Res.string.indexed_materials_section),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (uiState.documents.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(Res.string.no_indexed_materials),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(uiState.documents) { doc ->
                            DocumentRow(
                                doc = doc,
                                onDelete = { screenModel.deleteDocument(doc.sourceId) }
                            )
                        }
                    }

                    // Sezione Generatore Quiz
                    item {
                        QuizGeneratorSection(
                            topicQuery = uiState.topicQuery,
                            numQuestions = uiState.numQuestions,
                            focus = uiState.focus,
                            isGenerating = uiState.isGeneratingQuiz,
                            isLlmReady = uiState.isLlmReady,
                            onTopicQueryChanged = screenModel::onTopicQueryChanged,
                            onNumQuestionsChanged = screenModel::onNumQuestionsChanged,
                            onFocusChanged = screenModel::onFocusChanged,
                            onGenerateQuiz = { screenModel.generateQuiz() }
                        )
                    }
                }

                // Banner Messaggi di errore o successo temporanei
                uiState.errorMessage?.let { errorMsg ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = { screenModel.clearMessages() }) {
                                Text("OK", color = MaterialTheme.colorScheme.inversePrimary)
                            }
                        }
                    ) {
                        Text(errorMsg)
                    }
                }

                uiState.successMessage?.let { successMsg ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        action = {
                            TextButton(onClick = { screenModel.clearMessages() }) {
                                Text("OK", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    ) {
                        Text(successMsg)
                    }
                }
            }
        }
    }

    @Composable
    private fun AddMaterialSection(
        titleInput: String,
        textInput: String,
        isIndexing: Boolean,
        onTitleChanged: (String) -> Unit,
        onTextChanged: (String) -> Unit,
        onIndexClick: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.add_text_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = onTitleChanged,
                    placeholder = { Text(stringResource(Res.string.text_title_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    enabled = !isIndexing
                )

                OutlinedTextField(
                    value = textInput,
                    onValueChange = onTextChanged,
                    placeholder = { Text(stringResource(Res.string.text_content_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isIndexing
                )

                Button(
                    onClick = onIndexClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isIndexing && titleInput.isNotBlank() && textInput.isNotBlank()
                ) {
                    if (isIndexing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(Res.string.indexing_in_progress))
                    } else {
                        Text(stringResource(Res.string.index_text_button))
                    }
                }
            }
        }
    }

    @Composable
    private fun DocumentRow(
        doc: DocumentSummary,
        onDelete: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = doc.sourceId,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(Res.string.chunks_count, doc.chunkCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = LingoIcons.Delete,
                        contentDescription = "Elimina",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    @Composable
    private fun QuizGeneratorSection(
        topicQuery: String,
        numQuestions: Int,
        focus: QuizFocus,
        isGenerating: Boolean,
        isLlmReady: Boolean,
        onTopicQueryChanged: (String) -> Unit,
        onNumQuestionsChanged: (Int) -> Unit,
        onFocusChanged: (QuizFocus) -> Unit,
        onGenerateQuiz: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.quiz_generator_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Topic query textfield
                OutlinedTextField(
                    value = topicQuery,
                    onValueChange = onTopicQueryChanged,
                    placeholder = { Text(stringResource(Res.string.topic_query_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    enabled = !isGenerating
                )

                // Selector Numero Domande
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(Res.string.num_questions_label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(3, 5, 10).forEach { num ->
                            val selected = num == numQuestions
                            val containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            val textColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            val border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(containerColor)
                                    .then(if (border != null) Modifier.border(border, RoundedCornerShape(8.dp)) else Modifier)
                                    .clickable(enabled = !isGenerating) { onNumQuestionsChanged(num) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = num.toString(),
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        }
                    }
                }

                // Selector Focus
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(Res.string.focus_label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            QuizFocus.GENERAL to Res.string.focus_general,
                            QuizFocus.GRAMMAR to Res.string.focus_grammar,
                            QuizFocus.VOCABULARY to Res.string.focus_vocabulary,
                            QuizFocus.WITH_EXAMPLES to Res.string.focus_examples
                        ).chunked(2).forEach { pairList ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                pairList.forEach { (quizFocus, labelRes) ->
                                    val selected = quizFocus == focus
                                    val containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                    val textColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    val border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(containerColor)
                                            .then(if (border != null) Modifier.border(border, RoundedCornerShape(8.dp)) else Modifier)
                                            .clickable(enabled = !isGenerating) { onFocusChanged(quizFocus) }
                                            .padding(horizontal = 8.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(labelRes),
                                            fontWeight = FontWeight.Bold,
                                            color = textColor,
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (!isLlmReady) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = LingoIcons.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = stringResource(Res.string.llm_not_ready_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Button(
                    onClick = onGenerateQuiz,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    enabled = isLlmReady && !isGenerating
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(Res.string.generating_quiz_progress))
                    } else {
                        Text(stringResource(Res.string.generate_quiz_button))
                    }
                }
            }
        }
    }
}
