package org.lingolocal.project.domain.model

/**
 * Mazzo di flashcard. Immutabile: ogni modifica produce una nuova istanza.
 *
 * @property id null per i deck non ancora persistiti
 * @property language codice ISO (es. "en", "it", "es"). Determina il modello LLM da usare.
 * @property createdAt epoch millis di creazione
 */
data class Deck(
    val id: Long?,
    val name: String,
    val language: String,
    val createdAt: Long
)
