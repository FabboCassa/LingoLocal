package org.lingolocal.project.presentation.voice

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import org.lingolocal.project.data.platform.AudioRecorder
import org.lingolocal.project.data.platform.FileStorage
import org.lingolocal.project.data.whisper.WhisperEngine
import org.lingolocal.project.domain.model.DownloadProgress
import org.lingolocal.project.domain.repository.LlamaRepository
import org.lingolocal.project.domain.repository.ModelRepository
import org.lingolocal.project.domain.usecase.DownloadModelUseCase
import org.lingolocal.project.domain.usecase.StartAudioRecordingUseCase
import org.lingolocal.project.domain.usecase.StopAudioRecordingUseCase
import org.lingolocal.project.domain.usecase.TranscribeAudioUseCase
import org.lingolocal.project.domain.usecase.SpeakTextUseCase
import org.lingolocal.project.util.logError
import org.lingolocal.project.util.logInfo
import lingolocal.composeapp.generated.resources.Res
import lingolocal.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString

enum class Sender { USER, TUTOR }

data class VoiceMessage(
    val id: String,
    val sender: Sender,
    val text: String,
    val timestamp: Long
)

data class VoiceConversationUiState(
    val isRecording: Boolean = false,
    val isTranscribing: Boolean = false,
    val isAnalyzing: Boolean = false,
    val isSpeaking: Boolean = false,
    val statusMessage: String = "",
    val messages: List<VoiceMessage> = emptyList(),
    val micAmplitude: Float = 0.0f,
    val studyLanguage: String = "en", // Lingua di studio corrente
    val errorMessage: String? = null,
    /** True quando il modello Whisper (STT) sta caricando in RAM. */
    val isWhisperLoading: Boolean = false,
    /** True se Whisper non è scaricato e va richiesto il download. */
    val whisperMissing: Boolean = false,
    /** Progresso download Whisper (0.0..1.0) o null se non in download. */
    val whisperDownloadProgress: Float? = null
)

