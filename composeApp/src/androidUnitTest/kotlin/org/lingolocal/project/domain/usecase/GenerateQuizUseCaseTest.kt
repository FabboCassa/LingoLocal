package org.lingolocal.project.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.lingolocal.project.domain.repository.LlamaRepository

class GenerateQuizUseCaseTest {

    private class SimpleFakeLlamaRepository(
        private val response: String,
        private val ready: Boolean = true
    ) : LlamaRepository {
        override suspend fun initialize() {}
        override suspend fun loadModel(modelPath: String, contextSize: Int, threads: Int) = true
        override fun isReady() = ready
        override fun generate(prompt: String, imageBytes: ByteArray?, maxTokens: Int): Flow<String> = flowOf(response)
        override suspend fun unloadModel() {}
        override suspend fun embed(text: String): FloatArray? = null
    }

    private class MultiResponseFakeLlamaRepository(
        private val responses: List<String>
    ) : LlamaRepository {
        var callCount = 0
        override suspend fun initialize() {}
        override suspend fun loadModel(modelPath: String, contextSize: Int, threads: Int) = true
        override fun isReady() = true
        override fun generate(prompt: String, imageBytes: ByteArray?, maxTokens: Int): Flow<String> {
            val resp = if (callCount < responses.size) responses[callCount] else "malformed"
            callCount++
            return flowOf(resp)
        }
        override suspend fun unloadModel() {}
        override suspend fun embed(text: String): FloatArray? = null
    }

    private val validQuizJson = """
        {
          "title": "Verbo Essere",
          "questions": [
            {
              "question": "Qual è la prima persona singolare di essere?",
              "options": ["sono", "sei", "è", "siamo"],
              "correctOptionIndex": 0,
              "explanation": "La prima persona singolare è 'io sono'."
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `lancia eccezione se il modello non e pronto`() = runTest {
        val llama = SimpleFakeLlamaRepository(response = "", ready = false)
        val useCase = GenerateQuizUseCase(llama)

        try {
            useCase("contesto")
            fail("Dovrebbe lanciare IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("non è caricato"))
        }
    }

    @Test
    fun `estrae correttamente il JSON anche se avvolto in markdown code blocks`() = runTest {
        val llama = SimpleFakeLlamaRepository(response = "")
        val useCase = GenerateQuizUseCase(llama)

        val rawWithMarkdown = "Ecco il quiz:\n```json\n$validQuizJson\n```\nSpero sia utile."
        val extracted = useCase.extractJson(rawWithMarkdown)
        assertTrue(extracted.startsWith("{"))
        assertTrue(extracted.endsWith("}"))
        assertTrue(extracted.contains("Verbo Essere"))
    }

    @Test
    fun `genera correttamente il quiz se la risposta e valida`() = runTest {
        val llama = SimpleFakeLlamaRepository(response = validQuizJson)
        val useCase = GenerateQuizUseCase(llama)

        val quiz = useCase("Il verbo essere è irregolare.")
        assertNotNull(quiz)
        assertEquals("Verbo Essere", quiz.title)
        assertEquals(1, quiz.questions.size)
        assertEquals("Qual è la prima persona singolare di essere?", quiz.questions[0].question)
        assertEquals(0, quiz.questions[0].correctOptionIndex)
    }

    @Test
    fun `esegue il retry se il primo tentativo fallisce ma il secondo ha successo`() = runTest {
        val llama = MultiResponseFakeLlamaRepository(
            responses = listOf(
                "Questa risposta non è un JSON valido!",
                validQuizJson
            )
        )
        val useCase = GenerateQuizUseCase(llama)

        val quiz = useCase("Il verbo essere.")
        assertEquals(2, llama.callCount) // Primo tentativo fallito, secondo ok
        assertEquals("Verbo Essere", quiz.title)
    }

    @Test
    fun `restituisce un quiz di fallback se tutti i tentativi falliscono`() = runTest {
        val llama = MultiResponseFakeLlamaRepository(
            responses = listOf(
                "risposta non valida 1",
                "risposta non valida 2",
                "risposta non valida 3"
            )
        )
        val useCase = GenerateQuizUseCase(llama)

        val quiz = useCase("Contesto per fallback.")
        assertEquals(3, llama.callCount) // Ha provato 3 volte (maxRetries)
        assertEquals("Quiz di Ripasso (Fallback)", quiz.title)
        assertEquals(1, quiz.questions.size)
        assertTrue(quiz.questions[0].explanation.contains("modalità di recupero"))
    }
}
