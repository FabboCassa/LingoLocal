package org.lingolocal.project.domain.model

/**
 * Chunk di testo indicizzato per Retrieval-Augmented Generation.
 *
 * @property id          PK locale (assegnata da SQLite). 0 prima della persistenza.
 * @property sourceId    Riferimento logico alla sorgente d'origine (documento, nota...).
 *                       Usato per cancellazioni bulk quando l'utente rimuove il materiale.
 * @property content     Testo originale del chunk (tipicamente 200–300 token).
 * @property embedding   Vettore denso di feature prodotto dall'LLM on-device.
 *                       Conservato come [FloatArray] in memoria, serializzato come BLOB su SQLite.
 * @property createdAt   Epoch millis di creazione (UTC).
 */
data class TextChunk(
    val id: Long,
    val sourceId: String,
    val content: String,
    val embedding: FloatArray,
    val createdAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextChunk) return false
        return id == other.id &&
            sourceId == other.sourceId &&
            content == other.content &&
            createdAt == other.createdAt &&
            embedding.contentEquals(other.embedding)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + sourceId.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}

/**
 * Risultato di una ricerca semantica: chunk + score di similarità coseno (∈ [-1, 1]).
 * I valori più alti indicano maggior rilevanza rispetto alla query.
 */
data class ScoredChunk(
    val chunk: TextChunk,
    val score: Float
)
