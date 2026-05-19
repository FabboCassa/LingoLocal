package org.lingolocal.project.domain.usecase

import org.lingolocal.project.domain.model.ScoredChunk
import org.lingolocal.project.domain.repository.VectorRepository

/**
 * Motore RAG (Retrieval-Augmented Generation):
 * dato un testo di query (es. argomento o domanda dell'utente), genera il suo embedding,
 * interroga il [VectorRepository] per trovare i top-K chunk di testo più simili semanticamente,
 * e li concatena in una singola stringa di contesto da inserire nel prompt dell'LLM.
 */
class RagQueryUseCase(
    private val generateEmbedding: GenerateEmbeddingUseCase,
    private val vectorRepository: VectorRepository
) {

    data class Result(
        val context: String,
        val scoredChunks: List<ScoredChunk>
    )

    suspend operator fun invoke(
        query: String,
        topK: Int = 3
    ): Result {
        if (query.isBlank()) {
            return Result("", emptyList())
        }

        val queryEmbedding = generateEmbedding(query)
        if (queryEmbedding == null) {
            return Result("", emptyList())
        }

        val similarChunks = vectorRepository.searchSimilar(queryEmbedding, topK)
        val context = similarChunks.joinToString("\n\n") { it.chunk.content }

        return Result(context, similarChunks)
    }
}
