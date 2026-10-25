package com.hariv.listo.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.hariv.listo.data.CATEGORIES
import com.hariv.listo.widget.TodoWidgetReceiver
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    MaterialTheme(colorScheme = scheme, content = content)
}

/** my / bl / pw picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPicker(selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CATEGORIES.forEach { cat ->
            FilterChip(selected = cat == selected, onClick = { onSelect(cat) }, label = { Text(cat) })
        }
    }
}

object Fmt {
    private val dateTime = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.ENGLISH)
    private val fullDateTime = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)
    private val time = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
    private val shortDate = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
    private val longDate = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH)

    private fun zoned(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())

    /** "18 Oct 09:15" */
    fun dateTime(ms: Long): String = zoned(ms).format(dateTime)

    /** "25 Oct 2026, 10:42" */
    fun fullDateTime(ms: Long): String = zoned(ms).format(fullDateTime)

    /** "10:42" today, otherwise "18 Oct 09:15" */
    fun whenShort(ms: Long): String {
        val z = zoned(ms)
        return if (z.toLocalDate() == LocalDate.now()) z.format(time) else z.format(dateTime)
    }

    /** "Nov 1" */
    fun shortDate(date: LocalDate): String = date.format(shortDate)
    fun shortDate(iso: String): String = runCatching { shortDate(LocalDate.parse(iso)) }.getOrDefault(iso)

    /** "Sun 25 Oct 2026" */
    fun longDate(date: LocalDate): String = date.format(longDate)
}

object Device {
    fun isBatteryUnrestricted(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    /** Shows Android's "let this app run in the background" prompt (Samsung: battery = Unrestricted). */
    fun requestBatteryUnrestricted(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${context.packageName}".toUri())
        runCatching { context.startActivity(intent) }.onFailure {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
            )
        }
    }

    private fun widgetComponent(context: Context) = ComponentName(context, TodoWidgetReceiver::class.java)

    fun hasWidget(context: Context): Boolean =
        AppWidgetManager.getInstance(context).getAppWidgetIds(widgetComponent(context)).isNotEmpty()

    /** Asks the launcher to place the widget. Returns false if the launcher can't. */
    fun requestPinWidget(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false
        return manager.requestPinAppWidget(widgetComponent(context), null, null)
    }
}
