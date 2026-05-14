package org.lingolocal.project.domain.repository

/**
 * Interfaccia del repository per i dati di benvenuto.
 * Definita nel layer Domain per garantire l'inversione delle dipendenze.
 */
interface WelcomeRepository {
    fun getWelcomeMessage(): String
}
