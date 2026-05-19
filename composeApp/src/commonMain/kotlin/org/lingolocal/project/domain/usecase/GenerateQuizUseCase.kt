package org.lingolocal.project.domain.usecase

import kotlinx.coroutines.flow.fold
import kotlinx.serialization.json.Json
import org.lingolocal.project.domain.model.Quiz
import org.lingolocal.project.domain.model.QuizQuestion
import org.lingolocal.project.domain.model.QuizFocus
import org.lingolocal.project.domain.repository.LlamaRepository
import org.lingolocal.project.util.logDebug
import org.lingolocal.project.util.logError
import org.lingolocal.project.util.logInfo

/**
 * Caso d'uso per la generazione di quiz a risposta multipla partendo da un testo/contesto.
 * Istruisce l'LLM tramite un prompt di sistema rigido per produrre solo JSON strutturato.
 * Include logica di estrazione robusta per superare le imperfezioni dei piccoli modelli on-device,
 * oltre a un meccanismo di retry (fino a 3 tentativi) e di fallback per evitare crash dell'applicazione.
 */
class GenerateQuizUseCase(
    private val llamaRepository: LlamaRepository,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }
) {

    suspend operator fun invoke(
        contextText: String,
        numQuestions: Int = 3,
        focus: QuizFocus = QuizFocus.GENERAL,
        maxRetries: Int = 3,
        maxTokens: Int = 512
    ): Quiz {
        if (!llamaRepository.isReady()) {
            throw IllegalStateException("Il modello LLM locale non è caricato in memoria.")
        }
        if (contextText.isBlank()) {
            return generateFallbackQuiz("Nessun contesto fornito.")
        }

        val prompt = buildPrompt(contextText, numQuestions, focus)

        for (attempt in 1..maxRetries) {
            try {
                logInfo(TAG, "Tentativo di generazione quiz: $attempt di $maxRetries")
                
                val rawResponse = llamaRepository.generate(prompt, maxTokens)
                    .fold("") { acc, piece -> acc + piece }
                
                logDebug(TAG, "Risposta grezza LLM: $rawResponse")

                val cleanedJson = extractJson(rawResponse)
                logDebug(TAG, "JSON estratto per il parsing: $cleanedJson")

                val quiz = json.decodeFromString<Quiz>(cleanedJson)
                
                // Validiamo la correttezza del quiz generato
                validateQuiz(quiz)
                
                logInfo(TAG, "Quiz generato e validato correttamente al tentativo $attempt")
                return quiz
            } catch (t: Throwable) {
                logError(TAG, "Errore durante il tentativo $attempt: ${t.message}", t)
            }
        }

        // Se tutti i tentativi falliscono, restituiamo un quiz di fallback per una UX fluida
        logError(TAG, "Tutti i tentativi di generazione sono falliti. Genero il quiz di fallback.", null)
        return generateFallbackQuiz(contextText)
    }

    private fun buildPrompt(context: String, numQuestions: Int, focus: QuizFocus): String {
        val focusInstruction = when (focus) {
            QuizFocus.GENERAL -> "Generate a balanced, comprehensive quiz about the main points."
            QuizFocus.GRAMMAR -> "Focus intensely on testing grammar rules, syntax, conjugations, and structural mechanics."
            QuizFocus.VOCABULARY -> "Focus intensely on testing vocabulary, word meanings, synonyms, idioms, and definitions."
            QuizFocus.WITH_EXAMPLES -> "Generate realistic, practical examples and contextual situations for each question."
        }
        return """
            You are an expert language teacher. Generate a multiple-choice quiz based ONLY on the provided context.
            You must respond EXCLUSIVELY with a JSON object. Do not write any introduction, explanation, markdown formatting, or code blocks.
            
            Focus instructions: $focusInstruction
            
            The JSON object must match this schema exactly:
            {
              "title": "Quiz Title",
              "questions": [
                {
                  "question": "The question text?",
                  "options": ["Option 1", "Option 2", "Option 3", "Option 4"],
                  "correctOptionIndex": 0,
                  "explanation": "Brief explanation of why Option 1 is correct."
                }
              ]
            }
            Ensure the correctOptionIndex is a 0-based index of the correct option in the options array.
            
            Context:
            $context
            
            Generate a quiz with exactly $numQuestions multiple choice questions in the same language as the context.
        """.trimIndent()
    }

    /**
     * Pulisce l'output dell'LLM isolando il blocco JSON principale.
     * Rimuove i blocchi di codice markdown (```json o ```) e qualsiasi introduzione/conclusione.
     */
    fun extractJson(raw: String): String {
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

    private fun validateQuiz(quiz: Quiz) {
        require(quiz.title.isNotBlank()) { "Il titolo del quiz non può essere vuoto." }
        require(quiz.questions.isNotEmpty()) { "Il quiz deve contenere almeno una domanda." }
        for ((idx, q) in quiz.questions.withIndex()) {
            require(q.question.isNotBlank()) { "La domanda $idx non può essere vuota." }
            require(q.options.size >= 2) { "La domanda $idx deve avere almeno due opzioni." }
            require(q.correctOptionIndex in q.options.indices) {
                "L'indice di risposta corretta ${q.correctOptionIndex} per la domanda $idx è fuori dai limiti."
            }
        }
    }

    private fun generateFallbackQuiz(contextText: String): Quiz {
        val snippet = if (contextText.length > 60) contextText.take(60) + "..." else contextText
        return Quiz(
            title = "Quiz di Ripasso (Fallback)",
            questions = listOf(
                QuizQuestion(
                    question = "Quale dei seguenti argomenti è trattato nel testo fornito?",
                    options = listOf(
                        "L'argomento descritto in: $snippet",
                        "Un argomento del tutto irrilevante",
                        "Nessuna delle risposte fornite",
                        "La storia della fisica nucleare"
                    ),
                    correctOptionIndex = 0,
                    explanation = "Questo quiz è stato generato in modalità di recupero a causa di problemi tecnici nella generazione automatica."
                )
            )
        )
    }

    companion object {
        private const val TAG = "GenerateQuizUseCase"
    }
}
