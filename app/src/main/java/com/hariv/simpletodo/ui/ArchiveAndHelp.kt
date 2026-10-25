package com.hariv.simpletodo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hariv.simpletodo.App
import com.hariv.simpletodo.data.Todo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(back: () -> Unit) {
    val context = LocalContext.current
    val app = remember { App.get(context) }
    val archived by app.repo.archivedTodos.collectAsState(initial = null)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Archive") }, navigationIcon = { BackButton(back) }) },
    ) { padding ->
        val list = archived
        when {
            list == null -> Unit
            list.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Nothing archived yet.\n\nTicked items move here 7 days after you tick them, " +
                        "or right away with \"Clear completed\".",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                item {
                    Text(
                        "${list.size} archived, newest first. View only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(list, key = { it.id }) { todo ->
                    ListItem(
                        headlineContent = { Text(todo.text) },
                        supportingContent = { Text(archiveLine(todo)) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

/** "pw · Done 18 Oct 09:15 · Archived 25 Oct 10:42 · Auto" */
private fun archiveLine(t: Todo): String = buildList {
    add(t.category)
    t.completedAt?.let { add("Done ${Fmt.dateTime(it)}") }
    t.archivedAt?.let { add("Archived ${Fmt.dateTime(it)}") }
    add(if (t.archivedBy == "auto") "Auto" else "Cleared")
}.joinToString(" · ")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(back: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("How it works") }, navigationIcon = { BackButton(back) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HelpItem("Add", "Type a task, pick pw, my or bl, and tap Add.")
            HelpItem("Filter", "Tap All, pw, my or bl above the list to show one category. Tap a todo's category label to move it.")
            HelpItem("Done", "Tick the box. Tick again to undo. × deletes a todo for good.")
            HelpItem("Archive", "Ticked items move to the Archive 7 days after you tick them, or right away with \"Clear completed\". Open it with Archive at the top.")
            HelpItem("Recurring", "Tap Recurring at the top to set up repeating todos (bills, birthdays, quarterly tasks). Each one appears on your list the day before it's due, even if you don't open the app.")
            HelpItem("Widget", "Long-press an empty home-screen page → Widgets → Simple Todo, then stretch it to fill the page. Or use ⋮ → Add widget to home screen. Tick, filter, add and clear right on the widget; tap a todo to open the app.")
            HelpItem("Backup", "Every change is saved to Documents/TodoBackup/todo-backup.json (or the folder you chose). The line at the bottom of the main screen shows the last backup.")
            HelpItem("Restore", "⋮ → Restore from backup, then pick todo-backup.json. On a new phone, copy the TodoBackup folder over first.")
            HelpItem("Samsung battery", "⋮ → Battery: allow background, so recurring todos appear on time.")
            HelpItem("Updates", "⋮ → Check for update installs the newest version from GitHub. Your todos stay.")
        }
    }
}

@Composable
private fun HelpItem(title: String, body: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}
