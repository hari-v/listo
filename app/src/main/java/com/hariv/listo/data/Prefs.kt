package com.hariv.listo.data

import android.content.Context
import org.json.JSONArray

/** Small app settings kept in SharedPreferences. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** True when this phone already used the app before categories became editable (1.0.x). */
    val isExistingInstall: Boolean
        get() = sp.contains("backupTreeUri") || sp.contains("lastCategory") || sp.contains("lastBackupAt")

    var backupTreeUri: String?
        get() = sp.getString("backupTreeUri", null)
        set(value) = sp.edit().putString("backupTreeUri", value).apply()

    var lastBackupAt: Long
        get() = sp.getLong("lastBackupAt", 0L)
        set(value) = sp.edit().putLong("lastBackupAt", value).apply()

    var backupPaused: Boolean
        get() = sp.getBoolean("backupPaused", false)
        set(value) = sp.edit().putBoolean("backupPaused", value).apply()

    /** The category used for the last added task (may no longer exist; callers check). */
    var lastCategory: String?
        get() = sp.getString("lastCategory", null)
        set(value) = sp.edit().putString("lastCategory", value).apply()

    var setupHidden: Boolean
        get() = sp.getBoolean("setupHidden", false)
        set(value) = sp.edit().putBoolean("setupHidden", value).apply()

    var categoriesReviewed: Boolean
        get() = sp.getBoolean("categoriesReviewed", false)
        set(value) = sp.edit().putBoolean("categoriesReviewed", value).apply()

    var categories: List<String>?
        get() = sp.getString("categories", null)?.let { raw ->
            runCatching {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { arr.getString(it) }
            }.getOrNull()
        }
        set(value) = sp.edit().putString("categories", value?.let { JSONArray(it).toString() }).apply()
}
