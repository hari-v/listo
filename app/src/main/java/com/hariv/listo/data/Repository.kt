package com.hariv.listo.data

import android.content.Context
import android.net.Uri
import androidx.glance.appwidget.updateAll
import androidx.room.withTransaction
import com.hariv.listo.backup.BackupJson
import com.hariv.listo.backup.BackupManager
import com.hariv.listo.recurring.Recurrence
import com.hariv.listo.widget.TodoWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.util.UUID

/** All reads and writes go through here, so every change triggers a backup and a widget refresh. */
class Repository(
    private val context: Context,
    private val db: AppDatabase,
    private val prefs: Prefs,
    val backup: BackupManager,
    private val scope: CoroutineScope,
) {
    val activeTodos: Flow<List<Todo>> = db.todos().observeActive().map { sortActive(it) }
    val archivedTodos: Flow<List<Todo>> = db.todos().observeArchived()
    val rules: Flow<List<Rule>> = db.rules().observeAll()

    suspend fun activeTodosNow(): List<Todo> = sortActive(db.todos().active())

    // ---- Todos ----

    suspend fun add(text: String, category: String) {
        val clean = text.trim().take(MAX_TEXT)
        if (clean.isEmpty()) return
        db.todos().insert(Todo(id = newId(), text = clean, category = category, createdAt = now()))
        prefs.lastCategory = category
        changed()
    }

    suspend fun toggle(id: String) {
        if (db.todos().toggle(id, now()) > 0) changed()
    }

    suspend fun setCategory(id: String, category: String) {
        if (db.todos().setCategory(id, category) > 0) changed()
    }

    suspend fun delete(id: String) {
        if (db.todos().delete(id) > 0) changed()
    }

    suspend fun clearCompleted() {
        if (db.todos().clearCompleted(now()) > 0) changed()
    }

    // ---- Recurring rules ----

    suspend fun getRule(id: String): Rule? = db.rules().get(id)

    suspend fun saveRule(rule: Rule) {
        db.rules().upsert(rule)
        changed()
        runMaintenance()
    }

    suspend fun deleteRule(id: String) {
        db.rules().delete(id)
        changed()
    }

    suspend fun setRulePaused(id: String, paused: Boolean) {
        db.rules().setPaused(id, paused)
        changed()
        if (!paused) runMaintenance()
    }

    fun newRuleId(): String = newId()

    // ---- Daily maintenance: auto-archive + recurring todos ----

    private val maintenanceLock = Mutex()

    /** Archives items done more than 7 days ago and creates due recurring todos (1 day early). */
    suspend fun runMaintenance(today: LocalDate = LocalDate.now()): Boolean = maintenanceLock.withLock {
        var didChange = false
        val now = now()
        if (db.todos().autoArchive(now, now - ARCHIVE_AFTER_MS) > 0) didChange = true

        val upTo = today.plusDays(1)
        for (rule in db.rules().all()) {
            if (rule.paused) continue
            val schedule = runCatching { rule.schedule() }.getOrNull() ?: continue
            val due = Recurrence.latestDue(schedule, rule.lastDue(), upTo) ?: continue
            db.withTransaction {
                db.todos().insert(
                    Todo(
                        id = newId(),
                        text = rule.text,
                        category = rule.category,
                        createdAt = now,
                        recurringId = rule.id,
                        dueDate = due.toString(),
                    )
                )
                db.rules().setLastDue(rule.id, due.toString())
            }
            didChange = true
        }
        if (didChange) changed()
        didChange
    }

    // ---- Backup & restore ----

    private var backupJob: Job? = null

    /** Backs up 1 second after the last change, and refreshes the widget. */
    private fun changed() {
        synchronized(this) {
            backupJob?.cancel()
            backupJob = scope.launch {
                delay(1000)
                backupNow()
            }
        }
        scope.launch { runCatching { TodoWidget().updateAll(context) } }
    }

    /** Writes the pending backup right away (used before the app may be put to sleep). */
    suspend fun flushBackup() {
        synchronized(this) { backupJob?.cancel() }
        backupNow()
    }

    suspend fun backupNow(): Boolean {
        if (!backup.isSetUp) return false
        val todos = db.todos().all()
        val rules = db.rules().all()
        if (todos.isEmpty() && rules.isEmpty()) {
            // Never overwrite a backup that has data with an empty list.
            val existing = backup.readExisting()
            if (existing != null && !existing.isEmpty) return false
        }
        return backup.write(BackupJson.encode(todos, rules, now()))
    }

    suspend fun isEmpty(): Boolean = db.todos().all().isEmpty() && db.rules().all().isEmpty()

    suspend fun readBackup(uri: Uri): BackupJson.Data = BackupJson.decode(backup.readText(uri))

    /** Replaces everything on the phone with the backup's contents. */
    suspend fun restore(data: BackupJson.Data) {
        db.withTransaction {
            db.todos().deleteAll()
            db.rules().deleteAll()
            db.todos().insertAll(data.todos)
            db.rules().insertAll(data.rules)
        }
        changed()
        runMaintenance()
    }

    companion object {
        const val MAX_TEXT = 200
        const val ARCHIVE_AFTER_MS = 7L * 24 * 60 * 60 * 1000

        private fun newId() = UUID.randomUUID().toString()
        private fun now() = System.currentTimeMillis()

        /** Open items first (newest first), then done items (most recently completed first). */
        fun sortActive(list: List<Todo>): List<Todo> =
            list.sortedWith(
                compareBy<Todo> { it.done }
                    .thenByDescending { if (it.done) it.completedAt ?: 0L else it.createdAt }
            )
    }
}
