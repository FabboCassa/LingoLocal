package org.lingolocal.project.domain.usecase

import org.lingolocal.project.domain.repository.LlamaRepository

/**
 * Richiede l'embedding di un testo al modello GGUF on-device tramite
 * [LlamaRepository]. Il vettore restituito è già L2-normalizzato lato JNI:
 * questo permette al consumer (RAG) di calcolare cosine similarity senza
 * doversi preoccupare di rinormalizzazioni.
 *
 * Restituisce null se non c'è un modello caricato o l'inferenza fallisce —
 * il caller (es. pipeline di indicizzazione) deve decidere se ritentare o
 * scartare il chunk.
 */
class GenerateEmbeddingUseCase(
    private val repository: LlamaRepository
) {
    suspend operator fun invoke(text: String): FloatArray? = repository.embed(text)
}
