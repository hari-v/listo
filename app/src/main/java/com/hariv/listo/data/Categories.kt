package com.hariv.listo.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The user's own category list (editable in Settings), kept in order. */
class CategoryStore(private val prefs: Prefs) {

    private val _list = MutableStateFlow(load())
    val list: StateFlow<List<String>> = _list
    val current: List<String> get() = _list.value

    private fun load(): List<String> {
        prefs.categories?.takeIf { it.isNotEmpty() }?.let { return it }
        // First run of this version: keep the old fixed list for existing users.
        val existing = prefs.isExistingInstall
        val initial = if (existing) LEGACY else DEFAULTS
        prefs.categories = initial
        if (existing) prefs.categoriesReviewed = true
        return initial
    }

    fun set(list: List<String>) {
        val clean = list.map { clean(it) }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }.take(MAX_COUNT)
        if (clean.isEmpty()) return
        prefs.categories = clean
        _list.value = clean
    }

    /** The category to preselect when adding a task. */
    fun defaultCategory(): String = prefs.lastCategory?.takeIf { it in current } ?: current.first()

    /** Null if the name is fine, otherwise what's wrong with it. */
    fun problemWith(name: String, renaming: String? = null): String? {
        val n = clean(name)
        return when {
            n.isEmpty() -> "Enter a name."
            current.any { it.equals(n, ignoreCase = true) && !it.equals(renaming, ignoreCase = true) } -> "\"$n\" already exists."
            renaming == null && current.size >= MAX_COUNT -> "You can have up to $MAX_COUNT categories."
            else -> null
        }
    }

    companion object {
        const val MAX_COUNT = 6
        const val MAX_LENGTH = 12
        val LEGACY = listOf("my", "bl", "pw")
        val DEFAULTS = listOf("Personal", "Work")

        fun clean(name: String): String = name.trim().replace(Regex("\\s+"), " ").take(MAX_LENGTH)

        /** Categories for a restored backup made before categories were saved in it. */
        fun derive(used: List<String>, fallback: List<String>): List<String> {
            val distinct = used.filter { it.isNotBlank() }.distinct()
            return when {
                distinct.isEmpty() -> fallback
                distinct.all { it in LEGACY } -> LEGACY
                else -> distinct.take(MAX_COUNT)
            }
        }
    }
}
