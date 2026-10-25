package com.hariv.simpletodo.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.hariv.simpletodo.App
import com.hariv.simpletodo.backup.BackupJson
import com.hariv.simpletodo.backup.BackupManager
import com.hariv.simpletodo.backup.BackupStatus
import com.hariv.simpletodo.backup.OpenBackupFile
import com.hariv.simpletodo.data.CATEGORIES
import com.hariv.simpletodo.data.Repository
import com.hariv.simpletodo.data.Todo
import com.hariv.simpletodo.update.Updater
import kotlinx.coroutines.launch

private const val ALL = "all"

private sealed interface UpdateState {
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data object NoReleases : UpdateState
    data class Available(val release: Updater.Release) : UpdateState
    data class NeedsPermission(val release: Updater.Release) : UpdateState
    data object Downloading : UpdateState
    data class Failed(val message: String) : UpdateState
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(go: (Screen) -> Unit) {
    val context = LocalContext.current
    val app = remember { App.get(context) }
    val repo = app.repo
    val writeScope = app.appScope // writes finish even if the screen closes
    val uiScope = rememberCoroutineScope()

    val todos by repo.activeTodos.collectAsState(initial = null)
    val status by app.backup.status.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var text by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(app.prefs.lastCategory) }
    var filter by rememberSaveable { mutableStateOf(ALL) }
    var showMenu by remember { mutableStateOf(false) }

    var batteryOk by remember { mutableStateOf(Device.isBatteryUnrestricted(context)) }
    var widgetOk by remember { mutableStateOf(Device.hasWidget(context)) }
    var setupHidden by remember { mutableStateOf(app.prefs.setupHidden) }
    LifecycleResumeEffect(Unit) {
        batteryOk = Device.isBatteryUnrestricted(context)
        widgetOk = Device.hasWidget(context)
        onPauseOrDispose { }
    }

    var folderConflict by remember { mutableStateOf<BackupJson.Data?>(null) }
    var restoreCandidate by remember { mutableStateOf<BackupJson.Data?>(null) }
    var updateState by remember { mutableStateOf<UpdateState?>(null) }

