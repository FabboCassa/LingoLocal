package org.lingolocal.project.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import org.lingolocal.project.data.db.LingoDatabase
import org.lingolocal.project.data.db.toDomain
import org.lingolocal.project.domain.model.Deck
import org.lingolocal.project.domain.repository.DeckRepository

/**
 * Implementazione di [DeckRepository] su SQLDelight.
 * Tutte le operazioni di I/O sono effettuate su [Dispatchers.IO].
 * Il [clock] è iniettato per testabilità (fake clock nei test).
 */
class DeckRepositoryImpl(
    private val db: LingoDatabase,
    private val clock: Clock
) : DeckRepository {

    private val queries get() = db.schemaQueries

    private fun nowMillis(): Long = clock.now().toEpochMilliseconds()

    override fun observeAllDecks(): Flow<List<Deck>> =
        queries.selectAllDecks()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun createDeck(name: String, language: String): Long = withContext(Dispatchers.IO) {
        db.transactionWithResult {
            queries.insertDeck(name = name, language = language, created_at = nowMillis())
            queries.lastInsertedId().executeAsOne()
        }
    }

    override suspend fun getDeckById(id: Long): Deck? = withContext(Dispatchers.IO) {
        queries.selectDeckById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun deleteDeck(id: Long) {
        withContext(Dispatchers.IO) { queries.deleteDeck(id) }
    }
}
