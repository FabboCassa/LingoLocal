package org.lingolocal.project.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import org.lingolocal.project.data.db.EmbeddingCodec
import org.lingolocal.project.data.db.LingoDatabase
import org.lingolocal.project.data.db.Text_chunks
import org.lingolocal.project.domain.model.ScoredChunk
import org.lingolocal.project.domain.model.TextChunk
import org.lingolocal.project.domain.repository.VectorRepository

/**
 * Implementazione di [VectorRepository] su SQLite (SQLDelight) + cosine similarity in-memory.
 *
 * Strategia:
 *   - Persistenza: gli embedding sono serializzati come BLOB little-endian (vedi [EmbeddingCodec]).
 *   - Ricerca:    tutti i chunk vengono caricati in memoria e scoredecon [CosineSimilarity].
 *                 Per un utente personale (≤ qualche migliaio di chunk) il costo è trascurabile;
 *                 evita una dipendenza nativa (sqlite-vec) non disponibile su tutte le piattaforme KMP.
 */
class VectorRepositoryImpl(
    private val db: LingoDatabase,
    private val clock: Clock
) : VectorRepository {

    private val queries get() = db.schemaQueries

    private fun nowMillis(): Long = clock.now().toEpochMilliseconds()

    override suspend fun insertChunk(
        sourceId: String,
        content: String,
        embedding: FloatArray
    ): Long = withContext(Dispatchers.Default) {
        require(embedding.isNotEmpty()) { "embedding non può essere vuoto" }
        db.transactionWithResult {
            queries.insertTextChunk(
                source_id = sourceId,
                content = content,
                embedding = EmbeddingCodec.encode(embedding),
                created_at = nowMillis()
            )
            queries.lastInsertedId().executeAsOne()
        }
    }

    override suspend fun getAllChunks(): List<TextChunk> = withContext(Dispatchers.Default) {
        queries.selectAllTextChunks().executeAsList().map { it.toDomain() }
    }

    override suspend fun deleteBySourceId(sourceId: String) {
        withContext(Dispatchers.Default) { queries.deleteTextChunksBySource(sourceId) }
    }

    override suspend fun searchSimilar(
        queryEmbedding: FloatArray,
        topK: Int
    ): List<ScoredChunk> = withContext(Dispatchers.Default) {
        require(topK > 0) { "topK deve essere > 0" }
        require(queryEmbedding.isNotEmpty()) { "queryEmbedding non può essere vuoto" }

        val all = queries.selectAllTextChunks().executeAsList()
        if (all.isEmpty()) return@withContext emptyList()

        all.asSequence()
            .map { it.toDomain() }
            .filter { it.embedding.size == queryEmbedding.size }
            .map { chunk -> ScoredChunk(chunk, CosineSimilarity.between(queryEmbedding, chunk.embedding)) }
            .sortedByDescending { it.score }
            .take(topK)
            .toList()
    }

    private fun Text_chunks.toDomain(): TextChunk = TextChunk(
        id = id,
        sourceId = source_id,
        content = content,
        embedding = EmbeddingCodec.decode(embedding),
        createdAt = created_at
    )
}
