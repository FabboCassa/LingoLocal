package org.lingolocal.project.domain.model

/**
 * Sessione di studio (per analytics e progressione).
 *
 * @property deckId null se la sessione non è legata a un singolo mazzo
 *                  (es. review globale di tutte le card "due")
 * @property endedAt null finché la sessione è in corso
 */
data class StudySession(
    val id: Long?,
    val deckId: Long?,
    val startedAt: Long,
    val endedAt: Long?,
    val cardsReviewed: Int,
    val cardsCorrect: Int
)