class VoiceConversationScreenModel(
    private val startAudioRecordingUseCase: StartAudioRecordingUseCase,
    private val stopAudioRecordingUseCase: StopAudioRecordingUseCase,
    private val transcribeAudioUseCase: TranscribeAudioUseCase,
    private val speakTextUseCase: SpeakTextUseCase,
    private val audioRecorder: AudioRecorder,
    private val llamaRepository: LlamaRepository,
    private val fileStorage: FileStorage,
    private val clock: Clock,
    private val whisperEngine: WhisperEngine,
    private val modelRepository: ModelRepository,
    private val downloadModelUseCase: DownloadModelUseCase
) : ScreenModel {

    private val _uiState = MutableStateFlow(VoiceConversationUiState())
    val uiState: StateFlow<VoiceConversationUiState> = _uiState.asStateFlow()

    private var amplitudeJob: Job? = null
    // Cambiato da .mp4 a .wav: il nuovo AudioRecorder scrive PCM 16-bit 16kHz mono in WAV.
    private val tempRecordingPath: String by lazy {
        "${fileStorage.getModelsDirectory()}/temp_recording.wav"
    }

    init {
        screenModelScope.launch {
            resetStatusToIdle()
            // Lazy load del modello Whisper (75 MB Q5_1) — può richiedere 200-500ms.
            ensureWhisperLoaded()
            // Inserisci il primo messaggio di benvenuto del Tutor AI in base alla lingua
            addTutorWelcomeMessage()
        }
    }

    /**
     * Carica Whisper in RAM se non già caricato. Se il file non esiste sul disco,
     * setta whisperMissing=true così la UI può mostrare "Scarica modello voce".
     */
    private suspend fun ensureWhisperLoaded() {
        if (whisperEngine.isLoaded()) return
        val whisperFile = WHISPER_MODEL_FILENAME
        if (!modelRepository.isModelDownloaded(whisperFile)) {
            logInfo(TAG, "Whisper non scaricato: $whisperFile")
            _uiState.update { it.copy(whisperMissing = true) }
            return
        }
        val path = modelRepository.getModelPath(whisperFile)
        if (path.isNullOrEmpty()) {
            logError(TAG, "Whisper path non risolto", null)
            _uiState.update { it.copy(whisperMissing = true) }
            return
        }
        _uiState.update { it.copy(isWhisperLoading = true, whisperMissing = false) }
        val ok = whisperEngine.loadModel(path, threads = 4)
        _uiState.update {
            it.copy(isWhisperLoading = false, whisperMissing = !ok)
        }
        if (!ok) {
            logError(TAG, "loadModel Whisper fallito", null)
        }
    }

    private suspend fun resetStatusToIdle() {
        val idleMsg = getString(Res.string.voice_practice_status_idle)
        _uiState.update { it.copy(statusMessage = idleMsg, errorMessage = null) }
    }

    private suspend fun addTutorWelcomeMessage() {
        val lang = _uiState.value.studyLanguage
        val welcomeText = when (lang.lowercase()) {
            "it" -> "Ciao! Sono il tuo insegnante di italiano. Premi il microfono e parliamo!"
            "es" -> "¡Hola! Soy tu profesor de español. ¡Mantén presionado el micrófono y hablemos!"
            "fr" -> "Bonjour ! Je suis ton professeur de français. Parle-moi en appuyant sur le micro !"
            "de" -> "Hallo! Ich bin dein Deutschlehrer. Lass uns sprechen! Drücke auf das Mikrofon."
            else -> "Hello! I am your AI language tutor. Let's practice English! Hold the mic and start talking."
        }
        
        val welcomeMsg = VoiceMessage(
            id = "welcome_${clock.now().toEpochMilliseconds()}",
            sender = Sender.TUTOR,
            text = welcomeText,
            timestamp = clock.now().toEpochMilliseconds()
        )
        
        _uiState.update { it.copy(messages = listOf(welcomeMsg)) }
        speakTextUseCase(welcomeText, lang)
    }

    fun onPermissionDenied(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun startRecording() {
        if (!audioRecorder.hasPermission()) {
            screenModelScope.launch {
                val errMsg = getString(Res.string.voice_practice_mic_permission_denied)
                _uiState.update { it.copy(errorMessage = errMsg) }
            }
            return
        }

        screenModelScope.launch {
            // Se l'utente ha scaricato Whisper dopo essere entrato in questa schermata
            // (es. via il Model Manager o il bottone in app), ricarichiamolo al volo
            // prima di iniziare a registrare.
            if (!whisperEngine.isLoaded()) {
                ensureWhisperLoaded()
                if (!whisperEngine.isLoaded()) {
                    _uiState.update {
                        it.copy(
                            whisperMissing = true,
                            errorMessage = "Scarica il modello voce (32 MB) per registrare."
                        )
                    }
                    return@launch
                }
            }

            try {
                // Interrompe eventuale TTS attiva prima di ascoltare l'utente
                speakTextUseCase.stop()

                val recMsg = getString(Res.string.voice_practice_status_recording)
                _uiState.update { 
                    it.copy(
                        isRecording = true,
                        statusMessage = recMsg,
                        errorMessage = null,
                        micAmplitude = 0.0f
                    )
                }

                startAudioRecordingUseCase(tempRecordingPath, _uiState.value.studyLanguage)
                startAmplitudePolling()
            } catch (e: Exception) {
                logError(TAG, "Errore all'avvio della registrazione", e)
                val errStatus = getString(Res.string.voice_practice_status_error, e.message ?: "")
                _uiState.update { it.copy(isRecording = false, errorMessage = errStatus) }
                resetStatusToIdle()
            }
        }
    }

    fun stopRecording() {
        if (!_uiState.value.isRecording) return

        stopAmplitudePolling()

        screenModelScope.launch {
            try {
                val transcribingMsg = getString(Res.string.voice_practice_status_transcribing)
                _uiState.update { 
                    it.copy(
                        isRecording = false,
                        isTranscribing = true,
                        statusMessage = transcribingMsg,
                        micAmplitude = 0.0f
                    )
                }

                // Ferma la registrazione PCM. I bytes ritornati sono un placeholder
                // (il file WAV completo è già stato scritto su disco). Whisper lo
                // legge direttamente dal path per evitare doppia copia in RAM.
                val audioBytes = stopAudioRecordingUseCase()
                if (audioBytes == null || audioBytes.isEmpty()) {
                    logInfo(TAG, "Nessun audio registrato o file vuoto.")
                    resetStatusToIdle()
                    _uiState.update { it.copy(isTranscribing = false) }
                    return@launch
                }

                // Esegui la trascrizione locale STT via Whisper (passa path WAV).
                val lang = _uiState.value.studyLanguage
                val transcription = transcribeAudioUseCase(tempRecordingPath, lang)
                
                if (transcription.isBlank()) {
                    val noSpeechMsg = when (_uiState.value.studyLanguage.lowercase()) {
                        "it" -> "Non ho capito. Riprova tenendo premuto il microfono e parla più chiaramente."
                        "es" -> "No te he entendido. Vuelve a intentarlo manteniendo el micrófono pulsado."
                        "fr" -> "Je n'ai pas compris. Réessaie en maintenant le micro appuyé."
                        "de" -> "Ich habe dich nicht verstanden. Versuch es nochmal mit gedrücktem Mikro."
                        else -> "I didn't catch that. Try again — hold the mic and speak clearly."
                    }
                    _uiState.update { it.copy(isTranscribing = false, errorMessage = noSpeechMsg) }
                    resetStatusToIdle()
                    return@launch
                }

                // Aggiungi messaggio dell'utente alla lista
                val userMsg = VoiceMessage(
                    id = "user_${clock.now().toEpochMilliseconds()}",
                    sender = Sender.USER,
                    text = transcription,
                    timestamp = clock.now().toEpochMilliseconds()
                )

                _uiState.update { 
                    it.copy(
                        messages = it.messages + userMsg,
                        isTranscribing = false,
                        isAnalyzing = true,
                        statusMessage = getString(Res.string.voice_practice_status_generating)
                    )
                }

                // Genera la risposta dell'insegnante AI (Llama o Fallback)
                generateTutorResponse(transcription)

            } catch (e: Exception) {
                logError(TAG, "Errore durante il salvataggio o elaborazione audio", e)
                val errStatus = getString(Res.string.voice_practice_status_error, e.message ?: "")
                _uiState.update { 
                    it.copy(
                        isRecording = false,
                        isTranscribing = false,
                        isAnalyzing = false,
                        errorMessage = errStatus
                    )
                }
                resetStatusToIdle()
            }
        }
    }

    private suspend fun generateTutorResponse(userPrompt: String) {
        val lang = _uiState.value.studyLanguage
        try {
            var tutorReply = ""

            if (llamaRepository.isReady()) {
                val systemPrompt = buildTutorSystemPrompt(lang)

                val responseStringBuilder = StringBuilder()
                // Usa generateChat: il backend applica il chat template specifico
                // del modello caricato (Gemma <start_of_turn>, Qwen <|im_start|>...).
                // Niente più doppio system prompt né ruoli confusi.
                llamaRepository.generateChat(
                    systemPrompt = systemPrompt,
                    userMessage = userPrompt,
                    maxTokens = 96
                ).collect { token ->
                    responseStringBuilder.append(token)
                }
                tutorReply = sanitizeTutorReply(responseStringBuilder.toString())
            }

            // Fallback ad alta fedeltà se llama non è carico
            if (tutorReply.isBlank()) {
                delay(1200) // Simula la riflessione del tutor
                tutorReply = getSimulatedTutorReply(userPrompt, lang)
            }

            val tutorMsg = VoiceMessage(
                id = "tutor_${clock.now().toEpochMilliseconds()}",
                sender = Sender.TUTOR,
                text = tutorReply,
                timestamp = clock.now().toEpochMilliseconds()
            )

            _uiState.update { 
                it.copy(
                    messages = it.messages + tutorMsg,
                    isAnalyzing = false,
                    isSpeaking = true,
                    statusMessage = getString(Res.string.voice_practice_status_speaking)
                )
            }

            // Fai parlare il tutor offline nativamente!
            speakTextUseCase(tutorReply, lang)

            // Monitoriamo l'ascolto per resettare lo stato quando finisce
            screenModelScope.launch {
                while (speakTextUseCase.isSpeaking() /* monitora lo stato della voce nativa */) {
                    delay(300)
                }
                // Diamo un piccolo delay prima di tornare Idle
                delay(1000)
                _uiState.update { it.copy(isSpeaking = false) }
                resetStatusToIdle()
            }

        } catch (e: Exception) {
            logError(TAG, "Errore nella generazione risposta del tutor", e)
            val errStatus = getString(Res.string.voice_practice_status_error, e.message ?: "")
            _uiState.update { 
                it.copy(
                    isAnalyzing = false,
                    isSpeaking = false,
                    errorMessage = errStatus
                )
            }
            resetStatusToIdle()
        }
    }

    /**
     * Costruisce il prompt nel formato chat di Gemma (instruction-tuned).
     * Funziona anche con Qwen / altri instruct: il template è semplice e non
     * confonde i sampler. Le istruzioni sono brevi e VIETANO esplicitamente
     * la chain-of-thought visibile (che è ciò che dumpava "Thinking Process:").
     */
    private fun buildTutorSystemPrompt(lang: String): String {
        val langName = when (lang.lowercase()) {
            "it" -> "Italian"
            "es" -> "Spanish"
            "fr" -> "French"
            "de" -> "German"
            else -> "English"
        }
        // System prompt SOLO. La formattazione chat (ruoli, separator token,
        // generation prompt) la fa il backend nativo applicando il template
        // del modello caricato. Manteniamo il system compatto per minimizzare
        // i token da processare ad ogni turno.
        return "You are a friendly $langName conversation tutor. " +
            "The user's message comes from speech-to-text, so it has no punctuation. " +
            "Treat sentences starting with what/who/where/when/why/how/can/do/is/are as questions. " +
            "Reply ONLY in $langName, in 1-2 short natural sentences (max 25 words). " +
            "If the user greets, greet back and ask a friendly opener. " +
            "If they ask a question, answer it directly. " +
            "If you spot a clear grammar mistake, briefly give the correct form. " +
            "Never output analysis, headings, bullets, or any special tokens."
    }

    /**
     * Rimuove dal testo generato eventuali artefatti di reasoning/chat-template.
     * - Blocchi `<|channel|>...<|message|>` (formato Harmony / gpt-oss)
     * - Blocchi `<think>...</think>` (DeepSeek-style reasoning)
     * - Tag `<start_of_turn>` / `<end_of_turn>` (Gemma)
     * - Prefissi tipo "Thinking Process:" e numerazione di analisi
     */
    private fun sanitizeTutorReply(raw: String): String {
        var s = raw

        // 1. Gemma 4 / reasoning models: il vero output sta DOPO il blocco
        //    <|channel>thought ... <channel|>. Se troviamo la chiusura del thought
        //    block, scartiamo tutto ciò che la precede.
        val closeMarkers = listOf("<channel|>", "<|channel|>final", "<|message|>", "</think>")
        for (m in closeMarkers) {
            val idx = s.lastIndexOf(m, ignoreCase = true)
            if (idx >= 0) {
                s = s.substring(idx + m.length)
                break
            }
        }

        // 2. Blocchi <think>...</think> residui (DeepSeek-style)
        s = Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE).replace(s, "")

        // 3. Tag generici Harmony / Gemma 4 (con o senza pipe di chiusura)
        s = Regex("<\\|[^|>\\n]{0,40}\\|>").replace(s, "")            // <|...|>
        s = Regex("<\\|[a-zA-Z_]{1,30}>").replace(s, "")              // <|...>
        s = Regex("<[a-zA-Z_]{1,30}\\|>").replace(s, "")              // <...|>
        s = Regex("<\\|turn\\|?>(system|user|model|assistant)?", RegexOption.IGNORE_CASE).replace(s, "")

        // 4. Tag Gemma 2/3
        s = s.replace("<start_of_turn>model", "", ignoreCase = true)
            .replace("<start_of_turn>user", "", ignoreCase = true)
            .replace("<end_of_turn>", "", ignoreCase = true)
            .replace("<start_of_turn>", "", ignoreCase = true)

        // 5. Tag Llama/ChatML/BOS residui
        s = Regex("<\\|im_(start|end)\\|>[^\\n]*", RegexOption.IGNORE_CASE).replace(s, "")
        s = Regex("</?s>|<bos>|<eos>", RegexOption.IGNORE_CASE).replace(s, "")

        // 6. Se il modello ha emesso SOLO reasoning in chiaro (no closing marker),
        //    tronchiamo da marker tipici di analisi.
        val thinkMarkers = listOf(
            "Thinking Process:", "Thinking process:", "Analysis:", "Reasoning:",
            "Step 1:", "Step 1.", "**Analyze", "**Determine", "**Formulate",
            "1. **", "Grammar Check:"
        )
        for (marker in thinkMarkers) {
            val idx = s.indexOf(marker, ignoreCase = true)
            if (idx >= 0) s = s.substring(0, idx)
        }

        // 7. Prefissi "Tutor:" / "Model:" / "Assistant:" / "User:" all'inizio riga
        s = Regex("(?im)^(tutor|model|assistant|user)\\s*:\\s*").replace(s, "")

        // 8. Whitespace cleanup
        s = s.replace(Regex("\\n{2,}"), "\n").trim()

        // 9. Se resta solo qualche parola spuria → vuoto (cade sul fallback).
        if (s.length < 4 || s.lowercase() in setOf("thought", "message", "channel", "thinking", "final")) {
            return ""
        }
        return s
    }

    private fun getSimulatedTutorReply(userPrompt: String, lang: String): String {
        val userPromptLower = userPrompt.lowercase()
        return when (lang.lowercase()) {
            "it" -> {
                when {
                    userPromptLower.contains("ciao") || userPromptLower.contains("salve") -> 
                        "Ciao! Che piacere sentirti. Come sta andando la tua giornata?"
                    userPromptLower.contains("prenotare") || userPromptLower.contains("tavolo") -> 
                        "Ottima scelta! Prenotare in italiano è semplice. La tua frase è corretta! A che ora vorresti cenare?"
                    userPromptLower.contains("differenza") || userPromptLower.contains("passato") -> 
                        "Domanda fantastica! Il passato prossimo si usa per azioni concluse e vicine nel tempo, mentre l'imperfetto descrive stati d'animo o abitudini passate. Ad esempio: 'Ieri ho mangiato una pizza' contro 'Da piccolo mangiavo sempre la pizza'. Chiaro?"
                    else -> "Molto interessante! La tua pronuncia e la costruzione della frase sono ottime. Continuiamo a parlare!"
                }
            }
            "es" -> {
                when {
                    userPromptLower.contains("hola") -> 
                        "¡Hola! Qué gusto saludarte. ¿Cómo estás hoy y de qué te gustaría hablar?"
                    userPromptLower.contains("estación") || userPromptLower.contains("tren") -> 
                        "¡Muy bien preguntado! Para buscar la estación puedes decir: '¿Dónde queda la estación de tren?'. Tu pronunciación es excelente."
                    userPromptLower.contains("consejo") || userPromptLower.contains("pronunciación") -> 
                        "Mi consejo clave es escuchar audios y repetir en voz alta. ¡Lo estás haciendo genial hoy!"
                    else -> "¡Excelente! Has estructurado la frase de manera muy natural. ¿De qué más te gustaría conversar?"
                }
            }
            "fr" -> {
                when {
                    userPromptLower.contains("bonjour") -> 
                        "Bonjour ! Comment vas-tu aujourd'hui ? Prêt à pratiquer ton français ?"
                    userPromptLower.contains("croissant") || userPromptLower.contains("café") -> 
                        "Délicieux ! Ta commande est parfaite. En France, on dit souvent : 'Un croissant et un café, s'il vous plaît !' pour être poli. Bravo !"
                    userPromptLower.contains("subjonctif") || userPromptLower.contains("irrégulier") -> 
                        "Ah, le subjonctif ! Par exemple, pour il faut que tu fasses (du verbe faire) ou il faut que tu sois (du verbe être). C'est un peu difficile mais tu t'en sors très bien !"
                    else -> "Magnifique ! Ta phrase est très bien construite et ton accent est charmant. Continue comme ça !"
                }
            }
            "de" -> {
                when {
                    userPromptLower.contains("hallo") -> 
                        "Hallo! Wie geht es dir heute? Worüber möchten wir sprechen?"
                    userPromptLower.contains("nächste") || userPromptLower.contains("station") -> 
                        "Sehr gut! Deine Frage ist absolut verständlich. Du hast die Satzstruktur perfekt gemeistert."
                    userPromptLower.contains("dativ") || userPromptLower.contains("akkusativ") -> 
                        "Gute Frage! Der Dativ antwortet auf 'Wo' (z.B. 'in der Schule') und der Akkusativ auf 'Wohin' (z.B. 'in die Schule'). Das ist ein super Fortschritt!"
                    else -> "Hervorragend! Dein Deutsch klingt schon sehr flüssend. Lass uns weiter üben!"
                }
            }
            else -> { // English
                when {
                    userPromptLower.contains("hello") || userPromptLower.contains("hi ") || userPromptLower.contains("hi!") -> 
                        "Hello! It is wonderful to hear from you. How has your day been so far?"
                    userPromptLower.contains("coffee") || userPromptLower.contains("order") -> 
                        "Great job! Ordering coffee in English is very common. Your sentence is perfect! Would you like milk with that?"
                    userPromptLower.contains("grammar") || userPromptLower.contains("perfect") || userPromptLower.contains("past") -> 
                        "Great question! We use the simple past for finished actions in the past (e.g. 'I visited Rome in 2024'). We use the present perfect for actions connected to the present (e.g. 'I have visited Rome twice'). Got it?"
                    else -> "Wonderful! Your sentence layout is clear and correct. You have a very good pronunciation. Let's keep practicing!"
                }
            }
        }
    }

    fun changeLanguage(langCode: String) {
        screenModelScope.launch {
            speakTextUseCase.stop()
            _uiState.update { 
                it.copy(
                    studyLanguage = langCode,
                    messages = emptyList()
                ) 
            }
            addTutorWelcomeMessage()
        }
    }

    private fun startAmplitudePolling() {
        amplitudeJob?.cancel()
        amplitudeJob = screenModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(50)
                val amp = audioRecorder.getAmplitude()
                _uiState.update { it.copy(micAmplitude = amp) }
            }
        }
    }

    private fun stopAmplitudePolling() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        _uiState.update { it.copy(micAmplitude = 0.0f) }
    }

    /**
     * Tenta di ricaricare Whisper dopo che l'utente ha scaricato il modello
     * dal Model Manager e torna alla schermata voice chat.
     */
    fun retryLoadWhisper() {
        screenModelScope.launch { ensureWhisperLoaded() }
    }

    /**
     * Scarica direttamente il modello Whisper Tiny dalla schermata voice chat,
     * mostrando il progresso (whisperDownloadProgress). Al completamento carica
     * automaticamente il modello in RAM.
     *
     * Pensato per il caso in cui l'utente entra in voice chat senza aver mai
     * scaricato Whisper: dal bottone "Scarica modello voce (32 MB)".
     */
    fun downloadWhisperModel() {
        screenModelScope.launch {
            _uiState.update { it.copy(whisperDownloadProgress = 0f, errorMessage = null) }
            try {
                downloadModelUseCase(WHISPER_MODEL_URL, WHISPER_MODEL_FILENAME).collect { p ->
                    when (p) {
                        is DownloadProgress.Downloading -> {
                            _uiState.update { it.copy(whisperDownloadProgress = p.progressPercent) }
                        }
                        is DownloadProgress.Completed -> {
                            logInfo(TAG, "Whisper downloaded, loading in RAM...")
                            _uiState.update {
                                it.copy(whisperDownloadProgress = 1f, isWhisperLoading = true)
                            }
                            ensureWhisperLoaded()
                            _uiState.update {
                                it.copy(whisperDownloadProgress = null, whisperMissing = !whisperEngine.isLoaded())
                            }
                        }
                        is DownloadProgress.Error -> {
                            logError(TAG, "Whisper download error: ${p.message}", null)
                            _uiState.update {
                                it.copy(
                                    whisperDownloadProgress = null,
                                    errorMessage = "Download fallito: ${p.message}"
                                )
                            }
                        }
                        DownloadProgress.Idle -> { /* no-op */ }
                    }
                }
            } catch (t: Throwable) {
                logError(TAG, "Whisper download threw", t)
                _uiState.update {
                    it.copy(
                        whisperDownloadProgress = null,
                        errorMessage = "Download fallito: ${t.message ?: "errore sconosciuto"}"
                    )
                }
            }
        }
    }

    override fun onDispose() {
        amplitudeJob?.cancel()
        audioRecorder.release()
        screenModelScope.launch {
            speakTextUseCase.stop()
            // Libera la RAM occupata da Whisper quando l'utente esce dalla chat vocale.
            whisperEngine.unloadModel()
        }
        super.onDispose()
    }

    private companion object {
        private const val TAG = "VoiceConversationScreenModel"
        private const val WHISPER_MODEL_FILENAME = "ggml-tiny-q5_1.bin"
        private const val WHISPER_MODEL_URL =
            "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny-q5_1.bin"
    }
}
