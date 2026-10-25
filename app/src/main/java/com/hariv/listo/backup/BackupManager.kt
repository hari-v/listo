package com.hariv.listo.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.hariv.listo.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

data class BackupStatus(
    /** e.g. "Documents/ListoBackup", or null when no folder is set up. */
    val folderLabel: String?,
    val lastBackupAt: Long,
    val paused: Boolean,
)

/** Reads and writes listo-backup.json in a folder the user picked (Storage Access Framework). */
class BackupManager(private val context: Context, private val prefs: Prefs) {

    private val _status = MutableStateFlow(currentStatus())
    val status: StateFlow<BackupStatus> = _status

    private fun currentStatus() = BackupStatus(
        folderLabel = prefs.backupTreeUri?.let { folderLabel(it.toUri()) },
        lastBackupAt = prefs.lastBackupAt,
        paused = prefs.backupPaused,
    )

    private fun refresh() {
        _status.value = currentStatus()
    }

    val isSetUp: Boolean get() = prefs.backupTreeUri != null

    /** Remember the folder the user picked, with permanent access. */
    fun setFolder(treeUri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(treeUri, flags)
        prefs.backupTreeUri = treeUri.toString()
        prefs.backupPaused = false
        refresh()
    }

    /** The ListoBackup folder: the picked folder itself if it is called ListoBackup, else a ListoBackup folder inside it. */
    private fun backupDir(create: Boolean): DocumentFile? {
        val uri = prefs.backupTreeUri?.toUri() ?: return null
        val tree = DocumentFile.fromTreeUri(context, uri) ?: return null
        if (!tree.exists()) return null
        if (tree.name == DIR_NAME) return tree
        tree.findFile(DIR_NAME)?.let { if (it.isDirectory) return it }
        return if (create) tree.createDirectory(DIR_NAME) else null
    }

    private fun backupFile(): DocumentFile? =
        runCatching { backupDir(create = false)?.findFile(FILE_NAME)?.takeIf { it.isFile } }.getOrNull()

    /** The backup already in the chosen folder, if there is one and it can be read. */
    suspend fun readExisting(): BackupJson.Data? = withContext(Dispatchers.IO) {
        val file = backupFile() ?: return@withContext null
        runCatching { BackupJson.decode(readText(file.uri)) }.getOrNull()
    }

    suspend fun write(json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = backupDir(create = true) ?: error("Backup folder not available")
            val file = dir.findFile(FILE_NAME)
                ?: dir.createFile("application/octet-stream", FILE_NAME)
                ?: error("Could not create $FILE_NAME")
            val out = context.contentResolver.openOutputStream(file.uri, "wt") ?: error("Could not open $FILE_NAME")
            out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            prefs.lastBackupAt = System.currentTimeMillis()
            prefs.backupPaused = false
            refresh()
            true
        } catch (e: Exception) {
            prefs.backupPaused = true
            refresh()
            false
        }
    }

    suspend fun readText(uri: Uri): String = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: error("Could not open the file")
        input.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    /** Where the restore picker should open. */
    fun restoreStartUri(): Uri =
        runCatching { backupDir(create = false)?.uri }.getOrNull() ?: documentUri("primary:Documents/$DIR_NAME")

    companion object {
        const val DIR_NAME = "ListoBackup"
        const val FILE_NAME = "listo-backup.json"
        private const val EXTERNAL_STORAGE = "com.android.externalstorage.documents"

        fun documentUri(documentId: String): Uri = DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE, documentId)

        /** Where the folder picker should open. */
        fun folderPickerStartUri(): Uri = documentUri("primary:Documents")

        fun folderLabel(treeUri: Uri): String {
            val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return DIR_NAME
            val path = docId.substringAfter(':', docId).trim('/').ifBlank { "Phone storage" }
            return if (path.endsWith(DIR_NAME)) path else "$path/$DIR_NAME"
        }
    }
}

/** Opens the system file picker for listo-backup.json, starting in the given folder. */
class OpenBackupFile : ActivityResultContract<Uri?, Uri?>() {
    override fun createIntent(context: Context, input: Uri?): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            if (input != null) putExtra(DocumentsContract.EXTRA_INITIAL_URI, input)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}
