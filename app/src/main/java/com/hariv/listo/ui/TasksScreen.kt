package com.hariv.listo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.hariv.listo.App
import com.hariv.listo.data.Repository
import com.hariv.listo.data.Todo
import com.hariv.listo.ui.theme.ListoIcons
import com.hariv.listo.ui.theme.categoryColor
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val ALL = "all"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasksScreen(go: (Screen) -> Unit) {
    val context = LocalContext.current
    val app = remember { App.get(context) }
    val repo = app.repo
    val writeScope = app.appScope // writes finish even if the screen closes
    val uiScope = rememberCoroutineScope()

    val todos by repo.activeTodos.collectAsState(initial = null)
    val categories by repo.categories.list.collectAsState()
    val status by app.backup.status.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var text by rememberSaveable { mutableStateOf("") }
    var categoryChoice by rememberSaveable { mutableStateOf(repo.categories.defaultCategory()) }
    val category = if (categoryChoice in categories) categoryChoice else categories.first()
    var filterChoice by rememberSaveable { mutableStateOf(ALL) }
    val filter = if (filterChoice == ALL || filterChoice in categories) filterChoice else ALL

    var today by remember { mutableStateOf(LocalDate.now()) }
    var batteryOk by remember { mutableStateOf(Device.isBatteryUnrestricted(context)) }
    var widgetOk by remember { mutableStateOf(Device.hasWidget(context)) }
    var setupHidden by remember { mutableStateOf(app.prefs.setupHidden) }
    var categoriesReviewed by remember { mutableStateOf(app.prefs.categoriesReviewed) }
    LifecycleResumeEffect(Unit) {
        today = LocalDate.now()
        batteryOk = Device.isBatteryUnrestricted(context)
        widgetOk = Device.hasWidget(context)
        categoriesReviewed = app.prefs.categoriesReviewed
        onPauseOrDispose { }
    }

    fun toast(message: String) {
        uiScope.launch { snackbar.showSnackbar(message) }
    }

    val backupUi = rememberBackupController(::toast)

    fun submit() {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val cat = category
        text = ""
        writeScope.launch { repo.add(clean, cat) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { ListoNavBar(Screen.Tasks, go) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
            // Header: today's date is the page title.
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(Fmt.dayName(today), style = MaterialTheme.typography.displaySmall)
                    Text(Fmt.dateLine(today), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { go(Screen.Settings) }) {
                    Icon(ListoIcons.Settings, contentDescription = "Settings")
                }
            }

            val list = todos
            if (list == null) {
                Spacer(Modifier.weight(1f))
            } else {
                val openTasks = list.filter { !it.done }
                Spacer(Modifier.height(18.dp))
                ChoiceRow(
                    options = listOf(ALL) + categories,
                    selected = filter,
                    onSelect = { filterChoice = it },
                    label = { if (it == ALL) "All" else it },
                    count = { option -> openTasks.count { option == ALL || it.category == option } },
                    dotColor = { option -> if (option == ALL) null else categoryColor(categories, option) },
                    contentPadding = PaddingValues(horizontal = 20.dp),
                )

                val visible = if (filter == ALL) list else list.filter { it.category == filter }
                val openVisible = visible.filter { !it.done }
                val doneVisible = visible.filter { it.done }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
                ) {
                    if (status.folderLabel == null) {
                        item(key = "backup-setup") {
                            BackupSetupCard(onChooseFolder = backupUi.chooseFolder, onRestore = backupUi.startRestore)
                        }
                    } else if (status.paused) {
                        item(key = "backup-paused") { PausedBanner(onFix = backupUi.chooseFolder) }
                    }
                    val showSetup = status.folderLabel != null && !setupHidden && (!batteryOk || !widgetOk || !categoriesReviewed)
                    if (showSetup) {
                        item(key = "setup") {
                            SetupCard(
                                categories = categories,
                                categoriesReviewed = categoriesReviewed,
                                batteryOk = batteryOk,
                                widgetOk = widgetOk,
                                onCategories = { go(Screen.Settings) },
                                onBattery = { Device.requestBatteryUnrestricted(context) },
                                onWidget = { if (!Device.requestPinWidget(context)) toast("Long-press the home screen, tap Widgets, then Listo") },
                                onHide = {
                                    app.prefs.setupHidden = true
                                    setupHidden = true
                                },
                            )
                        }
                    }

                    if (openVisible.isEmpty() && doneVisible.isEmpty()) {
                        item(key = "empty") {
                            Box(Modifier.fillParentMaxHeight(0.6f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                EmptyState(
                                    icon = ListoIcons.Tasks,
                                    title = if (list.isEmpty()) "Nothing to do" else "Nothing in $filter",
                                    body = "Add a task in the field below.",
                                )
                            }
                        }
                    }

                    itemsIndexed(openVisible, key = { _, t -> t.id }) { index, todo ->
                        GroupRow(index, openVisible.size, Modifier.animateItem()) {
                            TaskRow(
                                todo = todo,
                                categories = categories,
                                onToggle = { writeScope.launch { repo.toggle(todo.id) } },
                                onCategory = { cat -> writeScope.launch { repo.setCategory(todo.id, cat) } },
                                onDelete = { writeScope.launch { repo.delete(todo.id) } },
                            )
                        }
                    }

                    if (doneVisible.isNotEmpty()) {
                        item(key = "done-header") {
                            SectionLabel("Done (${doneVisible.size})", Modifier.animateItem()) {
                                TextButton(onClick = { writeScope.launch { repo.clearCompleted() } }) { Text("Clear completed") }
                            }
                        }
                        itemsIndexed(doneVisible, key = { _, t -> t.id }) { index, todo ->
                            GroupRow(index, doneVisible.size, Modifier.animateItem()) {
                                TaskRow(
                                    todo = todo,
                                    categories = categories,
                                    onToggle = { writeScope.launch { repo.toggle(todo.id) } },
                                    onCategory = null,
                                    onDelete = { writeScope.launch { repo.delete(todo.id) } },
                                )
                            }
                        }
                    }

                    item(key = "backup-line") {
                        Text(
                            text = backupLine(status),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        )
                    }
                }

                Composer(
                    categories = categories,
                    category = category,
                    onCategory = { categoryChoice = it },
                    text = text,
                    onText = { text = it.take(Repository.MAX_TEXT) },
                    onSubmit = ::submit,
                )
            }
        }
    }
}

