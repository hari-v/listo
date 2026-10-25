package com.hariv.listo.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.hariv.listo.App
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Main : Screen
    data object Recurring : Screen
    data class EditRule(val ruleId: String?) : Screen
    data object Archive : Screen
    data object Help : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme { AppRoot() }
        }
    }

    override fun onResume() {
        super.onResume()
        // Catch up on recurring todos and auto-archive whenever the app is opened.
        val app = App.get(this)
        app.appScope.launch { app.repo.runMaintenance() }
    }
}

@Composable
private fun AppRoot() {
    var screen by remember { mutableStateOf<Screen>(Screen.Main) }
    val go: (Screen) -> Unit = { screen = it }

    val parent: Screen? = when (screen) {
        Screen.Main -> null
        is Screen.EditRule -> Screen.Recurring
        else -> Screen.Main
    }
    BackHandler(enabled = parent != null) { parent?.let { screen = it } }
    val back: () -> Unit = { parent?.let { screen = it } }

    when (val s = screen) {
        Screen.Main -> MainScreen(go)
        Screen.Recurring -> RecurringScreen(go, back)
        is Screen.EditRule -> RuleEditorScreen(s.ruleId, back)
        Screen.Archive -> ArchiveScreen(back)
        Screen.Help -> HelpScreen(back)
    }
}