    fun toast(message: String) {
        uiScope.launch { snackbar.showSnackbar(message) }
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val ok = try {
                app.backup.setFolder(uri)
                true
            } catch (e: Exception) {
                false
            }
            if (!ok) {
                toast("Couldn't use that folder. Try Documents.")
            } else {
                uiScope.launch {
                    val existing = app.backup.readExisting()
                    if (existing != null && !existing.isEmpty) {
                        folderConflict = existing
                    } else {
                        writeScope.launch { repo.backupNow() }
                        toast("Backups on: ${app.backup.status.value.folderLabel}")
                    }
                }
            }
        }
    }

    val restorePicker = rememberLauncherForActivityResult(OpenBackupFile()) { uri ->
        if (uri != null) {
            uiScope.launch {
                try {
                    restoreCandidate = repo.readBackup(uri)
                } catch (e: Exception) {
                    toast(e.message ?: "Couldn't read that file.")
                }
            }
        }
    }

    fun chooseFolder() = folderPicker.launch(BackupManager.folderPickerStartUri())
    fun startRestore() = restorePicker.launch(app.backup.restoreStartUri())

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
                UpdateState.Failed(e.message ?: "No internet connection")
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
                updateState = UpdateState.Failed(e.message ?: "Download failed")
            }
        }
    }

    fun submit() {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val cat = category
        text = ""
        writeScope.launch { repo.add(clean, cat) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Todo") },
                actions = {
                    TextButton(onClick = { go(Screen.Recurring) }) { Text("Recurring") }
                    TextButton(onClick = { go(Screen.Archive) }) { Text("Archive") }
                    TextButton(onClick = { go(Screen.Help) }) { Text("?") }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            val pick: (() -> Unit) -> Unit = { action -> showMenu = false; action() }
                            DropdownMenuItem(text = { Text("Restore from backup") }, onClick = { pick { startRestore() } })
                            DropdownMenuItem(
                                text = { Text(if (status.folderLabel == null) "Set up backup folder" else "Change backup folder") },
                                onClick = { pick { chooseFolder() } },
                            )
                            if (status.folderLabel != null) {
                                DropdownMenuItem(text = { Text("Back up now") }, onClick = {
                                    pick {
                                        writeScope.launch {
                                            val ok = repo.backupNow()
                                            toast(if (ok) "Backed up" else "Backup failed. Check the backup folder.")
                                        }
                                    }
                                })
                            }
                            DropdownMenuItem(text = { Text("Add widget to home screen") }, onClick = {
                                pick {
                                    if (!Device.requestPinWidget(context)) {
                                        toast("Long-press the home screen → Widgets → Simple Todo")
                                    }
                                }
                            })
                            DropdownMenuItem(
                                text = { Text("Battery: allow background") },
                                onClick = { pick { Device.requestBatteryUnrestricted(context) } },
                            )
                            DropdownMenuItem(text = { Text("Check for update") }, onClick = { pick { checkForUpdate() } })
                            DropdownMenuItem(
                                text = { Text("Version ${Updater.currentVersion}") },
                                onClick = { showMenu = false },
                                enabled = false,
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .padding(horizontal = 16.dp),
        ) {
            if (status.folderLabel == null) {
                BackupSetupCard(onRestore = ::startRestore, onChooseFolder = ::chooseFolder)
            } else if (status.paused) {
                PausedBanner(onFix = ::chooseFolder)
            }
            if (status.folderLabel != null && !setupHidden && (!batteryOk || !widgetOk)) {
                SetupCard(
                    batteryOk = batteryOk,
                    widgetOk = widgetOk,
                    onBattery = { Device.requestBatteryUnrestricted(context) },
                    onWidget = {
                        if (!Device.requestPinWidget(context)) toast("Long-press the home screen → Widgets → Simple Todo")
                    },
                    onHide = {
                        app.prefs.setupHidden = true
                        setupHidden = true
                    },
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(Repository.MAX_TEXT) },
                label = { Text("What needs doing?") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                CategoryPicker(selected = category, onSelect = { category = it })
                Spacer(Modifier.weight(1f))
                Button(onClick = { submit() }, enabled = text.isNotBlank()) { Text("Add") }
            }

            val list = todos
            if (list == null) {
                Spacer(Modifier.weight(1f))
            } else {
                val open = list.filter { !it.done }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(selected = filter == ALL, onClick = { filter = ALL }, label = { Text("All ${open.size}") })
                    CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = filter == cat,
                            onClick = { filter = cat },
                            label = { Text("$cat ${open.count { it.category == cat }}") },
                        )
                    }
                }

                val visible = if (filter == ALL) list else list.filter { it.category == filter }
                if (visible.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (list.isEmpty()) "Nothing to do. Nice." else "Nothing in $filter.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(Modifier.weight(1f)) {
                        items(visible, key = { it.id }) { todo ->
                            TodoRow(
                                todo = todo,
                                onToggle = { writeScope.launch { repo.toggle(todo.id) } },
                                onCategory = { cat -> writeScope.launch { repo.setCategory(todo.id, cat) } },
                                onDelete = { writeScope.launch { repo.delete(todo.id) } },
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("${open.size} left", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    if (list.any { it.done }) {
                        TextButton(onClick = { writeScope.launch { repo.clearCompleted() } }) { Text("Clear completed") }
                    }
                }
                Text(
                    text = backupLine(status),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
    }

    folderConflict?.let { existing ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text("A backup is already here") },
            text = {
                Text(
                    "Saved ${savedLabel(existing.savedAt)} with ${existing.todos.size} todos and " +
                        "${existing.rules.size} recurring rules. Restore it onto this phone, or replace it with what's on this phone now?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    folderConflict = null
                    writeScope.launch {
                        repo.restore(existing)
                        toast("Restored ${existing.todos.size} todos and ${existing.rules.size} rules")
                    }
                }) { Text("Restore it") }
            },
            dismissButton = {
                TextButton(onClick = {
                    folderConflict = null
                    writeScope.launch { repo.backupNow() }
                }) { Text("Replace with this phone's") }
            },
        )
    }

    restoreCandidate?.let { data ->
        AlertDialog(
            onDismissRequest = { restoreCandidate = null },
            title = { Text("Restore this backup?") },
            text = {
                Text(
                    "Saved ${savedLabel(data.savedAt)}: ${data.todos.size} todos (including the archive) and " +
                        "${data.rules.size} recurring rules. This replaces everything on this phone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    restoreCandidate = null
                    writeScope.launch {
                        repo.restore(data)
                        toast(
                            if (app.backup.isSetUp) "Restored"
                            else "Restored. Now tap \"Choose folder\" so backups keep going."
                        )
                    }
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { restoreCandidate = null }) { Text("Cancel") } },
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
                        UpdateState.UpToDate -> "You have the latest version (${Updater.currentVersion})."
                        UpdateState.NoReleases -> "No releases published yet."
                        is UpdateState.Available -> "Version ${st.release.version} is available (you have ${Updater.currentVersion}). Your todos stay as they are."
                        is UpdateState.NeedsPermission -> "Turn on \"Allow from this source\" for Simple Todo in the screen that opened, then come back and tap Install."
                        UpdateState.Downloading -> "Downloading…"
                        is UpdateState.Failed -> "Couldn't update: ${st.message}"
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

private fun savedLabel(ms: Long) = if (ms > 0) Fmt.fullDateTime(ms) else "(date unknown)"

private fun backupLine(s: BackupStatus): String = when {
    s.folderLabel == null -> "Backup not set up"
    s.paused -> "Backups paused · ${s.folderLabel}"
    s.lastBackupAt == 0L -> "Backup folder: ${s.folderLabel}"
    else -> "Backed up ${Fmt.whenShort(s.lastBackupAt)} · ${s.folderLabel}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoRow(
    todo: Todo,
    onToggle: () -> Unit,
    onCategory: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = todo.done, onCheckedChange = { onToggle() })
        Column(Modifier.weight(1f)) {
            val base = MaterialTheme.typography.bodyLarge
            Text(
                text = todo.text,
                style = if (todo.done) base.copy(textDecoration = TextDecoration.LineThrough) else base,
                color = if (todo.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            todo.dueDate?.let {
                Text(
                    text = "↻ ${Fmt.shortDate(it)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box {
            AssistChip(onClick = { menu = true }, label = { Text(todo.category) })
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                CATEGORIES.forEach { cat ->
                    DropdownMenuItem(text = { Text("Move to $cat") }, onClick = {
                        menu = false
                        onCategory(cat)
                    })
                }
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Close, contentDescription = "Delete permanently")
        }
    }
}

@Composable
private fun BackupSetupCard(onRestore: () -> Unit, onChooseFolder: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Set up backup", style = MaterialTheme.typography.titleMedium)
            Text(
                "Every change is saved to a file on your phone, so reinstalling or a new phone never loses your todos.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Used this app before? Restore your backup — it's in Documents/TodoBackup/todo-backup.json. " +
                    "New here? Choose a folder: pick Documents and the app makes a TodoBackup folder inside it.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRestore) { Text("Restore from backup") }
                OutlinedButton(onClick = onChooseFolder) { Text("Choose folder") }
            }
        }
    }
}

@Composable
private fun PausedBanner(onFix: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable(onClick = onFix),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Text(
            "Backups paused — tap to fix (choose the backup folder again)",
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun SetupCard(
    batteryOk: Boolean,
    widgetOk: Boolean,
    onBattery: () -> Unit,
    onWidget: () -> Unit,
    onHide: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Finish setup", style = MaterialTheme.typography.titleMedium)
            SetupLine(true, "Backup folder chosen", null, null)
            SetupLine(batteryOk, "Allow background, so recurring todos appear on time", "Allow", onBattery)
            SetupLine(widgetOk, "Put the list on a home-screen page", "Add widget", onWidget)
            TextButton(onClick = onHide, modifier = Modifier.align(Alignment.End)) { Text("Hide") }
        }
    }
}

@Composable
private fun SetupLine(done: Boolean, label: String, action: String?, onAction: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(if (done) "✓" else "•", modifier = Modifier.width(20.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        if (!done && action != null && onAction != null) {
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}
