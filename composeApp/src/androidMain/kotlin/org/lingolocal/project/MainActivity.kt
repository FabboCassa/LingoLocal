package org.lingolocal.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.koin.dsl.module
import org.lingolocal.project.di.androidModule

/**
 * Activity principale Android.
 * Fornisce il Context Android al sistema DI tramite il modulo platform-specific.
 */
class MainActivity : ComponentActivity() {

    private val contextModule = module {
        single { this@MainActivity.applicationContext }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            App(platformModules = listOf(contextModule, androidModule))
        }
    }
}