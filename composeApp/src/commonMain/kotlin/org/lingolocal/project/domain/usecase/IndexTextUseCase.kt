package org.lingolocal.project.domain.usecase

import org.lingolocal.project.domain.repository.VectorRepository

/**
 * Pipeline di ingestione testo per il RAG (Task 2.3):
 *   testo → chunking (frase-aware) → embedding on-device → persistenza vector store.
 *
 * Restituisce un [Result] che riassume:
 *   - `chunksCreated`: quanti chunk sono stati prodotti dal chunker
 *   - `chunksIndexed`: quanti sono stati effettivamente salvati con embedding valido
 *   - `failedChunks`: quanti chunk hanno fallito la generazione dell'embedding
 *     (il modello potrebbe non essere caricato o aver restituito null)
 *
 * Errori non bloccanti: se un singolo chunk fallisce, gli altri vengono comunque
 * processati. È responsabilità del caller decidere se ritentare i falliti.
 */
class IndexTextUseCase(
    private val chunker: TextChunkingUseCase,
    private val generateEmbedding: GenerateEmbeddingUseCase,
    private val vectorRepository: VectorRepository
) {

    data class Result(
        val chunksCreated: Int,
        val chunksIndexed: Int,
        val failedChunks: Int
    )

    suspend operator fun invoke(
        sourceId: String,
        text: String,
        maxTokensPerChunk: Int = TextChunkingUseCase.DEFAULT_MAX_TOKENS
    ): Result {
        require(sourceId.isNotBlank()) { "sourceId non può essere blank" }
        val chunks = chunker(text, maxTokensPerChunk)
        if (chunks.isEmpty()) return Result(0, 0, 0)

        var indexed = 0
        var failed = 0
        for (chunk in chunks) {
            val embedding = generateEmbedding(chunk)
            if (embedding == null) {
                failed++
                continue
            }
            vectorRepository.insertChunk(sourceId, chunk, embedding)
            indexed++
        }
        return Result(
            chunksCreated = chunks.size,
            chunksIndexed = indexed,
            failedChunks = failed
        )
    }
}
