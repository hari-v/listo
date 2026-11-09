package com.hariv.listo.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckboxDefaults
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider as DayNightColor
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.hariv.listo.App
import com.hariv.listo.R
import com.hariv.listo.data.Todo
import com.hariv.listo.ui.DueState
import com.hariv.listo.ui.MainActivity
import com.hariv.listo.ui.QuickAddActivity
import com.hariv.listo.ui.dueState
import com.hariv.listo.ui.dueText
import com.hariv.listo.ui.theme.CategoryColorsDark
import com.hariv.listo.ui.theme.CategoryColorsLight
import com.hariv.listo.ui.theme.CategoryInkLight
import com.hariv.listo.ui.theme.DarkScheme
import com.hariv.listo.ui.theme.LightScheme
import java.time.LocalDate

/** The home-screen widget, in the app's palette. Stretch it to fill a whole page. */
class TodoWidget : GlanceAppWidget() {

    // Lay out for the exact size each launcher gives us (Pixel and Samsung cells differ).
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = App.get(context).repo
        val initial = repo.activeTodosNow()
        provideContent {
            val todos by repo.activeTodos.collectAsState(initial = initial)
            val categories by repo.categories.list.collectAsState()
            val saved = currentState(FilterKey) ?: ALL
            val filter = if (saved == ALL || saved in categories) saved else ALL
            GlanceTheme(colors = WidgetColors) {
                WidgetContent(todos, categories, filter, LocalDate.now())
            }
        }
    }

    companion object {
        val FilterKey = stringPreferencesKey("filter")
        const val ALL = "all"
        private val WidgetColors = ColorProviders(light = LightScheme, dark = DarkScheme)
    }
}

class TodoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodoWidget()
}

/** Category colours, light and dark: bright for rings and dots, deeper for text and filled chips. */
private class WidgetTone(val strong: ColorProvider, val text: ColorProvider, val fill: ColorProvider, val onFill: ColorProvider)

private fun toneFor(categories: List<String>, name: String): WidgetTone {
    val i = categories.indexOf(name)
    if (i < 0) {
        val neutral = DayNightColor(LightScheme.outline, DarkScheme.outline)
        return WidgetTone(neutral, DayNightColor(LightScheme.onSurfaceVariant, DarkScheme.onSurfaceVariant), neutral, DayNightColor(LightScheme.surface, DarkScheme.surface))
    }
    val n = CategoryColorsLight.size
    val strong = DayNightColor(CategoryColorsLight[i % n], CategoryColorsDark[i % n])
    val ink = DayNightColor(CategoryInkLight[i % n], CategoryColorsDark[i % n])
    return WidgetTone(strong, ink, ink, DayNightColor(LightScheme.surface, DarkScheme.background))
}

private val NeutralChip = WidgetTone(
    strong = DayNightColor(LightScheme.onSurfaceVariant, DarkScheme.onSurfaceVariant),
    text = DayNightColor(LightScheme.onSurface, DarkScheme.onSurface),
    fill = DayNightColor(LightScheme.onSurface, DarkScheme.onSurface),
    onFill = DayNightColor(LightScheme.surface, DarkScheme.surface),
)

private val ChipBackground = DayNightColor(LightScheme.surfaceContainerHigh, DarkScheme.surfaceContainerHigh)
private val Hairline = DayNightColor(LightScheme.outlineVariant, DarkScheme.outlineVariant)

/** Rough width of a chip at 14sp, used to show only the chips that fit on one line. */
private fun chipWidth(label: String, hasDot: Boolean): Dp = (24f + label.length * 8.4f + (if (hasDot) 13f else 0f) + 6f).dp

