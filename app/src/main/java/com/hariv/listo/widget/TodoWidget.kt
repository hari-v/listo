package com.hariv.listo.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckboxDefaults
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
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
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.hariv.listo.App
import com.hariv.listo.data.Todo
import com.hariv.listo.ui.Fmt
import com.hariv.listo.ui.MainActivity
import com.hariv.listo.ui.QuickAddActivity
import com.hariv.listo.ui.theme.CategoryColorsDark
import com.hariv.listo.ui.theme.CategoryColorsLight
import com.hariv.listo.ui.theme.DarkScheme
import com.hariv.listo.ui.theme.LightScheme

/** The home-screen widget, in the app's slate & stone palette. Stretch it to fill a whole page. */
class TodoWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = App.get(context).repo
        val initial = repo.activeTodosNow()
        provideContent {
            val todos by repo.activeTodos.collectAsState(initial = initial)
            val categories by repo.categories.list.collectAsState()
            val saved = currentState(FilterKey) ?: ALL
            val filter = if (saved == ALL || saved in categories) saved else ALL
            GlanceTheme(colors = WidgetColors) {
                WidgetContent(todos, categories, filter)
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

private fun categoryColor(categories: List<String>, name: String): ColorProvider {
    val i = categories.indexOf(name)
    if (i < 0) return DayNightColor(LightScheme.outline, DarkScheme.outline)
    return DayNightColor(CategoryColorsLight[i % CategoryColorsLight.size], CategoryColorsDark[i % CategoryColorsDark.size])
}

@Composable
private fun WidgetContent(todos: List<Todo>, categories: List<String>, filter: String) {
    val colors = GlanceTheme.colors
    val openCount = todos.count { !it.done }
    val visible = if (filter == TodoWidget.ALL) todos else todos.filter { it.category == filter }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.background)
            .cornerRadius(28.dp)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        // Header: tap the title to open the app; + opens the quick-add sheet.
        Row(modifier = GlanceModifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>())) {
                Text("Listo", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium, color = colors.onSurface))
                Text(
                    if (openCount == 1) "1 open" else "$openCount open",
                    style = TextStyle(fontSize = 13.sp, color = colors.onSurfaceVariant),
                )
            }
            Box(
                modifier = GlanceModifier
                    .size(44.dp)
                    .background(colors.primary)
                    .cornerRadius(22.dp)
                    .clickable(actionStartActivity<QuickAddActivity>()),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", style = TextStyle(fontSize = 24.sp, color = colors.onPrimary, textAlign = TextAlign.Center))
            }
        }

        Spacer(GlanceModifier.height(12.dp))

        // Filter chips
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Chip("All", TodoWidget.ALL, filter, null)
            categories.forEach { Chip(it, it, filter, categoryColor(categories, it)) }
        }

        Spacer(GlanceModifier.height(10.dp))

        // The list sits on a paper-white card.
        Box(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxWidth()
                .background(colors.surface)
                .cornerRadius(18.dp),
            contentAlignment = if (visible.isEmpty()) Alignment.Center else Alignment.TopStart,
        ) {
            if (visible.isEmpty()) {
                Text(
                    if (todos.isEmpty()) "Nothing to do" else "Nothing in $filter",
                    style = TextStyle(fontSize = 14.sp, color = colors.onSurfaceVariant),
                )
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize().padding(vertical = 4.dp)) {
                    items(visible, itemId = { it.id.hashCode().toLong() }) { todo ->
                        TodoRow(todo, categories)
                    }
                }
            }
        }

        if (todos.any { it.done }) {
            Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.End) {
                Text(
                    "Clear completed",
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.onSurfaceVariant),
                    modifier = GlanceModifier
                        .cornerRadius(14.dp)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .clickable(actionRunCallback<ClearCompletedAction>()),
                )
            }
        }
    }
}

@Composable
private fun Chip(label: String, value: String, selected: String, dot: ColorProvider?) {
    val colors = GlanceTheme.colors
    val isSelected = value == selected
    Box(modifier = GlanceModifier.padding(end = 6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier
                .background(if (isSelected) colors.primary else colors.surface)
                .cornerRadius(16.dp)
                .padding(horizontal = 12.dp, vertical = 7.dp)
                .clickable(actionRunCallback<SetFilterAction>(actionParametersOf(FilterParam to value))),
        ) {
            if (dot != null) {
                Box(modifier = GlanceModifier.size(7.dp).background(dot).cornerRadius(4.dp)) {}
                Spacer(GlanceModifier.width(6.dp))
            }
            Text(
                label,
                maxLines = 1,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) colors.onPrimary else colors.onSurface,
                ),
            )
        }
    }
}

@Composable
private fun TodoRow(todo: Todo, categories: List<String>) {
    val colors = GlanceTheme.colors
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckBox(
            checked = todo.done,
            onCheckedChange = actionRunCallback<ToggleAction>(actionParametersOf(TodoIdParam to todo.id)),
            colors = CheckboxDefaults.colors(checkedColor = colors.primary, uncheckedColor = colors.outline),
        )
        Column(modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>())) {
            Text(
                todo.text,
                maxLines = 2,
                style = TextStyle(
                    fontSize = 15.sp,
                    color = if (todo.done) colors.onSurfaceVariant else colors.onSurface,
                    textDecoration = if (todo.done) TextDecoration.LineThrough else null,
                ),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = GlanceModifier.size(6.dp).background(categoryColor(categories, todo.category)).cornerRadius(3.dp)) {}
                Spacer(GlanceModifier.width(5.dp))
                Text(todo.category, style = TextStyle(fontSize = 12.sp, color = colors.onSurfaceVariant))
                todo.dueDate?.let { due ->
                    Spacer(GlanceModifier.width(10.dp))
                    Text("↻ Due ${Fmt.shortDate(due)}", style = TextStyle(fontSize = 12.sp, color = colors.tertiary))
                }
            }
        }
    }
}
