package com.hariv.simpletodo.data

import android.content.Context

/** Small app settings kept in SharedPreferences. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var backupTreeUri: String?
        get() = sp.getString("backupTreeUri", null)
        set(value) = sp.edit().putString("backupTreeUri", value).apply()

    var lastBackupAt: Long
        get() = sp.getLong("lastBackupAt", 0L)
        set(value) = sp.edit().putLong("lastBackupAt", value).apply()

    var backupPaused: Boolean
        get() = sp.getBoolean("backupPaused", false)
        set(value) = sp.edit().putBoolean("backupPaused", value).apply()

    var lastCategory: String
        get() = sp.getString("lastCategory", null)?.takeIf { it in CATEGORIES } ?: CATEGORIES.first()
        set(value) = sp.edit().putString("lastCategory", value).apply()

    var setupHidden: Boolean
        get() = sp.getBoolean("setupHidden", false)
        set(value) = sp.edit().putBoolean("setupHidden", value).apply()
}
