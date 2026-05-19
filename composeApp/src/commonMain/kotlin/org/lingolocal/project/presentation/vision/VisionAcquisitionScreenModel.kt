package org.lingolocal.project.presentation.vision

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.lingolocal.project.domain.model.Deck
import org.lingolocal.project.domain.model.VisionResult
import org.lingolocal.project.domain.repository.LlamaRepository
import org.lingolocal.project.domain.usecase.CreateDeckUseCase
import org.lingolocal.project.domain.usecase.CreateFlashcardUseCase
import org.lingolocal.project.domain.usecase.ObserveDecksUseCase
import org.lingolocal.project.domain.usecase.PreprocessImageUseCase
import org.lingolocal.project.util.logError
import org.lingolocal.project.util.logInfo
import lingolocal.composeapp.generated.resources.Res
import lingolocal.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString

data class VisionAcquisitionUiState(
    val imageBytes: ByteArray? = null,
    val processedImageBytes: ByteArray? = null,
    val isProcessingImage: Boolean = false,
    val isAnalyzing: Boolean = false,
    val analysisStep: String? = null,
    val rawResultJson: String? = null,
    val extractedResult: VisionResult? = null,
    val errorMessage: String? = null,
    val selectedTab: Int = 0,
    val savedWordIndices: Set<Int> = emptySet(),
    val documentType: String = "Ricevuta", // Ricevuta / Grammatica / Generale
    val availableDecks: List<Deck> = emptyList(),
    val selectedDeckId: Long? = null,
    val originalSizeText: String? = null,
    val processedSizeText: String? = null
)

