package com.hariv.listo.backup

import com.hariv.listo.data.CATEGORIES
import com.hariv.listo.data.Rule
import com.hariv.listo.data.Todo
import org.json.JSONArray
import org.json.JSONObject

/** The backup file format: { app, version, savedAt, todos: [...], rules: [...] }. */
object BackupJson {
    const val APP = "listo"
    const val VERSION = 1

    data class Data(val savedAt: Long, val todos: List<Todo>, val rules: List<Rule>) {
        val isEmpty get() = todos.isEmpty() && rules.isEmpty()
    }

    fun encode(todos: List<Todo>, rules: List<Rule>, savedAt: Long): String {
        val root = JSONObject()
        root.put("app", APP)
        root.put("version", VERSION)
        root.put("savedAt", savedAt)
        root.put("todos", JSONArray().apply { todos.forEach { put(todoToJson(it)) } })
        root.put("rules", JSONArray().apply { rules.forEach { put(ruleToJson(it)) } })
        return root.toString(2)
    }

    /** Throws IllegalArgumentException with a readable message if the file isn't a valid backup. */
    fun decode(text: String): Data {
        val root = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw IllegalArgumentException("That file isn't a Listo backup.")
        }
        require(root.optString("app") == APP) { "That file isn't a Listo backup." }
        require(root.optInt("version", 0) in 1..VERSION) {
            "This backup was made by a newer version of the app. Update the app first."
        }
        val todos = root.optJSONArray("todos")?.let { arr -> (0 until arr.length()).map { todoFromJson(arr.getJSONObject(it)) } }.orEmpty()
        val rules = root.optJSONArray("rules")?.let { arr -> (0 until arr.length()).map { ruleFromJson(arr.getJSONObject(it)) } }.orEmpty()
        return Data(root.optLong("savedAt", 0L), todos, rules)
    }

    private fun todoToJson(t: Todo) = JSONObject().apply {
        put("id", t.id)
        put("text", t.text)
        put("category", t.category)
        put("done", t.done)
        put("createdAt", t.createdAt)
        put("completedAt", t.completedAt ?: JSONObject.NULL)
        put("archivedAt", t.archivedAt ?: JSONObject.NULL)
        put("archivedBy", t.archivedBy ?: JSONObject.NULL)
        put("recurringId", t.recurringId ?: JSONObject.NULL)
        put("dueDate", t.dueDate ?: JSONObject.NULL)
    }

    private fun ruleToJson(r: Rule) = JSONObject().apply {
        put("id", r.id)
        put("text", r.text)
        put("category", r.category)
        put("unit", r.unit)
        put("every", r.every)
        put("weekday", r.weekday ?: JSONObject.NULL)
        put("day", r.day ?: JSONObject.NULL)
        put("month", r.month ?: JSONObject.NULL)
        put("startDate", r.startDate)
        put("lastDueDate", r.lastDueDate ?: JSONObject.NULL)
        put("paused", r.paused)
        put("createdAt", r.createdAt)
    }

    private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)
    private fun JSONObject.intOrNull(key: String): Int? = if (isNull(key)) null else getInt(key)
    private fun JSONObject.stringOrNull(key: String): String? = if (isNull(key)) null else getString(key)

    private fun category(value: String) = if (value in CATEGORIES) value else CATEGORIES.first()

    private fun todoFromJson(o: JSONObject) = Todo(
        id = o.getString("id"),
        text = o.getString("text"),
        category = category(o.optString("category")),
        done = o.optBoolean("done", false),
        createdAt = o.optLong("createdAt", 0L),
        completedAt = o.longOrNull("completedAt"),
        archivedAt = o.longOrNull("archivedAt"),
        archivedBy = o.stringOrNull("archivedBy"),
        recurringId = o.stringOrNull("recurringId"),
        dueDate = o.stringOrNull("dueDate"),
    )

    private fun ruleFromJson(o: JSONObject) = Rule(
        id = o.getString("id"),
        text = o.getString("text"),
        category = category(o.optString("category")),
        unit = o.optString("unit", "month"),
        every = o.optInt("every", 1).coerceAtLeast(1),
        weekday = o.intOrNull("weekday"),
        day = o.intOrNull("day"),
        month = o.intOrNull("month"),
        startDate = o.getString("startDate"),
        lastDueDate = o.stringOrNull("lastDueDate"),
        paused = o.optBoolean("paused", false),
        createdAt = o.optLong("createdAt", 0L),
    )
}
