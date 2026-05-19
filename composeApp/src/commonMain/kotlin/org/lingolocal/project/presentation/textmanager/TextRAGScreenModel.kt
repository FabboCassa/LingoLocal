package org.lingolocal.project.presentation.textmanager

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.lingolocal.project.domain.model.Quiz
import org.lingolocal.project.domain.model.QuizFocus
import org.lingolocal.project.domain.repository.LlamaRepository
import org.lingolocal.project.domain.repository.VectorRepository
import org.lingolocal.project.domain.usecase.GenerateQuizUseCase
import org.lingolocal.project.domain.usecase.IndexTextUseCase
import org.lingolocal.project.domain.usecase.RagQueryUseCase
import org.lingolocal.project.util.logError
import org.lingolocal.project.util.logInfo

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
    val generatedQuiz: Quiz? = null
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

    private companion object {
        private const val TAG = "TextRAGScreenModel"
    }
}
