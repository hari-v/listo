package com.hariv.listo.ui

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.hariv.listo.App
import com.hariv.listo.data.CategoryStore
import com.hariv.listo.ui.theme.ListoIcons
import com.hariv.listo.ui.theme.categoryColor
import com.hariv.listo.update.Updater
import kotlinx.coroutines.launch

private sealed interface UpdateState {
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data object NoReleases : UpdateState
    data class Available(val release: Updater.Release) : UpdateState
    data class NeedsPermission(val release: Updater.Release) : UpdateState
    data object Downloading : UpdateState
    data class Failed(val message: String) : UpdateState
}

private data class DeleteRequest(val name: String, val openTasks: Int, val rules: Int)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(go: (Screen) -> Unit, back: () -> Unit) {
    val context = LocalContext.current
    val app = remember { App.get(context) }
    val repo = app.repo
    val uiScope = rememberCoroutineScope()
    val categories by repo.categories.list.collectAsState()
    val status by app.backup.status.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var batteryOk by remember { mutableStateOf(Device.isBatteryUnrestricted(context)) }
    var widgetOk by remember { mutableStateOf(Device.hasWidget(context)) }
    LifecycleResumeEffect(Unit) {
        batteryOk = Device.isBatteryUnrestricted(context)
        widgetOk = Device.hasWidget(context)
        onPauseOrDispose { }
    }
    LaunchedEffect(Unit) { app.prefs.categoriesReviewed = true }

