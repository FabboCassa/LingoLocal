package org.lingolocal.project.domain.repository

import kotlinx.coroutines.flow.Flow
import org.lingolocal.project.domain.model.QuizResult

/**
 * Repository per il salvataggio e recupero dello storico dei risultati dei quiz.
 */
interface QuizRepository {
    /**
     * Salva un nuovo punteggio quiz nel database locale.
     * Restituisce l'id del record inserito.
     */
    suspend fun saveResult(title: String, questionsCount: Int, correctAnswers: Int): Long

    /**
     * Recupera tutti i risultati memorizzati ordinati per data decrescente.
     */
    fun getAllResults(): Flow<List<QuizResult>>
}
