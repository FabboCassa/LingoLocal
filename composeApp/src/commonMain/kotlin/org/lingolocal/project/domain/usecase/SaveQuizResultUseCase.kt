package org.lingolocal.project.domain.usecase

import org.lingolocal.project.domain.repository.QuizRepository

/**
 * Caso d'uso per salvare il risultato di un quiz nel database locale.
 */
class SaveQuizResultUseCase(
    private val quizRepository: QuizRepository
) {
    suspend operator fun invoke(title: String, questionsCount: Int, correctAnswers: Int): Long {
        return quizRepository.saveResult(title, questionsCount, correctAnswers)
    }
}
