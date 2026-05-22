package org.lingolocal.project.domain.model

enum class AppLanguage(val code: String, val displayName: String) {
    ITALIANO("it", "Italiano"),
    ENGLISH("en", "English"),
    ESPANOL("es", "Español"),
    FRANCAIS("fr", "Français"),
    DEUTSCH("de", "Deutsch");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ITALIANO
    }
}
