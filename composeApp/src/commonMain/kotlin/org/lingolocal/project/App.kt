package org.lingolocal.project

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.module.Module
import org.koin.dsl.koinConfiguration
import org.lingolocal.project.di.appModule
import org.lingolocal.project.domain.usecase.GetThemeModeUseCase
import org.lingolocal.project.presentation.main.MainScreen
import org.lingolocal.project.presentation.theme.AppTheme

/**
 * Entry point Compose dell'applicazione.
 * Responsabilità:
 * - Inizializzare Koin (DI) con i moduli comuni + quelli platform-specific
 * - Applicare il tema personalizzato (osservando la preferenza utente)
 * - Configurare il Navigator Voyager con la schermata iniziale (MainScreen + BottomBar)
 *
 * @param platformModules Moduli Koin specifici per piattaforma (es. androidModule)
 */
@Composable
fun App(platformModules: List<Module> = emptyList()) {
    KoinApplication(koinConfiguration {
        modules(appModule)
        modules(platformModules)
    }) {
        ThemedRoot()
    }
}

@Composable
private fun ThemedRoot() {
    val getThemeMode: GetThemeModeUseCase = koinInject()
    val themeMode by getThemeMode().collectAsState()

    AppTheme(themeMode = themeMode) {
        Navigator(MainScreen()) { navigator ->
            SlideTransition(navigator)
        }
    }
}