class VisionAcquisitionScreenModel(
    private val preprocessImageUseCase: PreprocessImageUseCase,
    private val llamaRepository: LlamaRepository,
    private val createDeckUseCase: CreateDeckUseCase,
    private val createFlashcardUseCase: CreateFlashcardUseCase,
    private val observeDecksUseCase: ObserveDecksUseCase
) : ScreenModel {

    private val _uiState = MutableStateFlow(VisionAcquisitionUiState())
    val uiState: StateFlow<VisionAcquisitionUiState> = _uiState.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    init {
        // Osserva i mazzi disponibili
        screenModelScope.launch {
            observeDecksUseCase().collect { decks ->
                _uiState.update { state ->
                    state.copy(
                        availableDecks = decks,
                        selectedDeckId = state.selectedDeckId ?: decks.firstOrNull()?.id
                    )
                }
            }
        }
    }

    fun onImagePicked(bytes: ByteArray?) {
        if (bytes == null) return

        val origSizeText = formatSize(bytes.size)
        _uiState.update { state ->
            state.copy(
                imageBytes = bytes,
                originalSizeText = origSizeText,
                processedImageBytes = null,
                processedSizeText = null,
                isProcessingImage = true,
                errorMessage = null,
                rawResultJson = null,
                extractedResult = null,
                savedWordIndices = emptySet()
            )
        }

        screenModelScope.launch {
            try {
                // Ridimensiona e comprime off-thread
                val processed = preprocessImageUseCase(bytes)
                val procSizeText = formatSize(processed.size)
                _uiState.update { state ->
                    state.copy(
                        processedImageBytes = processed,
                        processedSizeText = procSizeText,
                        isProcessingImage = false
                    )
                }
            } catch (e: Exception) {
                logError(TAG, "Errore nel pre-processamento immagine", e)
                val errorMsg = getString(Res.string.vision_acq_err_image_optimization, e.message ?: "")
                _uiState.update { state ->
                    state.copy(
                        errorMessage = errorMsg,
                        isProcessingImage = false
                    )
                }
            }
        }
    }

    fun onDocumentTypeSelected(type: String) {
        _uiState.update { it.copy(documentType = type) }
    }

    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun clearState() {
        _uiState.update { 
            VisionAcquisitionUiState(
                availableDecks = it.availableDecks,
                selectedDeckId = it.selectedDeckId
            )
        }
    }

    fun onPermissionDenied(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun onStartAnalysis() {
        screenModelScope.launch {
            val targetBytes = _uiState.value.processedImageBytes ?: _uiState.value.imageBytes
            if (targetBytes == null) {
                val errorMsg = getString(Res.string.vision_acq_err_no_image)
                _uiState.update { it.copy(errorMessage = errorMsg) }
                return@launch
            }

            val stepInit = getString(Res.string.vision_acq_step_init_engine)
            _uiState.update { state ->
                state.copy(
                    isAnalyzing = true,
                    analysisStep = stepInit,
                    errorMessage = null,
                    rawResultJson = null,
                    extractedResult = null,
                    savedWordIndices = emptySet()
                )
            }

            try {
                // FASE 1: Inizializza i backend GGML se necessario
                llamaRepository.initialize()
                delay(600)

                // FASE 2: Aggiorna lo stato di avanzamento
                val stepPixel = getString(Res.string.vision_acq_step_pixel_sampling)
                _uiState.update { it.copy(analysisStep = stepPixel) }
                delay(600)

                // FASE 3: Genera il prompt multimodale in base al tipo di documento selezionato
                val stepInference = getString(Res.string.vision_acq_step_inference)
                _uiState.update { it.copy(analysisStep = stepInference) }
                
                val docType = _uiState.value.documentType
                val prompt = when (docType) {
                    "Ricevuta" -> "Estrai testo, articoli, totale ed entità da questa ricevuta o scontrino e restituisci in formato JSON."
                    "Grammatica" -> "Estrai concetti, spiegazioni e regole da questa pagina di grammatica e restituisci in formato JSON."
                    else -> "Estrai tutto il testo da questa immagine e restituisci una traduzione e concetti chiave in JSON."
                }

                // Chiamata all'engine locale
                val responseStringBuilder = StringBuilder()
                llamaRepository.generate(prompt = prompt, imageBytes = targetBytes, maxTokens = 1024).collect { token ->
                    responseStringBuilder.append(token)
                    // Durante lo streaming aggiorniamo il testo grezzo per mostrare il caricamento reattivo
                    _uiState.update { state ->
                        state.copy(rawResultJson = responseStringBuilder.toString())
                    }
                }

                val finalJsonString = responseStringBuilder.toString().trim()
                logInfo(TAG, "Risposta JSON grezza ricevuta: $finalJsonString")

                // FASE 4: Parsing del risultato
                val stepDecoding = getString(Res.string.vision_acq_step_decoding)
                _uiState.update { it.copy(analysisStep = stepDecoding) }
                delay(400)

                val parsedResult = withContext(Dispatchers.Default) {
                    json.decodeFromString<VisionResult>(finalJsonString)
                }

                _uiState.update { state ->
                    state.copy(
                        isAnalyzing = false,
                        analysisStep = null,
                        extractedResult = parsedResult
                    )
                }

            } catch (e: Exception) {
                logError(TAG, "Errore durante l'inferenza AI Vision", e)
                val errInference = getString(Res.string.vision_acq_err_local_inference, e.message ?: "")
                _uiState.update { state ->
                    state.copy(
                        isAnalyzing = false,
                        analysisStep = null,
                        errorMessage = errInference
                    )
                }
            }
        }
    }

    fun onSaveWordAsFlashcard(wordIndex: Int) {
        val result = _uiState.value.extractedResult ?: return
        val wordList = result.vocaboli_chiave ?: return
        if (wordIndex !in wordList.indices) return

        val word = wordList[wordIndex]
        screenModelScope.launch {
            try {
                // Ottieni o crea il mazzo di destinazione
                var deckId = _uiState.value.selectedDeckId
                if (deckId == null) {
                    // Crea un mazzo predefinito per l'acquisizione visiva
                    val defaultDeckName = getString(Res.string.vision_acq_default_deck)
                    val newId = createDeckUseCase(defaultDeckName, "it")
                    deckId = newId
                    _uiState.update { it.copy(selectedDeckId = newId) }
                }

                val frontText = word.originale
                val backText = "${word.traduzione} [${word.pronuncia}]"

                val saveResult = createFlashcardUseCase(deckId!!, frontText, backText)
                if (saveResult.isSuccess) {
                    _uiState.update { state ->
                        state.copy(
                            savedWordIndices = state.savedWordIndices + wordIndex
                        )
                    }
                    logInfo(TAG, "Salvata con successo la parola '${word.originale}' nel mazzo $deckId")
                } else {
                    val err = saveResult.exceptionOrNull()
                    val errSave = getString(Res.string.vision_acq_err_save_flashcard, err?.message ?: "")
                    _uiState.update { state ->
                        state.copy(errorMessage = errSave)
                    }
                }
            } catch (e: Exception) {
                logError(TAG, "Eccezione nel salvataggio flashcard", e)
                val errUnexpected = getString(Res.string.vision_acq_err_unexpected, e.message ?: "")
                _uiState.update { state ->
                    state.copy(errorMessage = errUnexpected)
                }
            }
        }
    }

    fun selectDeck(deckId: Long) {
        _uiState.update { it.copy(selectedDeckId = deckId) }
    }

    private fun formatSize(bytesSize: Int): String {
        val sizeKb = (bytesSize / 1024.0 * 10.0).toInt() / 10.0
        return if (sizeKb > 1024) {
            val sizeMb = (sizeKb / 1024.0 * 10.0).toInt() / 10.0
            "$sizeMb MB"
        } else {
            "$sizeKb KB"
        }
    }

    private companion object {
        private const val TAG = "VisionAcquisitionScreenModel"
    }
}
