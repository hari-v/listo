package com.hariv.listo.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.hariv.listo.ui.theme.ListoIcons
import com.hariv.listo.ui.theme.categoryColor
import com.hariv.listo.widget.TodoWidgetReceiver
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------- Small shared building blocks ----------

/** Round check circle with a 48dp touch target. */
@Composable
fun RoundCheck(checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val fill by animateColorAsState(if (checked) scheme.primary else Color.Transparent, label = "check-fill")
    val ring by animateColorAsState(if (checked) scheme.primary else scheme.outline, label = "check-ring")
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() }),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(fill)
                .border(1.5.dp, ring, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(ListoIcons.Check, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(13.dp))
        }
    }
}

@Composable
fun CategoryDot(color: Color, size: Dp = 8.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

/** Category name with its colour dot. Tapping opens [onPick] choices when given. */
@Composable
fun CategoryTag(categories: List<String>, name: String, onPick: ((String) -> Unit)? = null) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .then(if (onPick != null) Modifier.clickable { expanded = true } else Modifier)
                .padding(vertical = 2.dp, horizontal = 2.dp),
        ) {
            CategoryDot(categoryColor(categories, name), 7.dp)
            Spacer(Modifier.width(6.dp))
            Text(name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onPick != null) {
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                categories.filter { it != name }.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text("Move to $cat") },
                        leadingIcon = { CategoryDot(categoryColor(categories, cat)) },
                        onClick = {
                            expanded = false
                            onPick(cat)
                        },
                    )
                }
            }
        }
    }
}

/** Pill showing the selected category; tap to choose another. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelector(categories: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            onClick = { expanded = true },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(44.dp).padding(start = 14.dp, end = 10.dp),
            ) {
                CategoryDot(categoryColor(categories, selected))
                Spacer(Modifier.width(8.dp))
                Text(selected, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(4.dp))
                Icon(ListoIcons.ChevronDown, contentDescription = "Choose category", modifier = Modifier.size(16.dp))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat) },
                    leadingIcon = { CategoryDot(categoryColor(categories, cat)) },
                    trailingIcon = { if (cat == selected) Icon(ListoIcons.Check, null, modifier = Modifier.size(16.dp)) },
                    onClick = {
                        expanded = false
                        onSelect(cat)
                    },
                )
            }
        }
    }
}

/** A row of choices where exactly one is selected (filters, category pickers, repeat units). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoiceRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (String) -> String = { it },
    count: ((String) -> Int)? = null,
    dotColor: @Composable (String) -> Color? = { null },
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()).padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val scheme = MaterialTheme.colorScheme
            Surface(
                onClick = { onSelect(option) },
                shape = CircleShape,
                color = if (isSelected) scheme.primary else Color.Transparent,
                contentColor = if (isSelected) scheme.onPrimary else scheme.onSurface,
                border = if (isSelected) null else BorderStroke(1.dp, scheme.outlineVariant),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(36.dp).padding(horizontal = 14.dp),
                ) {
                    dotColor(option)?.let {
                        CategoryDot(it)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(label(option), style = MaterialTheme.typography.labelLarge)
                    count?.let { c ->
                        Spacer(Modifier.width(6.dp))
                        Text(
                            c(option).toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) scheme.onPrimary.copy(alpha = 0.7f) else scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Page title for the tab screens and full-page screens. */
@Composable
fun PageHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth().padding(start = if (onBack != null) 4.dp else 20.dp, end = 8.dp, top = 8.dp),
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.padding(top = 2.dp)) {
                Icon(ListoIcons.Back, contentDescription = "Back")
            }
        }
        Column(Modifier.weight(1f).padding(top = 6.dp, start = if (onBack != null) 4.dp else 0.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            subtitle?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        actions()
    }
}

/** Small heading above a group of rows. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(start = 4.dp, top = 20.dp, bottom = 8.dp).height(32.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        trailing()
    }
}

/** Shape for row [index] of [count] in a grouped list: rounded at the group's outer corners only. */
fun groupShape(index: Int, count: Int, radius: Dp = 16.dp): Shape = when {
    count == 1 -> RoundedCornerShape(radius)
    index == 0 -> RoundedCornerShape(topStart = radius, topEnd = radius)
    index == count - 1 -> RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
    else -> RoundedCornerShape(0.dp)
}

/** One row of a grouped list, with a hairline divider under every row but the last. */
@Composable
fun GroupRow(index: Int, count: Int, modifier: Modifier = Modifier, dividerInset: Dp = 56.dp, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(groupShape(index, count))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        content()
        if (index < count - 1) {
            HorizontalDivider(modifier = Modifier.padding(start = dividerInset), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        }
    }
}

/** Settings-style row: icon, title, optional value text, and a trailing control. */
@Composable
fun InfoRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            value?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        trailing()
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

/** Bottom navigation between the three main tabs. */
@Composable
fun ListoNavBar(current: Screen, go: (Screen) -> Unit) {
    Column {
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            val colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            listOf(
                Triple(Screen.Tasks, "Tasks", ListoIcons.Tasks),
                Triple(Screen.Recurring, "Recurring", ListoIcons.Repeat),
                Triple(Screen.Archive, "Archive", ListoIcons.Archive),
            ).forEach { (screen, label, icon) ->
                NavigationBarItem(
                    selected = current == screen,
                    onClick = { go(screen) },
                    icon = { Icon(icon, contentDescription = null) },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    colors = colors,
                )
            }
        }
    }
}

// ---------- Formatting ----------

object Fmt {
    private val dateTime = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH)
    private val fullDateTime = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)
    private val time = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
    private val shortDate = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val longDate = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH)
    private val dayName = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)
    private val dateLine = DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH)
    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

    private fun zoned(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())

    /** "18 Oct, 09:15" */
    fun dateTime(ms: Long): String = zoned(ms).format(dateTime)

    /** "18 Oct" */
    fun dayMonth(ms: Long): String = zoned(ms).format(dayMonth)

    /** "25 Oct 2026, 10:42" */
    fun fullDateTime(ms: Long): String = zoned(ms).format(fullDateTime)

    /** "10:42" today, otherwise "18 Oct, 09:15" */
    fun whenShort(ms: Long): String {
        val z = zoned(ms)
        return if (z.toLocalDate() == LocalDate.now()) "today at ${z.format(time)}" else "on ${z.format(dateTime)}"
    }

    /** "Nov 1" */
    fun shortDate(date: LocalDate): String = date.format(shortDate)
    fun shortDate(iso: String): String = runCatching { shortDate(LocalDate.parse(iso)) }.getOrDefault(iso)

    /** "Sun 25 Oct 2026" */
    fun longDate(date: LocalDate): String = date.format(longDate)

    /** "Sunday" */
    fun dayName(date: LocalDate): String = date.format(dayName)

    /** "25 October" */
    fun dateLine(date: LocalDate): String = date.format(dateLine)

    /** "October 2026" */
    fun monthYear(ms: Long): String = zoned(ms).format(monthYear)
}

// ---------- Phone settings helpers ----------

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
