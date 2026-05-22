package org.lingolocal.project.presentation.textmanager

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.lingolocal.project.domain.model.ChatMessage
import org.lingolocal.project.domain.model.Quiz
import org.lingolocal.project.domain.model.QuizFocus
import org.lingolocal.project.domain.repository.LlamaRepository
import org.lingolocal.project.domain.repository.VectorRepository
import org.lingolocal.project.domain.usecase.GenerateQuizUseCase
import org.lingolocal.project.domain.usecase.IndexTextUseCase
import org.lingolocal.project.domain.usecase.RagQueryUseCase
import org.lingolocal.project.util.logError
import org.lingolocal.project.util.logInfo
import kotlin.random.Random

data class DocumentSummary(
    val sourceId: String,
    val chunkCount: Int
)

data class TextRAGUiState(
    val titleInput: String = "",
    val textInput: String = "",
    val topicQuery: String = "",
    val numQuestions: Int = 3,
    val focus: QuizFocus = QuizFocus.GENERAL,
    val isIndexing: Boolean = false,
    val isGeneratingQuiz: Boolean = false,
    val isLlmReady: Boolean = false,
    val documents: List<DocumentSummary> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val generatedQuiz: Quiz? = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val selectedTab: Int = 0,
    val chatbotLanguage: String = "es"
)

