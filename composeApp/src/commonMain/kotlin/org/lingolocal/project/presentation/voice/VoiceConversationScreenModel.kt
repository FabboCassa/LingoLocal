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
import org.lingolocal.project.domain.repository.LlamaRepository
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
    val errorMessage: String? = null
)

class VoiceConversationScreenModel(
    private val startAudioRecordingUseCase: StartAudioRecordingUseCase,
    private val stopAudioRecordingUseCase: StopAudioRecordingUseCase,
    private val transcribeAudioUseCase: TranscribeAudioUseCase,
    private val speakTextUseCase: SpeakTextUseCase,
    private val audioRecorder: AudioRecorder,
    private val llamaRepository: LlamaRepository,
    private val fileStorage: FileStorage,
    private val clock: Clock
) : ScreenModel {

    private val _uiState = MutableStateFlow(VoiceConversationUiState())
    val uiState: StateFlow<VoiceConversationUiState> = _uiState.asStateFlow()

    private var amplitudeJob: Job? = null
    private val tempRecordingPath: String by lazy {
        "${fileStorage.getModelsDirectory()}/temp_recording.mp4"
    }

    init {
        screenModelScope.launch {
            resetStatusToIdle()
            // Inserisci il primo messaggio di benvenuto del Tutor AI in base alla lingua
            addTutorWelcomeMessage()
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

                val audioBytes = stopAudioRecordingUseCase()
                if (audioBytes == null || audioBytes.isEmpty()) {
                    logInfo(TAG, "Nessun audio registrato o file vuoto.")
                    resetStatusToIdle()
                    _uiState.update { it.copy(isTranscribing = false) }
                    return@launch
                }

                // Esegui la trascrizione locale STT
                val lang = _uiState.value.studyLanguage
                val transcription = transcribeAudioUseCase(audioBytes, lang)
                
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
                val fullPrompt = buildGemmaChatPrompt(lang, userPrompt)

                val responseStringBuilder = StringBuilder()
                llamaRepository.generate(prompt = fullPrompt, maxTokens = 96).collect { token ->
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
    private fun buildGemmaChatPrompt(lang: String, userPrompt: String): String {
        val langName = when (lang.lowercase()) {
            "it" -> "Italian"
            "es" -> "Spanish"
            "fr" -> "French"
            "de" -> "German"
            else -> "English"
        }
        val system = "You are a friendly $langName language tutor. " +
            "Reply ONLY in $langName, in 1-2 short sentences. " +
            "If the user's sentence has a clear grammar mistake, gently give the correct version, then ask a short follow-up question. " +
            "If it is correct, briefly praise and ask a follow-up question. " +
            "Do NOT write analysis, thinking, headings, bullet points, or 'Thinking Process'. Just speak naturally."

        return buildString {
            append("<start_of_turn>user\n")
            append(system)
            append("\n\nUser said: \"")
            append(userPrompt)
            append("\"<end_of_turn>\n")
            append("<start_of_turn>model\n")
        }
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
        // 1. Rimuovi blocchi <think>...</think> completi
        s = Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE).replace(s, "")
        // 2. Rimuovi blocchi Harmony: <|channel|>thought<|message|>...<|end|>
        s = Regex("<\\|channel\\|>[\\s\\S]*?<\\|message\\|>", RegexOption.IGNORE_CASE).replace(s, "")
        s = Regex("<\\|[^|>]*\\|>").replace(s, "")
        // 3. Rimuovi tag Gemma
        s = s.replace("<start_of_turn>model", "", ignoreCase = true)
            .replace("<start_of_turn>user", "", ignoreCase = true)
            .replace("<end_of_turn>", "", ignoreCase = true)
            .replace("<start_of_turn>", "", ignoreCase = true)
        // 4. Se il modello ha aperto un "Thinking Process:" o simile, scarta tutto
        //    finché non troviamo una riga conversazionale "vera"
        val thinkMarkers = listOf("Thinking Process:", "Analysis:", "Reasoning:", "Step 1:")
        for (marker in thinkMarkers) {
            val idx = s.indexOf(marker, ignoreCase = true)
            if (idx >= 0) {
                // Tutto fino al marker è da scartare; dopo il marker, prendiamo solo
                // l'eventuale linea finale "conversazionale" se presente
                s = s.substring(0, idx)
            }
        }
        // 5. Pulizia righe vuote multiple e prefissi "Tutor:" / "Model:" / "Assistant:"
        s = Regex("(?im)^(tutor|model|assistant)\\s*:\\s*").replace(s, "")
        s = s.replace(Regex("\\n{2,}"), "\n").trim()
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

    override fun onDispose() {
        amplitudeJob?.cancel()
        audioRecorder.release()
        screenModelScope.launch {
            speakTextUseCase.stop()
        }
        super.onDispose()
    }

    private companion object {
        private const val TAG = "VoiceConversationScreenModel"
    }
}
