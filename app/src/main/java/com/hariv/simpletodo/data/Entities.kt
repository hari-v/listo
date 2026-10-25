package com.hariv.simpletodo.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hariv.simpletodo.recurring.RepeatUnit
import com.hariv.simpletodo.recurring.Schedule
import java.time.LocalDate

/** The fixed category list. Change it here (and only here). */
val CATEGORIES = listOf("pw", "my", "bl")

@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey val id: String,
    val text: String,
    val category: String,
    val done: Boolean = false,
    val createdAt: Long,
    val completedAt: Long? = null,
    val archivedAt: Long? = null,
    /** "auto" or "cleared" */
    val archivedBy: String? = null,
    /** The recurring rule that created this todo, if any. */
    val recurringId: String? = null,
    /** Due date (YYYY-MM-DD) for recurring items, shown as "↻ Nov 1". */
    val dueDate: String? = null,
)

@Entity(tableName = "rules")
data class Rule(
    @PrimaryKey val id: String,
    val text: String,
    val category: String,
    /** "week", "month" or "year" */
    val unit: String,
    val every: Int,
    /** 1 = Monday .. 7 = Sunday */
    val weekday: Int?,
    val day: Int?,
    val month: Int?,
    /** YYYY-MM-DD; the first due date is on or after this. */
    val startDate: String,
    /** Last due date that already created a todo; prevents duplicates. */
    val lastDueDate: String?,
    val paused: Boolean,
    val createdAt: Long,
) {
    fun schedule(): Schedule = Schedule(
        unit = RepeatUnit.of(unit),
        every = every.coerceAtLeast(1),
        weekday = weekday,
        day = day,
        month = month,
        startDate = LocalDate.parse(startDate),
    )

    fun lastDue(): LocalDate? = lastDueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
}
