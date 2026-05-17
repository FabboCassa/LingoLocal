package org.lingolocal.project

import androidx.compose.ui.window.ComposeUIViewController
import org.lingolocal.project.di.iosModule

fun MainViewController() = ComposeUIViewController { App(platformModules = listOf(iosModule)) }