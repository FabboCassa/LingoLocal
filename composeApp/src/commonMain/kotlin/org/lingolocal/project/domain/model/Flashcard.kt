package org.lingolocal.project.domain.model

/**
 * Singola flashcard con stato SRS (SM-2 semplificato).
 *
 * Lo stato SRS è incapsulato in [SrsState] per separare i dati di contenuto
 * (front/back) dalla logica di scheduling, e per permettere update atomici
 * dell'unica responsabilità "scheduling".
 */
data class Flashcard(
    val id: Long?,
    val deckId: Long,
    val front: String,
    val back: String,
    val createdAt: Long,
    val srs: SrsState
)

/**
 * Stato SRS di una flashcard.
 *
 * @property easeFactor fattore di facilità SM-2 (1.3..2.5+, default 2.5)
 * @property intervalDays giorni fino alla prossima review
 * @property repetitions numero di review consecutive corrette
 * @property nextReviewAt epoch millis della prossima review schedulata
 */
data class SrsState(
    val easeFactor: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val nextReviewAt: Long
) {
    companion object {
        const val DEFAULT_EASE_FACTOR = 2.5
        const val MIN_EASE_FACTOR = 1.3

        /**
         * Stato iniziale per una nuova flashcard: review schedulata immediatamente.
         */
        fun initial(now: Long): SrsState = SrsState(
            easeFactor = DEFAULT_EASE_FACTOR,
            intervalDays = 0,
            repetitions = 0,
            nextReviewAt = now
        )
    }
}
