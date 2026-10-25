package com.hariv.listo.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.hariv.listo.App
import com.hariv.listo.data.CATEGORIES
import com.hariv.listo.data.Todo
import com.hariv.listo.ui.Fmt
import com.hariv.listo.ui.MainActivity
import com.hariv.listo.ui.QuickAddActivity

/** The home-screen widget. Stretch it to fill a whole page. */
class TodoWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = App.get(context).repo
        val initial = repo.activeTodosNow()
        provideContent {
            val todos by repo.activeTodos.collectAsState(initial = initial)
            val filter = currentState(FilterKey) ?: ALL
            GlanceTheme {
                WidgetContent(todos, filter)
            }
        }
    }

    companion object {
        val FilterKey = stringPreferencesKey("filter")
        const val ALL = "all"
    }
}

class TodoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodoWidget()
}

@Composable
private fun WidgetContent(todos: List<Todo>, filter: String) {
    val openCount = todos.count { !it.done }
    val visible = if (filter == TodoWidget.ALL) todos else todos.filter { it.category == filter }
    val colors = GlanceTheme.colors

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(12.dp)
    ) {
        // Header: tap the title to open the app; + opens the quick-add sheet.
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>())) {
                Text(
                    text = "Listo",
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.onSurface),
                )
                Text(
                    text = "$openCount left",
                    style = TextStyle(fontSize = 12.sp, color = colors.onSurfaceVariant),
                )
            }
            Button(text = "+ Add", onClick = actionStartActivity<QuickAddActivity>())
        }

        Spacer(GlanceModifier.height(8.dp))

        // Filter chips
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            FilterChip("All", TodoWidget.ALL, filter)
            CATEGORIES.forEach { FilterChip(it, it, filter) }
        }

        Spacer(GlanceModifier.height(8.dp))

        if (visible.isEmpty()) {
            Box(
                modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Nothing to do. Nice.",
                    style = TextStyle(fontSize = 14.sp, color = colors.onSurfaceVariant),
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                items(visible, itemId = { it.id.hashCode().toLong() }) { todo ->
                    TodoRow(todo)
                }
            }
        }

        if (todos.any { it.done }) {
            Spacer(GlanceModifier.height(6.dp))
            Button(
                text = "Clear completed",
                onClick = actionRunCallback<ClearCompletedAction>(),
                modifier = GlanceModifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colors.secondaryContainer,
                    contentColor = colors.onSecondaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun FilterChip(label: String, value: String, selected: String) {
    val colors = GlanceTheme.colors
    val isSelected = value == selected
    Box(modifier = GlanceModifier.padding(end = 6.dp)) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) colors.onPrimary else colors.onSecondaryContainer,
            ),
            modifier = GlanceModifier
                .background(if (isSelected) colors.primary else colors.secondaryContainer)
                .cornerRadius(16.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clickable(actionRunCallback<SetFilterAction>(actionParametersOf(FilterParam to value))),
        )
    }
}

@Composable
private fun TodoRow(todo: Todo) {
    val colors = GlanceTheme.colors
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckBox(
            checked = todo.done,
            onCheckedChange = actionRunCallback<ToggleAction>(actionParametersOf(TodoIdParam to todo.id)),
        )
        Column(modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>())) {
            Text(
                text = todo.text,
                maxLines = 2,
                style = TextStyle(
                    fontSize = 15.sp,
                    color = if (todo.done) colors.onSurfaceVariant else colors.onSurface,
                    textDecoration = if (todo.done) TextDecoration.LineThrough else null,
                ),
            )
            todo.dueDate?.let { due ->
                Text(
                    text = "↻ ${Fmt.shortDate(due)}",
                    style = TextStyle(fontSize = 12.sp, color = colors.onSurfaceVariant),
                )
            }
        }
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = todo.category,
            style = TextStyle(fontSize = 12.sp, color = colors.onSecondaryContainer),
            modifier = GlanceModifier
                .background(colors.secondaryContainer)
                .cornerRadius(10.dp)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
