package org.lingolocal.project.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import org.lingolocal.project.presentation.main.tabs.HomeTab
import org.lingolocal.project.presentation.main.tabs.ModelsTab
import org.lingolocal.project.presentation.main.tabs.SettingsTab

/**
 * Entry screen dell'app: ospita la BottomBar con i tab principali.
 * Ogni tab incapsula il proprio Navigator interno (push/pop indipendente).
 */
class MainScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        TabNavigator(HomeTab) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(LocalTabNavigator.current.current.options.title) },
                        colors = TopAppBarDefaults.topAppBarColors()
                    )
                },
                bottomBar = { LingoBottomBar() }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    CurrentTab()
                }
            }
        }
    }
}

@Composable
private fun LingoBottomBar() {
    val tabNavigator = LocalTabNavigator.current
    NavigationBar {
        BottomBarItem(tabNavigator.current, HomeTab) { tabNavigator.current = it }
        BottomBarItem(tabNavigator.current, ModelsTab) { tabNavigator.current = it }
        BottomBarItem(tabNavigator.current, SettingsTab) { tabNavigator.current = it }
    }
}

@Composable
private fun RowScope.BottomBarItem(
    current: Tab,
    tab: Tab,
    onSelected: (Tab) -> Unit
) {
    val options = tab.options
    NavigationBarItem(
        selected = current.key == tab.key,
        onClick = { onSelected(tab) },
        icon = {
            options.icon?.let { Icon(painter = it, contentDescription = options.title) }
        },
        label = { Text(options.title) }
    )
}
