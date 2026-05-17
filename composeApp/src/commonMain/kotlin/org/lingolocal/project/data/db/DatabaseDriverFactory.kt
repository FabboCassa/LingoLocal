package org.lingolocal.project.data.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Factory expect/actual per la creazione del driver SQLDelight specifico per piattaforma.
 * - Android: AndroidSqliteDriver con Context
 * - iOS: NativeSqliteDriver
 *
 * Il driver è creato una sola volta (singleton via Koin).
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

/**
 * Helper centralizzato per costruire l'istanza [LingoDatabase].
 *
 * Abilita esplicitamente i foreign key constraint (SQLite li ha disabilitati
 * di default): senza questo, `ON DELETE CASCADE` non viene applicato e cancellare
 * un deck NON rimuoverebbe le sue flashcard.
 */
fun createLingoDatabase(factory: DatabaseDriverFactory): LingoDatabase {
    val driver = factory.createDriver()
    driver.execute(identifier = null, sql = "PRAGMA foreign_keys = ON;", parameters = 0)
    return LingoDatabase(driver)
}
