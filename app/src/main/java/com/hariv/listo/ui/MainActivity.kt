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
import com.hariv.listo.ui.theme.ListoTheme
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Tasks : Screen
    data object Recurring : Screen
    data object Archive : Screen
    data class EditRule(val ruleId: String?) : Screen
    data object Settings : Screen
    data object Help : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ListoTheme { AppRoot() }
        }
    }

    override fun onResume() {
        super.onResume()
        // Catch up on recurring tasks and auto-archive whenever the app is opened.
        val app = App.get(this)
        app.appScope.launch { app.repo.runMaintenance() }
    }
}

@Composable
private fun AppRoot() {
    // Tabs reset the stack; other screens open on top of the current one.
    var stack by remember { mutableStateOf(listOf<Screen>(Screen.Tasks)) }
    val go: (Screen) -> Unit = { screen ->
        stack = when (screen) {
            Screen.Tasks -> listOf(Screen.Tasks)
            Screen.Recurring, Screen.Archive -> listOf(Screen.Tasks, screen)
            else -> stack + screen
        }
    }
    val back: () -> Unit = { if (stack.size > 1) stack = stack.dropLast(1) }
    BackHandler(enabled = stack.size > 1) { back() }

    when (val screen = stack.last()) {
        Screen.Tasks -> TasksScreen(go)
        Screen.Recurring -> RecurringScreen(go)
        Screen.Archive -> ArchiveScreen(go)
        is Screen.EditRule -> RuleEditorScreen(screen.ruleId, back)
        Screen.Settings -> SettingsScreen(go, back)
        Screen.Help -> HelpScreen(back)
    }
}
