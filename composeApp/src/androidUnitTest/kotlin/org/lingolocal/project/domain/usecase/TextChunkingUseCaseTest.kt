package org.lingolocal.project.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test Task 2.3: text chunking sentence-aware.
 *
 * Verifica:
 *   - input blank -> lista vuota
 *   - 3 paragrafi vengono spezzati in chunk multipli, rispettando il limite token
 *   - i delimitatori di frase (. ! ? \n) producono boundary corretti
 *   - una frase più lunga di maxTokens diventa comunque un chunk autonomo
 */
class TextChunkingUseCaseTest {

    private val chunker = TextChunkingUseCase()

    @Test
    fun `input vuoto o blank restituisce lista vuota`() {
        assertTrue(chunker("").isEmpty())
        assertTrue(chunker("   \n\t  ").isEmpty())
    }

    @Test
    fun `frase singola breve produce un unico chunk`() {
        val text = "Questa è una frase breve."
        val chunks = chunker(text, maxTokens = 100)
        assertEquals(1, chunks.size)
        assertEquals(text, chunks[0])
    }

    @Test
    fun `tre paragrafi lunghi vengono spezzati in piu chunk`() {
        // ~120 char per paragrafo => ~30 token stimati a chunk (chars/4)
        val p1 = (1..6).joinToString(" ") { "Prima parte frase $it con contenuto." }
        val p2 = (1..6).joinToString(" ") { "Seconda parte frase $it con contenuto." }
        val p3 = (1..6).joinToString(" ") { "Terza parte frase $it con contenuto." }
        val text = "$p1\n\n$p2\n\n$p3"

        // maxTokens basso => costringe più chunk
        val chunks = chunker(text, maxTokens = 30)
        assertTrue("attesi più chunk, ottenuti ${chunks.size}", chunks.size >= 2)
        // Nessun chunk vuoto
        assertFalse(chunks.any { it.isBlank() })
        // Concatenando i chunk si recupera tutto il contenuto delle frasi
        val joined = chunks.joinToString(" ")
        assertTrue(joined.contains("Prima parte frase 1"))
        assertTrue(joined.contains("Seconda parte frase 1"))
        assertTrue(joined.contains("Terza parte frase 6"))
    }

    @Test
    fun `frase piu grande di maxTokens diventa un chunk a se`() {
        val longSentence = (1..50).joinToString(" ") { "parola$it" } + "."
        val chunks = chunker(longSentence, maxTokens = 10)
        // Non spezziamo a metà la frase: 1 chunk con tutta la frase
        assertEquals(1, chunks.size)
        assertEquals(longSentence, chunks[0])
    }

    @Test
    fun `delimitatori punto esclamativo interrogativo e newline producono boundary corretti`() {
        val text = "Prima frase. Seconda frase! Terza frase?\nQuarta frase finale."
        val chunks = chunker(text, maxTokens = 5) // forza split aggressivo
        assertTrue("attesi >=2 chunk, ottenuti ${chunks.size}", chunks.size >= 2)
        // Il primo chunk inizia con "Prima frase"
        assertTrue(chunks.first().startsWith("Prima frase"))
        // L'ultimo chunk contiene "Quarta frase finale"
        assertTrue(chunks.last().contains("Quarta frase finale"))
    }
}
