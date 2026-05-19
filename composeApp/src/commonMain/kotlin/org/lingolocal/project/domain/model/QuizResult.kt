package org.lingolocal.project.domain.model

/**
 * Storico del risultato di un quiz svolto dall'utente.
 */
data class QuizResult(
    val id: Long,
    val title: String,
    val questionsCount: Int,
    val correctAnswers: Int,
    val createdAt: Long
)
