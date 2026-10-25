package com.hariv.listo.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.hariv.listo.App
import com.hariv.listo.backup.BackupJson
import com.hariv.listo.backup.BackupManager
import com.hariv.listo.backup.BackupStatus
import kotlinx.coroutines.launch

/** The two backup actions any screen can offer. Dialogs are shown by [rememberBackupController]. */
class BackupController(val chooseFolder: () -> Unit, val startRestore: () -> Unit)

/** Folder picker, restore picker and their confirmation dialogs, shared by Tasks and Settings. */
@Composable
fun rememberBackupController(onMessage: (String) -> Unit): BackupController {
    val context = LocalContext.current
    val app = remember { App.get(context) }
    val repo = app.repo
    val uiScope = rememberCoroutineScope()

    var folderConflict by remember { mutableStateOf<BackupJson.Data?>(null) }
    var restoreCandidate by remember { mutableStateOf<BackupJson.Data?>(null) }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching { app.backup.setFolder(uri) }.isSuccess
        if (!ok) {
            onMessage("That folder can't be used. Choose Documents instead.")
            return@rememberLauncherForActivityResult
        }
        uiScope.launch {
            val existing = app.backup.readExisting()
            if (existing != null && !existing.isEmpty) {
                folderConflict = existing
            } else {
                app.appScope.launch { repo.backupNow() }
                onMessage("Backing up to ${app.backup.status.value.folderLabel}")
            }
        }
    }

    val restorePicker = rememberLauncherForActivityResult(com.hariv.listo.backup.OpenBackupFile()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        uiScope.launch {
            try {
                restoreCandidate = repo.readBackup(uri)
            } catch (e: Exception) {
                onMessage(e.message ?: "That file can't be read.")
            }
        }
    }

    folderConflict?.let { existing ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text("This folder already has a backup") },
            text = {
                Text(
                    "Saved ${savedLabel(existing.savedAt)}, with ${plural(existing.todos.size, "task")} and " +
                        "${plural(existing.rules.size, "recurring rule")}. Restore it, or replace it with what's on this phone?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    folderConflict = null
                    app.appScope.launch {
                        repo.restore(existing)
                        onMessage("Restored ${plural(existing.todos.size, "task")}")
                    }
                }) { Text("Restore backup") }
            },
            dismissButton = {
                TextButton(onClick = {
                    folderConflict = null
                    app.appScope.launch { repo.backupNow() }
                }) { Text("Replace it") }
            },
        )
    }

    restoreCandidate?.let { data ->
        AlertDialog(
            onDismissRequest = { restoreCandidate = null },
            title = { Text("Restore this backup?") },
            text = {
                Text(
                    "Saved ${savedLabel(data.savedAt)}, with ${plural(data.todos.size, "task")} (including the archive) and " +
                        "${plural(data.rules.size, "recurring rule")}. Everything on this phone is replaced."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    restoreCandidate = null
                    app.appScope.launch {
                        repo.restore(data)
                        onMessage(if (app.backup.isSetUp) "Backup restored" else "Backup restored. Choose a backup folder to keep it up to date.")
                    }
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { restoreCandidate = null }) { Text("Cancel") } },
        )
    }

    return BackupController(
        chooseFolder = { folderPicker.launch(BackupManager.folderPickerStartUri()) },
        startRestore = { restorePicker.launch(app.backup.restoreStartUri()) },
    )
}

fun savedLabel(ms: Long) = if (ms > 0) Fmt.fullDateTime(ms) else "on an unknown date"

fun plural(n: Int, word: String) = if (n == 1) "1 $word" else "$n ${word}s"

fun backupLine(s: BackupStatus): String = when {
    s.folderLabel == null -> "Not backed up yet"
    s.paused -> "Backups paused. Choose the folder again in Settings."
    s.lastBackupAt == 0L -> "Backing up to ${s.folderLabel}"
    else -> "Backed up ${Fmt.whenShort(s.lastBackupAt)} to ${s.folderLabel}"
}
