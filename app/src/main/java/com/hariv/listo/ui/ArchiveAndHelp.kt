package com.hariv.listo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hariv.listo.App
import com.hariv.listo.data.Todo
import com.hariv.listo.ui.theme.ListoIcons
import com.hariv.listo.ui.theme.categoryColor
import com.hariv.listo.ui.theme.categoryTone
import com.hariv.listo.ui.theme.neutralTone

private const val ARCHIVE_ALL = "all"

@Composable
fun ArchiveScreen(go: (Screen) -> Unit) {
    val context = LocalContext.current
    val app = remember { App.get(context) }
    val archived by app.repo.archivedTodos.collectAsState(initial = null)
    val categories by app.repo.categories.list.collectAsState()
    var filterChoice by rememberSaveable { mutableStateOf(ARCHIVE_ALL) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { ListoNavBar(Screen.Archive, go) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PageHeader("Archive", "Done tasks move here 7 days after you tick them. View only.")
            val list = archived
            when {
                list == null -> Unit
                list.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(ListoIcons.Archive, "Nothing archived yet", "Tick a task and tap Clear completed, or wait 7 days.")
                }
                else -> {
                    // Current categories in their order, then any old labels that only archived tasks still carry.
                    val oldLabels = list.map { it.category }.distinct().filter { it !in categories }.sorted()
                    val options = listOf(ARCHIVE_ALL) + categories + oldLabels
                    val filter = if (filterChoice in options) filterChoice else ARCHIVE_ALL
                    Spacer(Modifier.height(16.dp))
                    ChoiceRow(
                        options = options,
                        selected = filter,
                        onSelect = { filterChoice = it },
                        label = { if (it == ARCHIVE_ALL) "All" else it },
                        count = { option -> list.count { option == ARCHIVE_ALL || it.category == option } },
                        dotColor = { option -> if (option == ARCHIVE_ALL) null else categoryColor(categories, option) },
                        tone = { option -> if (option == ARCHIVE_ALL) neutralTone() else categoryTone(categories, option) },
                        contentPadding = PaddingValues(horizontal = 20.dp),
                    )
                    val shown = if (filter == ARCHIVE_ALL) list else list.filter { it.category == filter }
                    // Already newest first; group by the month each task was archived.
                    val byMonth = shown.groupBy { Fmt.monthYear(it.archivedAt ?: 0L) }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    ) {
                        if (shown.isEmpty()) {
                            item(key = "none") {
                                Box(Modifier.fillParentMaxHeight(0.6f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    EmptyState(ListoIcons.Archive, "Nothing archived in $filter", "Archived tasks in this category appear here.")
                                }
                            }
                        }
                        byMonth.forEach { (month, items) ->
                            item(key = "month-$month") { SectionLabel("$month (${items.size})") }
                            itemsIndexed(items, key = { _, t -> t.id }) { index, todo ->
                                GroupRow(index, items.size, Modifier.animateItem(), dividerInset = 50.dp) { ArchiveRow(todo, categories) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveRow(todo: Todo, categories: List<String>) {
    val tone = categoryTone(categories, todo.category)
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(top = 1.dp).size(20.dp).clip(CircleShape).background(tone.strong),
        ) {
            Icon(ListoIcons.Check, contentDescription = "Done", tint = tone.onFill, modifier = Modifier.size(11.dp))
        }
        Spacer(Modifier.width(14.dp))
        ArchiveDetails(todo, categories)
    }
}

@Composable
private fun ArchiveDetails(todo: Todo, categories: List<String>) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Text(todo.text, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryTag(categories, todo.category)
            Spacer(Modifier.width(12.dp))
            todo.archivedAt?.let {
                val how = if (todo.archivedBy == "auto") "Archived automatically" else "Cleared"
                Text("$how ${Fmt.dateTime(it)}", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            }
        }
        todo.completedAt?.let {
            Text("Done ${Fmt.dateTime(it)}", style = MaterialTheme.typography.labelMedium, color = scheme.outline, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun HelpScreen(back: () -> Unit) {
    val topics: List<Triple<ImageVector, String, String>> = listOf(
        Triple(ListoIcons.Tasks, "Add and tick tasks", "Type in the bar at the bottom, pick a category, and tap the arrow. Tick the circle when it's done; tick again to undo. The × deletes a task for good."),
        Triple(ListoIcons.Settings, "Categories", "Tap a category under a task to move it. Add, rename, reorder or delete categories in Settings."),
        Triple(ListoIcons.Archive, "Archive", "Ticked tasks move to the Archive 7 days after you tick them, or straight away with Clear completed."),
        Triple(ListoIcons.Repeat, "Recurring tasks", "Bills, birthdays, quarterly reviews: set them up under Recurring. Each one appears on your list the day before it's due, even if you don't open the app."),
        Triple(ListoIcons.Widget, "Home-screen widget", "Long-press an empty home-screen page, tap Widgets, then Listo, and stretch it to fill the page. Tick, filter, add and clear right on the widget."),
        Triple(ListoIcons.Save, "Backup", "Every change is saved to Documents/ListoBackup/listo-backup.json, or the folder you chose. Settings shows the last backup."),
        Triple(ListoIcons.Restore, "Restore", "Settings, then Restore from backup, and pick listo-backup.json. On a new phone, copy the ListoBackup folder across first."),
        Triple(ListoIcons.Battery, "Samsung battery", "Settings, then Run in background, so recurring tasks appear on time."),
        Triple(ListoIcons.Update, "Updates", "Settings, then Check for update, installs the newest version from GitHub. Your tasks stay."),
    )
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PageHeader("How Listo works", onBack = back)
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp)) {
                itemsIndexed(topics) { index, (icon, title, body) ->
                    GroupRow(index, topics.size, dividerInset = 64.dp) {
                        InfoRow(icon = icon, title = title, value = body)
                    }
                }
            }
        }
    }
}
