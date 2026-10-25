package com.hariv.simpletodo.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.hariv.simpletodo.App

val TodoIdParam = ActionParameters.Key<String>("todoId")
val FilterParam = ActionParameters.Key<String>("filter")

/** Tick / untick from the widget. */
class ToggleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[TodoIdParam] ?: return
        val repo = App.get(context).repo
        repo.toggle(id)
        repo.flushBackup()
    }
}

/** "Clear completed" from the widget: done items move to the Archive. */
class ClearCompletedAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val repo = App.get(context).repo
        repo.clearCompleted()
        repo.flushBackup()
    }
}

/** Filter chips on the widget; each widget remembers its own filter. */
class SetFilterAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val value = parameters[FilterParam] ?: TodoWidget.ALL
        updateAppWidgetState(context, glanceId) { prefs -> prefs[TodoWidget.FilterKey] = value }
        TodoWidget().update(context, glanceId)
    }
}