@Composable
private fun TaskRow(
    todo: Todo,
    categories: List<String>,
    onToggle: () -> Unit,
    onCategory: ((String) -> Unit)?,
    onDelete: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(start = 4.dp, end = 2.dp),
    ) {
        RoundCheck(checked = todo.done, onToggle = onToggle)
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(
                text = todo.text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (todo.done) scheme.onSurfaceVariant else scheme.onSurface,
                textDecoration = if (todo.done) TextDecoration.LineThrough else null,
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryTag(categories, todo.category, onCategory)
                todo.dueDate?.let { due ->
                    Spacer(Modifier.width(14.dp))
                    Icon(ListoIcons.Repeat, contentDescription = "Repeats", tint = scheme.tertiary, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Due ${Fmt.shortDate(due)}", style = MaterialTheme.typography.labelMedium, color = scheme.tertiary)
                }
            }
        }
        IconButton(onClick = onDelete) {
            Icon(ListoIcons.Close, contentDescription = "Delete permanently", tint = scheme.outline, modifier = Modifier.size(18.dp))
        }
    }
}

/** Capture bar pinned above the navigation: category, text, add. */
@Composable
private fun Composer(
    categories: List<String>,
    category: String,
    onCategory: (String) -> Unit,
    text: String,
    onText: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
    ) {
        CategorySelector(categories, category, onCategory)
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(CircleShape)
                .background(scheme.surfaceContainerHigh)
                .padding(horizontal = 16.dp),
        ) {
            if (text.isEmpty()) {
                Text("Add a task", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
            }
            BasicTextField(
                value = text,
                onValueChange = onText,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        FilledIconButton(
            onClick = onSubmit,
            enabled = text.isNotBlank(),
            modifier = Modifier.size(44.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary,
                disabledContainerColor = scheme.surfaceContainerHigh,
                disabledContentColor = scheme.outline,
            ),
        ) {
            Icon(ListoIcons.ArrowUp, contentDescription = "Add task", modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun BackupSetupCard(onChooseFolder: () -> Unit, onRestore: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(ListoIcons.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Keep a backup on this phone", style = MaterialTheme.typography.titleMedium)
            Text(
                "Every change is saved to a file outside the app, so reinstalling or moving to a new phone never loses your tasks. " +
                    "Choose Documents and Listo makes a ListoBackup folder there.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Used Listo before? Restore the file Documents/ListoBackup/listo-backup.json.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                Button(onClick = onChooseFolder) { Text("Choose folder") }
                OutlinedButton(onClick = onRestore) { Text("Restore backup") }
            }
        }
    }
}

@Composable
private fun PausedBanner(onFix: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)) {
            Text(
                "Backups are paused. Choose the backup folder again to resume.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onFix) { Text("Choose folder", color = MaterialTheme.colorScheme.onErrorContainer) }
        }
    }
}

@Composable
private fun SetupCard(
    categories: List<String>,
    categoriesReviewed: Boolean,
    batteryOk: Boolean,
    widgetOk: Boolean,
    onCategories: () -> Unit,
    onBattery: () -> Unit,
    onWidget: () -> Unit,
    onHide: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
    ) {
        Column(Modifier.padding(top = 16.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 20.dp, end = 8.dp)) {
                Text("Finish setting up", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onHide) { Text("Hide") }
            }
            SetupStep(categoriesReviewed, "Your categories", categories.joinToString(", "), "Edit", onCategories)
            SetupStep(batteryOk, "Run in the background", "So recurring tasks appear on time", "Allow", onBattery)
            SetupStep(widgetOk, "Put the list on your home screen", "Stretch the widget to fill a page", "Add widget", onWidget)
        }
    }
}

@Composable
private fun SetupStep(done: Boolean, title: String, detail: String, action: String, onAction: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (done) scheme.primary else scheme.surfaceContainerHigh),
        ) {
            if (done) Icon(ListoIcons.Check, contentDescription = "Done", tint = scheme.onPrimary, modifier = Modifier.size(12.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (done) scheme.onSurfaceVariant else scheme.onSurface)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        if (!done) TextButton(onClick = onAction) { Text(action) }
    }
}
