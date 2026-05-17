package org.lingolocal.project.data.repository

import kotlin.math.sqrt

/**
 * Similarità coseno tra due vettori densi.
 *
 * Risultato ∈ [-1, 1]:
 *   +1 vettori paralleli (massima rilevanza)
 *    0 vettori ortogonali
 *   -1 vettori antiparalleli
 *
 * Restituisce 0f se uno dei due vettori ha norma nulla
 * (caso degenere: nessuna informazione direzionale).
 *
 * @throws IllegalArgumentException se le dimensioni differiscono.
 */
internal object CosineSimilarity {

    fun between(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) {
            "Dimensioni incompatibili: ${a.size} vs ${b.size}"
        }
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            val ai = a[i].toDouble()
            val bi = b[i].toDouble()
            dot += ai * bi
            normA += ai * ai
            normB += bi * bi
        }
        if (normA == 0.0 || normB == 0.0) return 0f
        return (dot / (sqrt(normA) * sqrt(normB))).toFloat()
    }
}
