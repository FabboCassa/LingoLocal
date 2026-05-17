package org.lingolocal.project.data.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(LingoDatabase.Schema, context, DB_NAME)

    private companion object {
        const val DB_NAME = "lingolocal.db"
    }
}
