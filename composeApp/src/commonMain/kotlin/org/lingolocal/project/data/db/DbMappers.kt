package org.lingolocal.project.data.db

import org.lingolocal.project.domain.model.Deck
import org.lingolocal.project.domain.model.Flashcard
import org.lingolocal.project.domain.model.SrsState
import org.lingolocal.project.domain.model.StudySession

/**
 * Mapper tra le entity generate da SQLDelight e i modelli di dominio.
 *
 * Mantenere il mapping in un file dedicato evita di sporcare i repository
 * con conversioni ripetute, e isola il domain dalle classi generate.
 */

internal fun Decks.toDomain(): Deck = Deck(
    id = id,
    name = name,
    language = language,
    createdAt = created_at
)

internal fun Flashcards.toDomain(): Flashcard = Flashcard(
    id = id,
    deckId = deck_id,
    front = front,
    back = back,
    createdAt = created_at,
    srs = SrsState(
        easeFactor = ease_factor,
        intervalDays = interval_days.toInt(),
        repetitions = repetitions.toInt(),
        nextReviewAt = next_review_at
    )
)

internal fun Study_sessions.toDomain(): StudySession = StudySession(
    id = id,
    deckId = deck_id,
    startedAt = started_at,
    endedAt = ended_at,
    cardsReviewed = cards_reviewed.toInt(),
    cardsCorrect = cards_correct.toInt()
)
