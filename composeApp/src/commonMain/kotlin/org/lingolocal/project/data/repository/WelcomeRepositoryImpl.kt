package org.lingolocal.project.data.repository

import org.lingolocal.project.domain.repository.WelcomeRepository

/**
 * Implementazione concreta del WelcomeRepository.
 * In futuro questa classe potrà accedere a data source reali (DB, rete, ecc.).
 * Per ora restituisce un valore mock per validare il flusso MVVM.
 */
class WelcomeRepositoryImpl : WelcomeRepository {
    override fun getWelcomeMessage(): String {
        return "LingoLocal - AI Language Learning"
    }
}
