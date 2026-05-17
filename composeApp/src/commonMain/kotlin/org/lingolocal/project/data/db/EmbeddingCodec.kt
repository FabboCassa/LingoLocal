package org.lingolocal.project.data.db

/**
 * Serializzazione FloatArray <-> ByteArray per persistere embedding come BLOB SQLite.
 *
 * Formato: little-endian, 4 byte per float (IEEE 754).
 * Implementazione pura Kotlin (no java.nio) per essere KMP-compatibile.
 */
internal object EmbeddingCodec {

    fun encode(vector: FloatArray): ByteArray {
        val out = ByteArray(vector.size * Float.SIZE_BYTES)
        var offset = 0
        for (v in vector) {
            val bits = v.toRawBits()
            out[offset]     = (bits and 0xFF).toByte()
            out[offset + 1] = ((bits ushr 8) and 0xFF).toByte()
            out[offset + 2] = ((bits ushr 16) and 0xFF).toByte()
            out[offset + 3] = ((bits ushr 24) and 0xFF).toByte()
            offset += Float.SIZE_BYTES
        }
        return out
    }

    fun decode(bytes: ByteArray): FloatArray {
        require(bytes.size % Float.SIZE_BYTES == 0) {
            "Embedding BLOB corrotto: lunghezza ${bytes.size} non multipla di 4"
        }
        val out = FloatArray(bytes.size / Float.SIZE_BYTES)
        var offset = 0
        for (i in out.indices) {
            val bits =
                (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 3].toInt() and 0xFF) shl 24)
            out[i] = Float.fromBits(bits)
            offset += Float.SIZE_BYTES
        }
        return out
    }
}