class TextRAGScreenModel(
    private val indexTextUseCase: IndexTextUseCase,
    private val ragQueryUseCase: RagQueryUseCase,
    private val generateQuizUseCase: GenerateQuizUseCase,
    private val llamaRepository: LlamaRepository,
    private val vectorRepository: VectorRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(TextRAGUiState())
    val uiState: StateFlow<TextRAGUiState> = _uiState.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, t ->
        logError(TAG, "Errore nella gestione del RAG/Quiz", t)
        _uiState.value = _uiState.value.copy(
            isIndexing = false,
            isGeneratingQuiz = false,
            errorMessage = t.message ?: "Errore imprevisto"
        )
    }

    init {
        checkLlmStatus()
        loadDocuments()
    }

    fun checkLlmStatus() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLlmReady = llamaRepository.isReady()
            )
        }
    }

    fun loadDocuments() {
        screenModelScope.launch(exceptionHandler) {
            val chunks = vectorRepository.getAllChunks()
            val summary = chunks.groupBy { it.sourceId }
                .map { (sourceId, chunkList) ->
                    DocumentSummary(sourceId, chunkList.size)
                }
            _uiState.value = _uiState.value.copy(documents = summary)
        }
    }

    fun onTitleChanged(title: String) {
        _uiState.value = _uiState.value.copy(titleInput = title)
    }

    fun onTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(textInput = text)
    }

    fun onTopicQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(topicQuery = query)
    }

    fun onNumQuestionsChanged(num: Int) {
        _uiState.value = _uiState.value.copy(numQuestions = num)
    }

    fun onFocusChanged(focus: QuizFocus) {
        _uiState.value = _uiState.value.copy(focus = focus)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun clearGeneratedQuiz() {
        _uiState.value = _uiState.value.copy(generatedQuiz = null)
    }

    fun indexText() {
        val title = _uiState.value.titleInput.trim()
        val text = _uiState.value.textInput.trim()

        if (title.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Il titolo non può essere vuoto.")
            return
        }
        if (text.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Il testo non può essere vuoto.")
            return
        }

        _uiState.value = _uiState.value.copy(isIndexing = true, errorMessage = null, successMessage = null)

        screenModelScope.launch(exceptionHandler) {
            logInfo(TAG, "Inizio indicizzazione di '$title'...")
            val result = indexTextUseCase(sourceId = title, text = text)
            logInfo(TAG, "Completata indicizzazione: $result")

            if (result.chunksIndexed > 0) {
                _uiState.value = _uiState.value.copy(
                    isIndexing = false,
                    titleInput = "",
                    textInput = "",
                    successMessage = "Indicizzato con successo! Creati ${result.chunksCreated} frammenti (${result.chunksIndexed} inseriti, ${result.failedChunks} falliti)."
                )
                loadDocuments()
            } else {
                _uiState.value = _uiState.value.copy(
                    isIndexing = false,
                    errorMessage = "Impossibile indicizzare il testo. Assicurati che il modello sia configurato ed in memoria per generare gli embedding."
                )
            }
        }
    }

    fun deleteDocument(sourceId: String) {
        screenModelScope.launch(exceptionHandler) {
            vectorRepository.deleteBySourceId(sourceId)
            loadDocuments()
            _uiState.value = _uiState.value.copy(successMessage = "Materiale '$sourceId' eliminato correttamente.")
        }
    }

    fun generateQuiz() {
        if (!llamaRepository.isReady()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Il modello LLM non è pronto. Vai nel manager dei modelli per caricarlo.")
            return
        }
        if (_uiState.value.documents.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Non ci sono testi indicizzati nello store vettoriale. Carica prima un testo!")
            return
        }

        _uiState.value = _uiState.value.copy(isGeneratingQuiz = true, errorMessage = null, successMessage = null)

        screenModelScope.launch(exceptionHandler) {
            val query = _uiState.value.topicQuery.trim()
            
            // Eseguiamo il recupero semantico (RAG)
            val context = if (query.isNotEmpty()) {
                logInfo(TAG, "Esecuzione query RAG per il topic: '$query'")
                val ragResult = ragQueryUseCase(query = query, topK = 5)
                ragResult.context
            } else {
                logInfo(TAG, "Nessun topic specificato. Recupero tutti i chunk disponibili...")
                val allChunks = vectorRepository.getAllChunks()
                allChunks.take(10).joinToString("\n\n") { it.content }
            }

            if (context.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    isGeneratingQuiz = false,
                    errorMessage = "Nessun frammento di testo trovato relativo all'argomento richiesto."
                )
                return@launch
            }

            logInfo(TAG, "Contesto RAG recuperato. Avvio generazione quiz con focus ${_uiState.value.focus}...")
            
            val quiz = generateQuizUseCase(
                contextText = context,
                numQuestions = _uiState.value.numQuestions,
                focus = _uiState.value.focus
            )
            
            logInfo(TAG, "Quiz generato con successo: ${quiz.title}")
            _uiState.value = _uiState.value.copy(
                isGeneratingQuiz = false,
                generatedQuiz = quiz
            )
        }
    }

    fun onTabSelected(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun onChatbotLanguageSelected(lang: String) {
        _uiState.value = _uiState.value.copy(chatbotLanguage = lang)
    }

    fun clearChat() {
        _uiState.value = _uiState.value.copy(chatMessages = emptyList())
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        if (!llamaRepository.isReady()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Il modello LLM non è pronto. Vai nel manager dei modelli per caricarlo."
            )
            return
        }

        val userMsgId = "user_${Random.nextLong()}"
        val botMsgId = "bot_${Random.nextLong()}"

        val userMsg = ChatMessage(id = userMsgId, text = text, isUser = true)
        val initialBotMsg = ChatMessage(id = botMsgId, text = "", isUser = false, isGenerating = true)

        _uiState.value = _uiState.value.copy(
            chatMessages = _uiState.value.chatMessages + userMsg + initialBotMsg
        )

        screenModelScope.launch(exceptionHandler) {
            val lang = _uiState.value.chatbotLanguage
            val langName = when (lang) {
                "es" -> "Spagnolo"
                "en" -> "Inglese"
                "fr" -> "Francese"
                "de" -> "Tedesco"
                "it" -> "Italiano"
                else -> "Spagnolo"
            }

            // System prompt that instructs the model to act as a friendly language tutor
            val systemPrompt = """
                Sei un tutor di lingue amichevole ed esperto di nome LingoTutor.
                La lingua di studio selezionata dall'utente è: $langName ($lang).
                Rispondi in italiano in modo amichevole, chiaro ed educativo.
                
                Se l'utente ti chiede di creare un quiz (es. su un argomento di grammatica, vocaboli, verbi o regole della lingua $langName), tu devi:
                1. Scrivere una breve spiegazione grammaticale dell'argomento in italiano.
                2. Generare un quiz strutturato alla fine della tua risposta.
                3. Il quiz DEVE essere incluso in un blocco JSON valido e formattato contrassegnato da ```json ... ``` alla fine del messaggio.
                
                Il JSON del quiz deve seguire rigorosamente questo schema:
                {
                  "title": "Titolo descrittivo del quiz",
                  "questions": [
                    {
                      "question": "La domanda del quiz scritta nella lingua di studio ($langName)",
                      "options": ["Opzione A", "Opzione B", "Opzione C", "Opzione D"],
                      "correctOptionIndex": 0,
                      "explanation": "Una spiegazione chiara ed esaustiva in italiano del perché l'opzione corretta è quella giusta"
                    }
                  ]
                }
                
                Nota: Assicurati che l'oggetto JSON sia sintatticamente corretto e che non ci siano altri testi all'interno del blocco di codice JSON.
                Se l'utente non vuole un quiz ma fa solo una domanda di lingua o di spiegazione, rispondi normalmente senza includere il blocco JSON.
            """.trimIndent()

            val responseStringBuilder = StringBuilder()
            try {
                llamaRepository.generateChat(
                    systemPrompt = systemPrompt,
                    userMessage = text,
                    maxTokens = 1024
                ).collect { token ->
                    responseStringBuilder.append(token)
                    _uiState.update { state ->
                        state.copy(
                            chatMessages = state.chatMessages.map { msg ->
                                if (msg.id == botMsgId) {
                                    msg.copy(text = responseStringBuilder.toString())
                                } else {
                                    msg
                                }
                            }
                        )
                    }
                }

                val fullResponse = responseStringBuilder.toString()
                var quiz: Quiz? = null
                var cleanedText = fullResponse

                if (fullResponse.contains("```json") || fullResponse.contains("{")) {
                    try {
                        val jsonText = extractJson(fullResponse)
                        quiz = json.decodeFromString<Quiz>(jsonText)
                        cleanedText = fullResponse.substringBefore("```json").trim()
                        if (cleanedText.isEmpty()) {
                            cleanedText = "Ecco il quiz che ho preparato per te:"
                        }
                    } catch (e: Exception) {
                        logError("TextRAGScreenModel", "Impossibile decodificare il JSON del quiz dal chatbot", e)
                    }
                }

                _uiState.update { state ->
                    state.copy(
                        chatMessages = state.chatMessages.map { msg ->
                            if (msg.id == botMsgId) {
                                msg.copy(
                                    text = cleanedText,
                                    isGenerating = false,
                                    generatedQuiz = quiz
                                )
                            } else {
                                msg
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                logError("TextRAGScreenModel", "Errore durante generateChat del tutor", e)
                _uiState.update { state ->
                    state.copy(
                        chatMessages = state.chatMessages.map { msg ->
                            if (msg.id == botMsgId) {
                                msg.copy(
                                    text = "Spiacente, si è verificato un errore durante la generazione della risposta: ${e.message}",
                                    isGenerating = false,
                                    isError = true
                                )
                            } else {
                                msg
                            }
                        }
                    )
                }
            }
        }
    }

    private fun extractJson(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substringAfter("```json").substringBeforeLast("```").trim()
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substringAfter("```").substringBeforeLast("```").trim()
        }
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start != -1 && end != -1 && end > start) {
            cleaned = cleaned.substring(start, end + 1)
        }
        return cleaned
    }

    private companion object {
        private const val TAG = "TextRAGScreenModel"
    }
}
