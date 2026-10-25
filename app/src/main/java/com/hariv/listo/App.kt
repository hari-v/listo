package com.hariv.listo

import android.app.Application
import android.content.Context
import com.hariv.listo.backup.BackupManager
import com.hariv.listo.data.AppDatabase
import com.hariv.listo.data.Prefs
import com.hariv.listo.data.Repository
import com.hariv.listo.work.DailyWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class App : Application() {
    /** Lives as long as the app; writes launched here are never cancelled by a screen closing. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val prefs by lazy { Prefs(this) }
    val db by lazy { AppDatabase.build(this) }
    val backup by lazy { BackupManager(this, prefs) }
    val repo by lazy { Repository(this, db, prefs, backup, appScope) }

    override fun onCreate() {
        super.onCreate()
        DailyWorker.schedule(this)
    }

    companion object {
        fun get(context: Context): App = context.applicationContext as App
    }
}
