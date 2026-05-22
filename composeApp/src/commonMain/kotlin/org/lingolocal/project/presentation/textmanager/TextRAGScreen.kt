package org.lingolocal.project.presentation.textmanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import org.lingolocal.project.domain.model.ChatMessage
import org.lingolocal.project.domain.model.Quiz
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
                Column(modifier = Modifier.fillMaxSize()) {
                    // Modern Tab Selector
                    TabRow(
                        selectedTabIndex = uiState.selectedTab,
                        containerColor = Color.Transparent,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Tab(
                            selected = uiState.selectedTab == 0,
                            onClick = { screenModel.onTabSelected(0) },
                            text = {
                                Text(
                                    text = stringResource(Res.string.tab_quiz_chatbot),
                                    fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = uiState.selectedTab == 1,
                            onClick = { screenModel.onTabSelected(1) },
                            text = {
                                Text(
                                    text = stringResource(Res.string.tab_quiz_rag),
                                    fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.selectedTab == 0) {
                        ChatbotTabContent(
                            uiState = uiState,
                            screenModel = screenModel,
                            onStartQuiz = { quiz ->
                                navigator.push(QuizScreen(quiz))
                            }
                        )
                    } else {
                        RAGTabContent(
                            uiState = uiState,
                            screenModel = screenModel
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ChatLanguageSelector(
        selectedLanguage: String,
        onLanguageSelected: (String) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.chat_quiz_study_language),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                val languages = listOf(
                    "es" to "Spagnolo 🇪🇸",
                    "en" to "Inglese 🇬🇧",
                    "fr" to "Francese 🇫🇷",
                    "de" to "Tedesco 🇩🇪",
                    "it" to "Italiano 🇮🇹"
                )
                items(languages) { (code, label) ->
                    val isSelected = code == selectedLanguage
                    FilterChip(
                        selected = isSelected,
                        onClick = { onLanguageSelected(code) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }
    }

    @Composable
    private fun ChatBubble(
        message: ChatMessage,
        onStartQuiz: (Quiz) -> Unit
    ) {
        val isUser = message.isUser
        val alignment = if (isUser) Alignment.End else Alignment.Start
        val shape = if (isUser) {
            RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
        } else {
            RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
        }

        val containerColor = if (isUser) {
            MaterialTheme.colorScheme.primary
        } else if (message.isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        }

        val textColor = if (isUser) {
            MaterialTheme.colorScheme.onPrimary
        } else if (message.isError) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalAlignment = alignment
        ) {
            Text(
                text = if (isUser) "Tu" else "LingoTutor",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )

            Surface(
                color = containerColor,
                shape = shape,
                border = if (!isUser && !message.isError) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) else null,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (message.isGenerating && message.text.isEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 1.5.dp,
                                color = textColor
                            )
                            Text(
                                text = stringResource(Res.string.chat_quiz_generating),
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor.copy(alpha = 0.8f)
                              )
                        }
                    } else {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }

                    message.generatedQuiz?.let { quiz ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = LingoIcons.Science,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = quiz.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${quiz.questions.size} Domande",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Button(
                                    onClick = { onStartQuiz(quiz) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(
                                        text = stringResource(Res.string.chat_quiz_start_button),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
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
    private fun ChatbotTabContent(
        uiState: TextRAGUiState,
        screenModel: TextRAGScreenModel,
        onStartQuiz: (Quiz) -> Unit
    ) {
        var inputText by remember { mutableStateOf("") }
        val messages = uiState.chatMessages
        val listState = rememberLazyListState()

        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.lastIndex)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChatLanguageSelector(
                selectedLanguage = uiState.chatbotLanguage,
                onLanguageSelected = screenModel::onChatbotLanguageSelected
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        ChatBubble(
                            message = ChatMessage(
                                id = "welcome",
                                text = stringResource(Res.string.chat_quiz_welcome),
                                isUser = false
                            ),
                            onStartQuiz = onStartQuiz
                        )
                    }
                } else {
                    items(messages) { msg ->
                        ChatBubble(
                            message = msg,
                            onStartQuiz = onStartQuiz
                        )
                    }
                }
            }

            val suggestions = when (uiState.chatbotLanguage) {
                "es" -> listOf("Spiegami i verbi al passato", "Quiz pronomi riflessivi", "Vocaboli del cibo")
                "en" -> listOf("Explain present perfect", "Quiz phrasal verbs", "Travel vocabulary")
                "fr" -> listOf("Spiegami i pronomi relativi", "Quiz verbi irregolari", "Vocaboli della spesa")
                "de" -> listOf("Spiegami i casi in tedesco", "Quiz verbi modali", "Vocaboli del lavoro")
                else -> listOf("Spiegami la grammatica", "Quiz vocabolario", "Frasi utili")
            }

            if (messages.isEmpty() && uiState.isLlmReady) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    items(suggestions) { sugg ->
                        SuggestionChip(
                            onClick = {
                                inputText = sugg
                                screenModel.sendChatMessage(sugg)
                                inputText = ""
                            },
                            label = { Text(sugg, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(stringResource(Res.string.chat_quiz_prompt_hint)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    enabled = uiState.isLlmReady && !messages.any { it.isGenerating },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            screenModel.sendChatMessage(inputText.trim())
                            inputText = ""
                        }
                    },
                    enabled = uiState.isLlmReady && inputText.isNotBlank() && !messages.any { it.isGenerating },
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (inputText.isNotBlank() && uiState.isLlmReady && !messages.any { it.isGenerating }) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = LingoIcons.PlayArrow,
                        contentDescription = "Invia",
                        tint = if (inputText.isNotBlank() && uiState.isLlmReady && !messages.any { it.isGenerating }) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!uiState.isLlmReady) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                        .padding(8.dp),
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
        }
    }

    @Composable
    private fun RAGTabContent(
        uiState: TextRAGUiState,
        screenModel: TextRAGScreenModel
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
