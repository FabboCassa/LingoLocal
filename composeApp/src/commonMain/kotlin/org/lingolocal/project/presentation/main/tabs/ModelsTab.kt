package org.lingolocal.project.presentation.main.tabs

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import lingolocal.composeapp.generated.resources.Res
import lingolocal.composeapp.generated.resources.tab_models
import org.jetbrains.compose.resources.stringResource
import cafe.adriel.voyager.transitions.SlideTransition
import org.lingolocal.project.presentation.modelmanager.ModelManagerScreen
import org.lingolocal.project.presentation.theme.LingoIcons

object ModelsTab : Tab {

    var navigator: Navigator? = null
        private set

    override val options: TabOptions
        @Composable
        get() {
            val title = stringResource(Res.string.tab_models)
            val icon = rememberVectorPainter(LingoIcons.Storage)
            return TabOptions(index = 1u, title = title, icon = icon)
        }

    @Composable
    override fun Content() {
        Navigator(ModelManagerScreen()) { nav ->
            navigator = nav
            SlideTransition(nav)
        }
    }
}
