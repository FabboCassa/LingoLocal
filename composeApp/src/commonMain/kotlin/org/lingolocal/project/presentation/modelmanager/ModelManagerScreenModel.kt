package org.lingolocal.project.presentation.modelmanager

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.lingolocal.project.data.platform.DeviceHardwareResolver
import org.lingolocal.project.data.platform.FileStorage
import org.lingolocal.project.domain.model.DownloadProgress
import org.lingolocal.project.domain.repository.LlamaRepository
import org.lingolocal.project.domain.repository.ModelRepository
import org.lingolocal.project.domain.usecase.DeleteModelUseCase
import org.lingolocal.project.domain.usecase.DownloadModelUseCase
import org.lingolocal.project.domain.usecase.InitializeLlamaUseCase
import org.lingolocal.project.domain.usecase.LoadLlamaModelUseCase
import org.lingolocal.project.util.logDebug
import org.lingolocal.project.util.logError
import org.lingolocal.project.util.logInfo

/**
 * ScreenModel per la gestione avanzata dei modelli AI locali.
 * Supporta catalogo predefinito (Qwen, Gemma 2, Gemma 4), download resiliente,
 * caricamento in RAM (attivazione), disinstallazione (rimozione file) e importazione GGUF esterni.
 */
class ModelManagerScreenModel(
    private val downloadModelUseCase: DownloadModelUseCase,
    private val deleteModelUseCase: DeleteModelUseCase,
    private val modelRepository: ModelRepository,
    private val initializeLlamaUseCase: InitializeLlamaUseCase,
    private val loadModelUseCase: LoadLlamaModelUseCase,
    private val llamaRepository: LlamaRepository,
    private val settings: Settings,
    private val hardwareResolver: DeviceHardwareResolver,
    private val fileStorage: FileStorage
) : ScreenModel {

    companion object {
        private const val TAG = "ModelManagerScreenModel"
        
        // Chiavi per multiplatform-settings
        const val KEY_ACTIVE_MODEL_PATH = "active_model_path"
        const val KEY_IMPORTED_MODELS = "imported_models"

        // Catalogo di modelli nativi preconfigurati semplificato e user-friendly
        private val CATALOG_MODELS = listOf(
            AiModelInfo(
                id = "qwen_0_5b",
                name = "🧪 Fast & Light (Qwen 2.5 0.5B · Q4_0)",
                description = "Modello ultra-leggero quantizzato Q4_0 con repack ARM (KleidiAI). Velocissimo (~25 token/s su Pixel 8 Pro) ma con risposte semplici. Ideale per emulatore, dispositivi con < 6 GB RAM o test rapidi.",
                sizeLabel = "337 MB",
                fileName = "Qwen2.5-0.5B-Instruct-Q4_0.gguf",
                url = "https://huggingface.co/bartowski/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/Qwen2.5-0.5B-Instruct-Q4_0.gguf",
                totalBytes = 352972352L
            ),
            AiModelInfo(
                id = "gemma_3_1b_qat",
                name = "🌟 Tutor Consigliato (Gemma 3 1B QAT)",
                description = "Modello Google ufficiale OTTIMIZZATO PER MOBILE con QAT (Quantization-Aware Training): qualità simile a Q8 ma con peso di solo 689 MB. Velocità target ~15 token/s su Pixel 8 Pro. Best balance tra qualità conversazionale e velocità.",
                sizeLabel = "689 MB",
                fileName = "google_gemma-3-1b-it-qat-Q4_0.gguf",
                url = "https://huggingface.co/bartowski/google_gemma-3-1b-it-qat-GGUF/resolve/main/google_gemma-3-1b-it-qat-Q4_0.gguf",
                totalBytes = 721918784L
            ),
            AiModelInfo(
                id = "gemma_2_2b",
                name = "💎 Qualità Massima (Gemma 2 2B)",
                description = "Modello da 2B parametri di Google con eccellenti capacità di conversazione e correzione grammaticale. Più accurato dei modelli 1B ma più lento (~6-8 token/s su Pixel 8 Pro). Consigliato solo se hai 6+ GB di RAM libera e accetti attese di 10-15 secondi per risposta.",
                sizeLabel = "1.63 GB",
                fileName = "gemma-2-2b-it-Q4_K_M.gguf",
                url = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf",
                totalBytes = 1708582752L
            ),
            AiModelInfo(
                id = "gemma_4_e2b",
                name = "🧠 Sperimentale Gemma 4 E2B (5B – LENTO)",
                description = "⚠️ Modello reasoning multimodale di Google (5 miliardi di parametri totali). NON consigliato: anche su Pixel 8 Pro la generazione è di ~2-3 secondi per parola, con risposte di 20-60 secondi. Da provare SOLO se vuoi sperimentare con un reasoning model multimodale.",
                sizeLabel = "3.46 GB",
                fileName = "google_gemma-4-E2B-it-Q4_K_M.gguf",
                url = "https://huggingface.co/bartowski/google_gemma-4-E2B-it-GGUF/resolve/main/google_gemma-4-E2B-it-Q4_K_M.gguf",
                totalBytes = 3462678272L
            )
        )
    }

    private val _uiState = MutableStateFlow(ModelManagerUiState())
    val uiState: StateFlow<ModelManagerUiState> = _uiState.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        logError(TAG, "Errore non gestito in ModelManager", throwable)
        _uiState.value = _uiState.value.copy(errorMessage = throwable.message ?: "Errore sconosciuto")
    }

    init {
        refreshModelStates()
    }

    /**
     * Ricarica lo stato di tutti i modelli controllando la loro esistenza locale
     * e se sono attualmente attivi in memoria nel LlamaEngine.
     */
    fun refreshModelStates() {
        val activePath = settings.getStringOrNull(KEY_ACTIVE_MODEL_PATH)
        val isLlamaReady = llamaRepository.isReady()
        val hwInfo = hardwareResolver.getHardwareInfo()

        // Carica i file importati salvati nelle impostazioni
        val importedStr = settings.getString(KEY_IMPORTED_MODELS, "")
        val importedFiles = if (importedStr.isEmpty()) emptyList() else importedStr.split(",")
        
        val externalModels = importedFiles.mapNotNull { fileName ->
            if (fileName.isBlank()) return@mapNotNull null
            val isDownloaded = modelRepository.isModelDownloaded(fileName)
            if (!isDownloaded) return@mapNotNull null // Se il file non c'è più, autoguarigione

            val path = modelRepository.getModelPath(fileName) ?: return@mapNotNull null
            val isActive = isLlamaReady && activePath != null && activePath == path
            val sizeOnDisk = fileStorage.getFileSize(path)

            AiModelInfo(
                id = "ext_${fileName.hashCode()}",
                name = fileName,
                description = "Modello GGUF personalizzato importato dall'utente.",
                sizeLabel = if (sizeOnDisk > 0L) "${(sizeOnDisk / (1024.0 * 1024.0)).toInt()} MB" else "Esterno",
                fileName = fileName,
                url = path, // usiamo il path assoluto locale come "URL" per i file esterni
                isExternal = true,
                isDownloaded = true,
                isActive = isActive,
                isRecommended = false,
                downloadedBytes = sizeOnDisk,
                totalBytes = sizeOnDisk
            )
        }

        val activeModelsMap = _uiState.value.models.associateBy { it.id }

        // Unisce catalogo predefinito e modelli esterni con i loro stati reali
        val updatedModels = CATALOG_MODELS.map { model ->
            val isDownloaded = modelRepository.isModelDownloaded(model.fileName)
            val isPartial = modelRepository.isModelPartial(model.fileName)
            val downloadedBytes = if (isDownloaded) {
                model.totalBytes
            } else if (isPartial) {
                modelRepository.getDownloadedBytes(model.fileName)
            } else {
                0L
            }
            val path = modelRepository.getModelPath(model.fileName)
            val isActive = isLlamaReady && activePath != null && path != null && activePath == path
            val isRecommended = (model.id == hwInfo.recommendedModelId)

            val currentModel = activeModelsMap[model.id]
            val isDownloadingActive = currentModel?.isDownloadingActive ?: false

            val liveProgress = if (isDownloadingActive && currentModel != null) {
                currentModel.downloadProgress
            } else {
                when {
                    isDownloaded -> DownloadProgress.Completed(path ?: "")
                    isPartial -> DownloadProgress.Downloading(downloadedBytes, model.totalBytes)
                    else -> DownloadProgress.Idle
                }
            }

            model.copy(
                isDownloaded = isDownloaded,
                isPartial = isPartial,
                downloadedBytes = downloadedBytes,
                isActive = isActive,
                isRecommended = isRecommended,
                isDownloadingActive = isDownloadingActive,
                downloadProgress = liveProgress
            )
        } + externalModels

        _uiState.value = ModelManagerUiState(
            models = updatedModels, 
            errorMessage = null,
            hardwareInfo = hwInfo,
            modelsDirectory = fileStorage.getModelsDirectory()
        )
    }

    /**
     * Avvia il download progressivo in streaming di un modello dal catalogo.
     */
    fun downloadModel(model: AiModelInfo) {
        if (model.isDownloaded || model.isExternal) return
        logInfo(TAG, "Avvio download per: ${model.name}")

        // Imposta immediatamente lo stato attivo di download prima di iniziare a collezionare
        _uiState.value = _uiState.value.copy(
            models = _uiState.value.models.map { m ->
                if (m.id == model.id) {
                    m.copy(isDownloadingActive = true)
                } else m
            }
        )

        screenModelScope.launch(exceptionHandler) {
            downloadModelUseCase(model.url, model.fileName).collect { progress ->
                _uiState.value = _uiState.value.copy(
                    models = _uiState.value.models.map { m ->
                        if (m.id == model.id) {
                            m.copy(
                                downloadProgress = progress,
                                isDownloaded = progress is DownloadProgress.Completed,
                                isPartial = progress is DownloadProgress.Downloading && progress.progressPercent < 1.0f,
                                isDownloadingActive = progress is DownloadProgress.Downloading
                            )
                        } else m
                    }
                )
                
                // Se completato con successo o in errore, aggiorna lo stato generale alla fine
                if (progress is DownloadProgress.Completed || progress is DownloadProgress.Error) {
                    refreshModelStates()
                }
            }
        }
    }

    /**
     * Elimina fisicamente il modello dal disco locale liberando memoria.
     * Se il modello rimosso era attivo in RAM, esegue anche l'unload.
     */
    fun deleteModel(model: AiModelInfo) {
        logInfo(TAG, "Rimozione modello richiesta per: ${model.name}")
        screenModelScope.launch(exceptionHandler) {
            val success = deleteModelUseCase(model.fileName)
            if (success) {
                // Se il modello era caricato in RAM, esegui l'unload
                val activePath = settings.getStringOrNull(KEY_ACTIVE_MODEL_PATH)
                val modelPath = modelRepository.getModelPath(model.fileName)
                if (activePath != null && activePath == modelPath) {
                    llamaRepository.unloadModel()
                    settings.remove(KEY_ACTIVE_MODEL_PATH)
                }

                // Rimuove dai modelli importati nelle impostazioni se era esterno
                if (model.isExternal) {
                    val importedStr = settings.getString(KEY_IMPORTED_MODELS, "")
                    val list = importedStr.split(",").toMutableList()
                    list.remove(model.fileName)
                    settings.putString(KEY_IMPORTED_MODELS, list.joinToString(","))
                }

                refreshModelStates()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Impossibile rimuovere il file modello.")
            }
        }
    }

    /**
     * Carica il modello selezionato direttamente nella memoria RAM del telefono.
     * Gestisce la corretta disattivazione del modello precedente.
     */
    fun activateModel(model: AiModelInfo) {
        if (!model.isDownloaded) return
        logInfo(TAG, "Caricamento modello in RAM richiesto per: ${model.name}")

        screenModelScope.launch(exceptionHandler) {
            // Segna il modello corrente come in fase di caricamento
            _uiState.value = _uiState.value.copy(
                models = _uiState.value.models.map { m ->
                    if (m.id == model.id) m.copy(isLoading = true) else m.copy(isActive = false)
                }
            )

            // 1. Assicura inizializzazione backend nativo llama.cpp
            initializeLlamaUseCase()

            // 2. Ottieni il percorso assoluto locale del file
            val modelPath = if (model.isExternal) {
                model.url // Il percorso assoluto locale è memorizzato qui per modelli importati
            } else {
                modelRepository.getModelPath(model.fileName) ?: ""
            }

            if (modelPath.isEmpty() || !modelRepository.isModelDownloaded(model.fileName)) {
                _uiState.value = _uiState.value.copy(errorMessage = "File modello non trovato sul disco.")
                refreshModelStates()
                return@launch
            }

            // 3. Esegui il caricamento in memoria (RAM) usando i thread suggeriti dall'hardware.
            // Pixel 8 Pro Tensor G3 → 6 thread; dispositivi entry-level → 2-4 thread.
            // n_ctx 1024 vs 2048: KV cache ~50% più piccola, attention ~2x più veloce.
            // 1024 token sono sufficienti per chat conversazionale (system prompt ~150 +
            // storia conversazione + risposta da 96 token).
            val hwThreads = hardwareResolver.getHardwareInfo().recommendedThreads
            val loadSuccess = loadModelUseCase(modelPath, contextSize = 1024, threads = hwThreads)
            logInfo(TAG, "Caricamento in RAM completato. Successo: $loadSuccess (threads=$hwThreads)")

            if (loadSuccess) {
                settings.putString(KEY_ACTIVE_MODEL_PATH, modelPath)
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Errore durante il caricamento del modello in RAM.")
            }
            refreshModelStates()
        }
    }

    /**
     * Consente di importare un file GGUF esterno precedentemente copiato o scaricato manualmente.
     * Aggiunge il nome file alle impostazioni e rinfresca lo stato per abilitarne l'uso immediato.
     */
    fun importExternalModel(absolutePath: String) {
        logInfo(TAG, "Importazione modello esterno da path: $absolutePath")
        val fileSeparator = if (absolutePath.contains("/")) '/' else '\\'
        val fileName = absolutePath.substringAfterLast(fileSeparator)
        
        if (fileName.isBlank() || !fileName.endsWith(".gguf", ignoreCase = true)) {
            _uiState.value = _uiState.value.copy(errorMessage = "Il file selezionato deve essere un modello valido in formato GGUF (.gguf)")
            return
        }

        val importedStr = settings.getString(KEY_IMPORTED_MODELS, "")
        val currentList = if (importedStr.isEmpty()) emptyList() else importedStr.split(",")
        if (!currentList.contains(fileName)) {
            val newList = currentList + fileName
            settings.putString(KEY_IMPORTED_MODELS, newList.joinToString(","))
        }
        
        // Segna come completato in quanto il file picker lo copia interamente su disco
        settings.putBoolean("completed_$fileName", true)

        refreshModelStates()
    }
}
