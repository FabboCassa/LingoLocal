package org.lingolocal.project.domain.model

/**
 * Preferenza utente per il tema dell'app.
 * SYSTEM = segue il tema di sistema (default).
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
