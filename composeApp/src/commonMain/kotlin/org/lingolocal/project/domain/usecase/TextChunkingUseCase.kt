package org.lingolocal.project.domain.usecase

/**
 * Suddivide un testo libero in chunk pronti per la pipeline RAG.
 *
 * Strategia (Task 2.3):
 *   1. Split su confini di frase: `.`, `!`, `?`, `\n` — i delimitatori vengono
 *      mantenuti come terminatori della frase corrente.
 *   2. Aggregazione delle frasi in chunk finché il conteggio token stimato
 *      resta sotto [maxTokens] (default 250). Una frase singola che supera
 *      la soglia diventa comunque un chunk a sé (non viene spezzata a metà
 *      per non perdere il contesto sintattico).
 *
 * Stima token: euristica `chars / CHARS_PER_TOKEN` (≈ 4 char per token su
 * lingue latine). Sufficiente per dimensionare i chunk rispetto alla context
 * window dei modelli quantizzati (2048-4096 token); il valore esatto verrà
 * comunque ri-tokenizzato dall'engine prima dell'embedding.
 *
 * Output: lista di stringhe non vuote, con whitespace ai bordi rimosso.
 * Su input blank restituisce lista vuota.
 */
class TextChunkingUseCase {

    operator fun invoke(
        text: String,
        maxTokens: Int = DEFAULT_MAX_TOKENS
    ): List<String> {
        require(maxTokens > 0) { "maxTokens deve essere > 0 (era $maxTokens)" }
        if (text.isBlank()) return emptyList()

        val sentences = splitSentences(text)
        if (sentences.isEmpty()) return emptyList()

        val chunks = mutableListOf<String>()
        val current = StringBuilder()
        var currentTokens = 0

        for (sentence in sentences) {
            val sentenceTokens = estimateTokens(sentence)
            val wouldExceed = currentTokens + sentenceTokens > maxTokens
            if (current.isNotEmpty() && wouldExceed) {
                chunks.add(current.toString().trim())
                current.clear()
                currentTokens = 0
            }
            if (current.isNotEmpty()) current.append(' ')
            current.append(sentence)
            currentTokens += sentenceTokens
        }
        if (current.isNotEmpty()) chunks.add(current.toString().trim())
        return chunks
    }

    private fun splitSentences(text: String): List<String> {
        val out = mutableListOf<String>()
        var start = 0
        for (i in text.indices) {
            val c = text[i]
            if (c == '.' || c == '!' || c == '?' || c == '\n') {
                val s = text.substring(start, i + 1).trim()
                if (s.isNotEmpty() && s != "." && s != "!" && s != "?") out.add(s)
                start = i + 1
            }
        }
        if (start < text.length) {
            val tail = text.substring(start).trim()
            if (tail.isNotEmpty()) out.add(tail)
        }
        return out
    }

    private fun estimateTokens(s: String): Int =
        (s.length / CHARS_PER_TOKEN).coerceAtLeast(1)

    companion object {
        const val DEFAULT_MAX_TOKENS = 250
        private const val CHARS_PER_TOKEN = 4
    }
}
