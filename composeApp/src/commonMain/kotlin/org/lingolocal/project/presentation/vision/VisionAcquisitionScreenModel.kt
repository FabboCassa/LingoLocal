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
    val processedSizeText: String? = null,
    val imageList: List<ByteArray> = emptyList(),
    val processedImageList: List<ByteArray> = emptyList(),
    val selectedImageIndex: Int = 0,
    val targetLanguage: String = "es"
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
            val updatedList = state.imageList + bytes
            val nextSelectedIndex = updatedList.lastIndex
            state.copy(
                imageList = updatedList,
                selectedImageIndex = nextSelectedIndex,
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

        val currentIndex = _uiState.value.imageList.lastIndex
        screenModelScope.launch {
            try {
                // Ridimensiona e comprime off-thread
                val processed = preprocessImageUseCase(bytes)
                val procSizeText = formatSize(processed.size)
                _uiState.update { state ->
                    val updatedProcessedList = state.processedImageList.toMutableList()
                    while (updatedProcessedList.size <= currentIndex) {
                        updatedProcessedList.add(ByteArray(0))
                    }
                    updatedProcessedList[currentIndex] = processed
                    
                    // Se questa è ancora l'immagine selezionata, aggiorna anche le variabili di compatibilità
                    val isCurrentlySelected = state.selectedImageIndex == currentIndex
                    state.copy(
                        processedImageList = updatedProcessedList,
                        processedImageBytes = if (isCurrentlySelected) processed else state.processedImageBytes,
                        processedSizeText = if (isCurrentlySelected) procSizeText else state.processedSizeText,
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

    fun onSelectImage(index: Int) {
        _uiState.update { state ->
            if (index in state.imageList.indices) {
                val origBytes = state.imageList[index]
                val procBytes = state.processedImageList.getOrNull(index)?.takeIf { it.isNotEmpty() }
                state.copy(
                    selectedImageIndex = index,
                    imageBytes = origBytes,
                    originalSizeText = formatSize(origBytes.size),
                    processedImageBytes = procBytes,
                    processedSizeText = procBytes?.let { formatSize(it.size) }
                )
            } else state
        }
    }

    fun onRemoveImage(index: Int) {
        _uiState.update { state ->
            if (index !in state.imageList.indices) return@update state
            val newList = state.imageList.filterIndexed { idx, _ -> idx != index }
            val newProcessedList = if (index in state.processedImageList.indices) {
                state.processedImageList.filterIndexed { idx, _ -> idx != index }
            } else {
                state.processedImageList
            }
            val newSelectedIndex = if (newList.isEmpty()) 0 else {
                if (state.selectedImageIndex >= newList.size) newList.lastIndex else state.selectedImageIndex
            }
            val origBytes = newList.getOrNull(newSelectedIndex)
            val procBytes = newProcessedList.getOrNull(newSelectedIndex)?.takeIf { it.isNotEmpty() }
            state.copy(
                imageList = newList,
                processedImageList = newProcessedList,
                selectedImageIndex = newSelectedIndex,
                imageBytes = origBytes,
                originalSizeText = origBytes?.let { formatSize(it.size) },
                processedImageBytes = procBytes,
                processedSizeText = procBytes?.let { formatSize(it.size) },
                rawResultJson = null,
                extractedResult = null
            )
        }
    }

    fun onTargetLanguageSelected(lang: String) {
        _uiState.update { it.copy(targetLanguage = lang) }
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

    private fun mergeVisionResults(results: List<VisionResult>): VisionResult {
        if (results.isEmpty()) {
            return VisionResult(
                tipo = "Generale",
                lingua_originale = _uiState.value.targetLanguage,
                testo_estratto = "",
                traduzione = ""
            )
        }
        if (results.size == 1) return results[0]

        val first = results.first()
        val argomenti = results.mapNotNull { it.argomento }.filter { it.isNotBlank() }.distinct()
        val argomento = if (argomenti.isNotEmpty()) argomenti.joinToString(", ") else null
        
        val testoEstratto = results.joinToString("\n\n---\n\n") { it.testo_estratto }
        val traduzione = results.joinToString("\n\n---\n\n") { it.traduzione }
        
        val entita = results.flatMap { it.entita ?: emptyList() }.distinctBy { it.chiave }
        val regoleGrammaticali = results.flatMap { it.regoleGrammaticali ?: emptyList() }
            .distinctBy { it.regola.lowercase() }
        val vocaboliChiave = results.flatMap { it.vocaboli_chiave ?: emptyList() }
            .distinctBy { it.originale.lowercase() }

        return VisionResult(
            tipo = first.tipo,
            lingua_originale = first.lingua_originale,
            argomento = argomento,
            testo_estratto = testoEstratto,
            traduzione = traduzione,
            entita = entita,
            regoleGrammaticali = regoleGrammaticali,
            vocaboli_chiave = vocaboliChiave
        )
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
            val imagesToProcess = _uiState.value.processedImageList.filter { it.isNotEmpty() }.ifEmpty { _uiState.value.imageList }
            if (imagesToProcess.isEmpty()) {
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

                val resultsList = mutableListOf<VisionResult>()
                val docType = _uiState.value.documentType
                val targetLang = _uiState.value.targetLanguage
                val targetLangName = when(targetLang) {
                    "es" -> "Spagnolo"
                    "en" -> "Inglese"
                    "fr" -> "Francese"
                    "de" -> "Tedesco"
                    "it" -> "Italiano"
                    else -> "Spagnolo"
                }

                val prompt = when (docType) {
                    "Ricevuta" -> "Estrai testo, articoli, totale ed entità da questa ricevuta o scontrino e restituisci in formato JSON."
                    "Grammatica" -> """
                        Analizza questa pagina di un libro di grammatica o materiale didattico.
                        L'utente sta studiando la lingua: $targetLangName ($targetLang).
                        IMPORTANTE: Anche se le spiegazioni o gli esercizi sulla pagina sono scritti in un'altra lingua (es. Italiano per spiegare lo Spagnolo), devi trattare il $targetLangName come la lingua di studio ('lingua_originale').
                        Quindi:
                        1. Estrai i vocaboli chiave ('vocaboli_chiave') in cui 'originale' è nella lingua di studio ($targetLangName) e 'traduzione' è nella lingua delle spiegazioni (es. Italiano).
                        2. Identifica le regole grammaticali ('regoleGrammaticali') riferite esclusivamente alle strutture del $targetLangName.
                        3. Restituisci il risultato strutturato ESCLUSIVAMENTE in formato JSON con questo schema:
                        {
                          "tipo": "Grammatica",
                          "lingua_originale": "$targetLang",
                          "argomento": "Argomento grammaticale principale della pagina",
                          "testo_estratto": "Il testo completo trascritto dalla pagina",
                          "traduzione": "Traduzione in italiano di eventuali testi d'esempio o spiegazioni",
                          "regele_grammaticali": [
                            {"regola": "Nome regola", "dettaglio": "Descrizione in italiano"}
                          ],
                          "vocaboli_chiave": [
                            {"originale": "parola in $targetLangName", "traduzione": "parola in italiano", "pronuncia": "pronuncia approssimativa"}
                          ]
                        }
                    """.trimIndent()
                    else -> """
                        Estrai il testo da questa immagine e restituisci concetti chiave in JSON.
                        Tratta la lingua di studio come $targetLangName ($targetLang).
                        L'oggetto JSON restituito deve avere questo schema:
                        {
                          "tipo": "Generale",
                          "lingua_originale": "$targetLang",
                          "testo_estratto": "Il testo completo estratto",
                          "traduzione": "Traduzione in italiano",
                          "vocaboli_chiave": [
                            {"originale": "parola in $targetLangName", "traduzione": "traduzione in italiano", "pronuncia": ""}
                          ]
                        }
                    """.trimIndent()
                }

                for (i in imagesToProcess.indices) {
                    val stepInference = "Analisi pagina ${i + 1} di ${imagesToProcess.size} in corso..."
                    _uiState.update { it.copy(analysisStep = stepInference) }
                    
                    val responseStringBuilder = StringBuilder()
                    llamaRepository.generate(prompt = prompt, imageBytes = imagesToProcess[i], maxTokens = 1024).collect { token ->
                        responseStringBuilder.append(token)
                        _uiState.update { state ->
                            state.copy(rawResultJson = "Elaborazione pagina ${i + 1}/${imagesToProcess.size}...\n\n" + responseStringBuilder.toString())
                        }
                    }

                    val finalJsonString = responseStringBuilder.toString().trim()
                    logInfo(TAG, "Pagina ${i + 1} risposta grezza: $finalJsonString")

                    val parsedResult = withContext(Dispatchers.Default) {
                        json.decodeFromString<VisionResult>(extractJson(finalJsonString))
                    }
                    resultsList.add(parsedResult)
                }

                val stepDecoding = getString(Res.string.vision_acq_step_decoding)
                _uiState.update { it.copy(analysisStep = stepDecoding) }
                delay(400)

                val mergedResult = withContext(Dispatchers.Default) {
                    mergeVisionResults(resultsList)
                }

                _uiState.update { state ->
                    state.copy(
                        isAnalyzing = false,
                        analysisStep = null,
                        extractedResult = mergedResult
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
