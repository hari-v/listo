package com.hariv.simpletodo.update

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.hariv.simpletodo.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Checks this app's GitHub Releases for a newer APK and hands it to Android's installer. */
object Updater {

    data class Release(val version: String, val apkUrl: String)

    val currentVersion: String get() = BuildConfig.VERSION_NAME

    /** Latest release, or null if the repo has no releases yet. */
    suspend fun latestRelease(): Release? = withContext(Dispatchers.IO) {
        val url = URL("https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        try {
            when (conn.responseCode) {
                404 -> return@withContext null
                200 -> Unit
                else -> throw IOException("GitHub answered ${conn.responseCode}")
            }
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val version = json.getString("tag_name").removePrefix("v")
            val assets = json.getJSONArray("assets")
            val apk = (0 until assets.length())
                .map { assets.getJSONObject(it) }
                .firstOrNull { it.getString("name").endsWith(".apk") }
                ?: return@withContext null
            Release(version, apk.getString("browser_download_url"))
        } finally {
            conn.disconnect()
        }
    }

    fun isNewer(remote: String, local: String): Boolean {
        val r = parts(remote)
        val l = parts(local)
        for (i in 0 until 3) {
            if (r[i] != l[i]) return r[i] > l[i]
        }
        return false
    }

    private fun parts(v: String): List<Int> =
        v.split(".").map { p -> p.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 } + listOf(0, 0, 0)

    suspend fun download(context: Context, release: Release): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "simple-todo-${release.version}.apk")
        var url = URL(release.apkUrl)
        repeat(5) {
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 60_000
                instanceFollowRedirects = false
            }
            try {
                when (conn.responseCode) {
                    in 300..399 -> {
                        url = URL(url, conn.getHeaderField("Location") ?: throw IOException("Bad redirect"))
                    }
                    200 -> {
                        conn.inputStream.use { input -> file.outputStream().use { input.copyTo(it) } }
                        return@withContext file
                    }
                    else -> throw IOException("Download failed (${conn.responseCode})")
                }
            } finally {
                conn.disconnect()
            }
        }
        throw IOException("Too many redirects")
    }

    /** True if the app may install updates; otherwise opens the setting that allows it. */
    fun ensureInstallAllowed(context: Context): Boolean {
        if (context.packageManager.canRequestPackageInstalls()) return true
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return false
    }

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
