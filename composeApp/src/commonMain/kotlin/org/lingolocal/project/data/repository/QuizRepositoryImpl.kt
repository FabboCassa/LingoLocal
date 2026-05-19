package org.lingolocal.project.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import org.lingolocal.project.data.db.LingoDatabase
import org.lingolocal.project.domain.model.QuizResult
import org.lingolocal.project.domain.repository.QuizRepository

/**
 * Implementazione di [QuizRepository] tramite SQLDelight per la persistenza su SQLite.
 */
class QuizRepositoryImpl(
    private val db: LingoDatabase,
    private val clock: Clock
) : QuizRepository {

    private val queries = db.schemaQueries

    override suspend fun saveResult(
        title: String,
        questionsCount: Int,
        correctAnswers: Int
    ): Long = withContext(Dispatchers.Default) {
        db.transactionWithResult {
            queries.insertQuizResult(
                title = title,
                questions_count = questionsCount.toLong(),
                correct_answers = correctAnswers.toLong(),
                created_at = clock.now().toEpochMilliseconds()
            )
            queries.lastInsertedId().executeAsOne()
        }
    }

    override fun getAllResults(): Flow<List<QuizResult>> {
        return queries.selectAllQuizResults()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map {
                    QuizResult(
                        id = it.id,
                        title = it.title,
                        questionsCount = it.questions_count.toInt(),
                        correctAnswers = it.correct_answers.toInt(),
                        createdAt = it.created_at
                    )
                }
            }
    }
}
