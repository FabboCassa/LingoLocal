package org.lingolocal.project.presentation.main.tabs

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import lingolocal.composeapp.generated.resources.Res
import lingolocal.composeapp.generated.resources.tab_home
import org.jetbrains.compose.resources.stringResource
import cafe.adriel.voyager.transitions.SlideTransition
import org.lingolocal.project.presentation.home.HomeScreen
import org.lingolocal.project.presentation.theme.LingoIcons

object HomeTab : Tab {

    var navigator: Navigator? = null
        private set

    override val options: TabOptions
        @Composable
        get() {
            val title = stringResource(Res.string.tab_home)
            val icon = rememberVectorPainter(LingoIcons.Home)
            return TabOptions(index = 0u, title = title, icon = icon)
        }

    @Composable
    override fun Content() {
        Navigator(HomeScreen()) { nav ->
            navigator = nav
            SlideTransition(nav)
        }
    }
}
