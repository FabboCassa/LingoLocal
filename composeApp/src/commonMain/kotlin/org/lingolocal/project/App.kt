package org.lingolocal.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import org.koin.compose.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinConfiguration
import org.lingolocal.project.di.appModule
import org.lingolocal.project.presentation.home.HomeScreen

/**
 * Entry point Compose dell'applicazione.
 * Responsabilità:
 * - Inizializzare Koin (DI) con i moduli comuni + quelli platform-specific
 * - Applicare il tema Material
 * - Configurare il Navigator Voyager con la schermata iniziale
 *
 * @param platformModules Moduli Koin specifici per piattaforma (es. androidModule)
 */
@Composable
fun App(platformModules: List<Module> = emptyList()) {
    KoinApplication(koinConfiguration {
        modules(appModule)
        modules(platformModules)
    }) {
        MaterialTheme {
            Navigator(HomeScreen()) { navigator ->
                SlideTransition(navigator)
            }
        }
    }
}