    var newCategory by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<DeleteRequest?>(null) }
    var updateState by remember { mutableStateOf<UpdateState?>(null) }

    fun toast(message: String) {
        uiScope.launch { snackbar.showSnackbar(message) }
    }

    val backupUi = rememberBackupController(::toast)

    fun addCategory() {
        val problem = repo.categories.problemWith(newCategory)
        if (problem != null) {
            toast(problem)
            return
        }
        val name = newCategory
        newCategory = ""
        app.appScope.launch { repo.addCategory(name) }
    }

    fun checkForUpdate() {
        updateState = UpdateState.Checking
        uiScope.launch {
            updateState = try {
                val release = Updater.latestRelease()
                when {
                    release == null -> UpdateState.NoReleases
                    Updater.isNewer(release.version, Updater.currentVersion) -> UpdateState.Available(release)
                    else -> UpdateState.UpToDate
                }
            } catch (e: Exception) {
                UpdateState.Failed(e.message ?: "No internet connection.")
            }
        }
    }

    fun installUpdate(release: Updater.Release) {
        if (!Updater.ensureInstallAllowed(context)) {
            updateState = UpdateState.NeedsPermission(release)
            return
        }
        updateState = UpdateState.Downloading
        uiScope.launch {
            try {
                val apk = Updater.download(context, release)
                updateState = null
                Updater.install(context, apk)
            } catch (e: Exception) {
                updateState = UpdateState.Failed(e.message ?: "The download failed.")
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
            PageHeader("Settings", onBack = back)
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp)) {
                // ---- Categories ----
                item(key = "cat-label") { SectionLabel("Categories") }
                val rows = categories.size + 1
                itemsIndexed(categories, key = { _, c -> "cat-$c" }) { index, name ->
                    GroupRow(index, rows, Modifier.animateItem(), dividerInset = 44.dp) {
                        CategoryRow(
                            name = name,
                            categories = categories,
                            canMoveUp = index > 0,
                            canMoveDown = index < categories.size - 1,
                            canDelete = categories.size > 1,
                            onMove = { delta -> repo.moveCategory(name, delta) },
                            onRename = { renaming = name },
                            onDelete = {
                                uiScope.launch {
                                    val (open, rules) = repo.categoryUsage(name)
                                    deleting = DeleteRequest(name, open, rules)
                                }
                            },
                        )
                    }
                }
                item(key = "cat-add") {
                    GroupRow(categories.size, rows) {
                        AddCategoryRow(
                            value = newCategory,
                            onValueChange = { newCategory = it.take(CategoryStore.MAX_LENGTH) },
                            full = categories.size >= CategoryStore.MAX_COUNT,
                            onAdd = ::addCategory,
                        )
                    }
                }
                item(key = "cat-hint") {
                    Text(
                        "Up to ${CategoryStore.MAX_COUNT}, shown in this order in the app and on the widget. Renaming updates every task and rule.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                    )
                }

                // ---- Backup ----
                item(key = "backup-label") { SectionLabel("Backup") }
                item(key = "backup-folder") {
                    GroupRow(0, 3) {
                        InfoRow(
                            icon = ListoIcons.Folder,
                            title = "Backup folder",
                            value = status.folderLabel ?: "Not set up",
                            trailing = {
                                TextButton(onClick = backupUi.chooseFolder) { Text(if (status.folderLabel == null) "Choose" else "Change") }
                            },
                        )
                    }
                }
                item(key = "backup-last") {
                    GroupRow(1, 3) {
                        InfoRow(
                            icon = ListoIcons.Save,
                            title = "Last backup",
                            value = when {
                                status.folderLabel == null -> "Choose a folder first"
                                status.paused -> "Paused. Choose the folder again."
                                status.lastBackupAt == 0L -> "Not yet"
                                else -> Fmt.fullDateTime(status.lastBackupAt)
                            },
                            trailing = {
                                if (status.folderLabel != null) {
                                    TextButton(onClick = {
                                        app.appScope.launch {
                                            val ok = repo.backupNow()
                                            toast(if (ok) "Backed up" else "Backup failed. Choose the folder again.")
                                        }
                                    }) { Text("Back up now") }
                                }
                            },
                        )
                    }
                }
                item(key = "backup-restore") {
                    GroupRow(2, 3) {
                        InfoRow(
                            icon = ListoIcons.Restore,
                            title = "Restore from backup",
                            value = "Replaces everything on this phone",
                            onClick = backupUi.startRestore,
                            trailing = { Chevron() },
                        )
                    }
                }

                // ---- Phone ----
                item(key = "phone-label") { SectionLabel("Phone") }
                item(key = "phone-battery") {
                    GroupRow(0, 2) {
                        InfoRow(
                            icon = ListoIcons.Battery,
                            title = "Run in background",
                            value = if (batteryOk) "Allowed, so recurring tasks appear on time" else "Not allowed; recurring tasks may appear late",
                            trailing = { if (!batteryOk) TextButton(onClick = { Device.requestBatteryUnrestricted(context) }) { Text("Allow") } },
                        )
                    }
                }
                item(key = "phone-widget") {
                    GroupRow(1, 2) {
                        InfoRow(
                            icon = ListoIcons.Widget,
                            title = "Home-screen widget",
                            value = if (widgetOk) "On your home screen" else "Not added yet",
                            trailing = {
                                TextButton(onClick = {
                                    if (!Device.requestPinWidget(context)) toast("Long-press the home screen, tap Widgets, then Listo")
                                }) { Text(if (widgetOk) "Add another" else "Add") }
                            },
                        )
                    }
                }

                // ---- About ----
                item(key = "about-label") { SectionLabel("About") }
                item(key = "about-help") {
                    GroupRow(0, 2) {
                        InfoRow(icon = ListoIcons.Help, title = "How Listo works", onClick = { go(Screen.Help) }, trailing = { Chevron() })
                    }
                }
                item(key = "about-update") {
                    GroupRow(1, 2) {
                        InfoRow(
                            icon = ListoIcons.Update,
                            title = "Check for update",
                            value = "Version ${Updater.currentVersion}",
                            onClick = ::checkForUpdate,
                            trailing = { Chevron() },
                        )
                    }
                }
                item(key = "credits") {
                    Text(
                        "Typeface: Hanken Grotesk, SIL Open Font License 1.1. Icons adapted from Lucide (ISC).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(start = 4.dp, top = 20.dp),
                    )
                }
            }
        }
    }

    renaming?.let { old ->
        var value by remember(old) { mutableStateOf(old) }
        val problem = if (value.trim() == old) null else repo.categories.problemWith(value, renaming = old)
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename \"$old\"") },
            text = {
                Column {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it.take(CategoryStore.MAX_LENGTH) },
                        singleLine = true,
                        isError = problem != null,
                        supportingText = { Text(problem ?: "Every task and rule in \"$old\" moves with it.") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = problem == null && value.isNotBlank(),
                    onClick = {
                        renaming = null
                        app.appScope.launch { repo.renameCategory(old, value) }
                    },
                ) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } },
        )
    }

    deleting?.let { request ->
        val others = categories.filter { it != request.name }
        var moveTo by remember(request) { mutableStateOf(others.firstOrNull() ?: "") }
        val inUse = request.openTasks + request.rules > 0
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete \"${request.name}\"?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (inUse) {
                        Text(
                            "${plural(request.openTasks, "open task")} and ${plural(request.rules, "recurring rule")} move to the category you choose. " +
                                "Archived tasks keep their old label."
                        )
                        ChoiceRow(
                            options = others,
                            selected = moveTo,
                            onSelect = { moveTo = it },
                            dotColor = { categoryColor(categories, it) },
                        )
                    } else {
                        Text("No open tasks or rules use it. Archived tasks keep their old label.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    app.appScope.launch { repo.deleteCategory(request.name, moveTo) }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }

    updateState?.let { st ->
        AlertDialog(
            onDismissRequest = { if (st !is UpdateState.Downloading) updateState = null },
            title = { Text("App update") },
            text = {
                Text(
                    when (st) {
                        UpdateState.Checking -> "Checking GitHub for a newer version…"
                        UpdateState.UpToDate -> "You have the latest version, ${Updater.currentVersion}."
                        UpdateState.NoReleases -> "No versions have been published yet."
                        is UpdateState.Available -> "Version ${st.release.version} is available. You have ${Updater.currentVersion}. Your tasks stay as they are."
                        is UpdateState.NeedsPermission -> "Turn on \"Allow from this source\" for Listo in the screen that opened, then come back and tap Install."
                        UpdateState.Downloading -> "Downloading…"
                        is UpdateState.Failed -> "The update didn't complete: ${st.message}"
                    }
                )
            },
            confirmButton = {
                when (st) {
                    is UpdateState.Available -> TextButton(onClick = { installUpdate(st.release) }) { Text("Install") }
                    is UpdateState.NeedsPermission -> TextButton(onClick = { installUpdate(st.release) }) { Text("Install") }
                    UpdateState.Checking, UpdateState.Downloading -> Unit
                    else -> TextButton(onClick = { updateState = null }) { Text("OK") }
                }
            },
            dismissButton = {
                if (st is UpdateState.Available || st is UpdateState.NeedsPermission) {
                    TextButton(onClick = { updateState = null }) { Text("Later") }
                }
            },
        )
    }
}

@Composable
private fun Chevron() {
    Icon(
        ListoIcons.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(end = 8.dp).size(18.dp),
    )
}

@Composable
private fun CategoryRow(
    name: String,
    categories: List<String>,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canDelete: Boolean,
    onMove: (Int) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(56.dp).padding(start = 18.dp, end = 4.dp)) {
        CategoryDot(categoryColor(categories, name), 10.dp)
        Spacer(Modifier.width(16.dp))
        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
            Icon(ListoIcons.ChevronUp, contentDescription = "Move $name up", tint = if (canMoveUp) scheme.onSurfaceVariant else scheme.outlineVariant)
        }
        IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
            Icon(ListoIcons.ChevronDown, contentDescription = "Move $name down", tint = if (canMoveDown) scheme.onSurfaceVariant else scheme.outlineVariant)
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(ListoIcons.More, contentDescription = "More options for $name") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    leadingIcon = { Icon(ListoIcons.Pencil, null, modifier = Modifier.size(18.dp)) },
                    onClick = { menu = false; onRename() },
                )
                DropdownMenuItem(
                    text = { Text(if (canDelete) "Delete" else "Delete (keep at least one)") },
                    enabled = canDelete,
                    leadingIcon = { Icon(ListoIcons.Trash, null, modifier = Modifier.size(18.dp)) },
                    onClick = { menu = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun AddCategoryRow(value: String, onValueChange: (String) -> Unit, full: Boolean, onAdd: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(60.dp).padding(start = 14.dp, end = 4.dp)) {
        Icon(ListoIcons.Plus, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(CircleShape)
                .background(scheme.surfaceContainerHigh)
                .padding(horizontal = 14.dp),
        ) {
            if (value.isEmpty()) {
                Text(
                    if (full) "Limit of ${CategoryStore.MAX_COUNT} reached" else "New category",
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = !full,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAdd() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        TextButton(onClick = onAdd, enabled = !full && value.isNotBlank()) { Text("Add") }
    }
}
