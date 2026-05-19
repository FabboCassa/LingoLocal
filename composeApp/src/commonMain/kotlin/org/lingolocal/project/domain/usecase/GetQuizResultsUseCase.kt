package org.lingolocal.project.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.lingolocal.project.domain.model.QuizResult
import org.lingolocal.project.domain.repository.QuizRepository

/**
 * Caso d'uso per recuperare lo storico di tutti i risultati dei quiz svolti.
 */
class GetQuizResultsUseCase(
    private val quizRepository: QuizRepository
) {
    operator fun invoke(): Flow<List<QuizResult>> {
        return quizRepository.getAllResults()
    }
}