@Composable
private fun WidgetContent(todos: List<Todo>, categories: List<String>, filter: String, today: LocalDate) {
    val colors = GlanceTheme.colors
    val size = LocalSize.current
    val openTasks = todos.filter { !it.done }
    val visible = if (filter == TodoWidget.ALL) todos else todos.filter { it.category == filter }

    // Chips that fit between the app mark and the + button.
    var room = size.width - 20.dp - 32.dp - 48.dp
    val chips = buildList {
        val all = "All ${openTasks.size}"
        add(Triple(TodoWidget.ALL, all, false))
        room -= chipWidth(all, false)
        for (cat in categories) {
            val label = "$cat ${openTasks.count { it.category == cat }}"
            val w = chipWidth(label, true)
            if (w > room) break
            add(Triple(cat, label, true))
            room -= w
        }
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(colors.surface)
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 4.dp),
    ) {
        // One compact header row: app mark (opens Listo), filter chips, add button.
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_mark),
                contentDescription = "Open Listo",
                colorFilter = ColorFilter.tint(colors.primary),
                modifier = GlanceModifier.size(26.dp).clickable(actionStartActivity<MainActivity>()),
            )
            Spacer(GlanceModifier.width(6.dp))
            chips.forEach { (value, label, hasDot) ->
                Chip(label, value, filter, if (value == TodoWidget.ALL) NeutralChip else toneFor(categories, value), hasDot)
            }
            Spacer(GlanceModifier.defaultWeight())
            Box(
                contentAlignment = Alignment.Center,
                modifier = GlanceModifier
                    .size(40.dp)
                    .background(colors.primary)
                    .cornerRadius(20.dp)
                    .clickable(actionStartActivity<QuickAddActivity>()),
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_add),
                    contentDescription = "Add a task",
                    colorFilter = ColorFilter.tint(colors.onPrimary),
                    modifier = GlanceModifier.size(20.dp),
                )
            }
        }

        Spacer(GlanceModifier.height(4.dp))

        if (visible.isEmpty()) {
            Column(
                modifier = GlanceModifier.defaultWeight().fillMaxWidth().clickable(actionStartActivity<QuickAddActivity>()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (todos.isEmpty()) "Nothing to do" else "Nothing in $filter",
                    style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.onSurface),
                )
                Text("Tap + to add a task", style = TextStyle(fontSize = 14.sp, color = colors.onSurfaceVariant))
            }
        } else {
            LazyColumn(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                items(visible, itemId = { it.id.hashCode().toLong() }) { todo ->
                    TodoRow(todo, categories, today)
                }
            }
        }

        if (todos.any { it.done }) {
            Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                Text(
                    "Clear completed",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.primary),
                    modifier = GlanceModifier
                        .cornerRadius(14.dp)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .clickable(actionRunCallback<ClearCompletedAction>()),
                )
            }
        }
    }
}

@Composable
private fun Chip(label: String, value: String, selected: String, tone: WidgetTone, hasDot: Boolean) {
    val colors = GlanceTheme.colors
    val isSelected = value == selected
    Box(modifier = GlanceModifier.padding(end = 6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier
                .background(if (isSelected) tone.fill else ChipBackground)
                .cornerRadius(16.dp)
                .padding(horizontal = 12.dp, vertical = 7.dp)
                .clickable(actionRunCallback<SetFilterAction>(actionParametersOf(FilterParam to value))),
        ) {
            if (hasDot) {
                Box(modifier = GlanceModifier.size(7.dp).background(if (isSelected) tone.onFill else tone.strong).cornerRadius(4.dp)) {}
                Spacer(GlanceModifier.width(6.dp))
            }
            Text(
                label,
                maxLines = 1,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) tone.onFill else colors.onSurface,
                ),
            )
        }
    }
}

@Composable
private fun TodoRow(todo: Todo, categories: List<String>, today: LocalDate) {
    val colors = GlanceTheme.colors
    val tone = toneFor(categories, todo.category)
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(end = 6.dp, top = 1.dp, bottom = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CheckBox(
                checked = todo.done,
                onCheckedChange = actionRunCallback<ToggleAction>(actionParametersOf(TodoIdParam to todo.id)),
                colors = CheckboxDefaults.colors(checkedColor = tone.strong, uncheckedColor = tone.strong),
            )
            Column(modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>())) {
                Text(
                    todo.text,
                    maxLines = 2,
                    style = TextStyle(
                        fontSize = 17.sp,
                        color = if (todo.done) colors.onSurfaceVariant else colors.onSurface,
                        textDecoration = if (todo.done) TextDecoration.LineThrough else null,
                    ),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(todo.category, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = tone.text))
                    todo.dueDate?.let { due ->
                        val dueColor = when {
                            todo.done -> colors.onSurfaceVariant
                            else -> when (dueState(due, today)) {
                                DueState.Overdue, DueState.Today -> colors.error
                                DueState.Tomorrow -> colors.tertiary
                                DueState.Later -> colors.onSurfaceVariant
                            }
                        }
                        Spacer(GlanceModifier.width(10.dp))
                        Text("↻ ${dueText(due, today)}", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = dueColor))
                    }
                }
            }
        }
        // Hairline between rows, aligned with the text.
        Box(modifier = GlanceModifier.fillMaxWidth().padding(start = 48.dp)) {
            Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(Hairline)) {}
        }
    }
}
