package com.hariv.listo.screens

import android.graphics.Color
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.appwidget.compose
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.hariv.listo.App
import com.hariv.listo.data.Rule
import com.hariv.listo.data.Todo
import com.hariv.listo.ui.ArchiveScreen
import com.hariv.listo.ui.HelpScreen
import com.hariv.listo.ui.QuickAddSheet
import com.hariv.listo.ui.RecurringScreen
import com.hariv.listo.ui.RuleEditorScreen
import com.hariv.listo.ui.SettingsScreen
import com.hariv.listo.ui.TasksScreen
import com.hariv.listo.ui.theme.ListoTheme
import com.hariv.listo.widget.TodoWidget
import kotlinx.coroutines.runBlocking
import org.junit.Rule as JUnitRule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

// Screen sizes in dp: Galaxy S24 at its default display size, and Pixel 10.
private const val S24 = "w384dp-h832dp-xxhdpi"
private const val S24_DARK = "w384dp-h832dp-night-xxhdpi"
private const val PIXEL10 = "w411dp-h923dp-xxhdpi"
private const val PIXEL10_DARK = "w411dp-h923dp-night-xxhdpi"

/**
 * Renders every screen and the widget to PNGs in app/build/screenshots, for design review.
 * Runs only in CI (debug unit tests); nothing here ships in the app.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ScreenshotTest {

    @get:JUnitRule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app: App get() = ApplicationProvider.getApplicationContext()

    // ---------- Sample data ----------

    private fun seed(categories: List<String> = listOf("my", "bl", "pw"), firstRun: Boolean = false) = runBlocking {
        val prefs = app.prefs
        if (!firstRun) {
            prefs.backupTreeUri = "content://com.android.externalstorage.documents/tree/primary%3ADocuments"
            prefs.lastBackupAt = System.currentTimeMillis() - 4 * 60_000
            prefs.setupHidden = true
            prefs.categoriesReviewed = true
        }
        prefs.categories = categories
        val (c0, c1, c2) = Triple(categories[0], categories[1], categories.getOrElse(2) { categories[0] })
        val now = System.currentTimeMillis()
        val hour = 3_600_000L
        val day = 24 * hour
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        app.db.rules().insertAll(
            listOf(
                Rule("r1", "Pay electricity bill", c1, "month", 1, null, tomorrow.dayOfMonth, null, today.minusMonths(6).toString(), tomorrow.toString(), false, now - 90 * day),
                Rule("r2", "Renew car insurance", c1, "year", 1, null, today.dayOfMonth, today.monthValue, today.minusYears(1).toString(), today.toString(), false, now - 80 * day),
                Rule("r3", "File quarterly taxes", c2, "month", 3, null, 15, null, today.minusMonths(4).toString(), null, false, now - 70 * day),
                Rule("r4", "Take out the recycling", c0, "week", 1, 1, null, null, today.minusWeeks(3).toString(), null, false, now - 60 * day),
                Rule("r5", "Gym membership fee", c0, "month", 1, null, 28, null, today.minusMonths(2).toString(), null, true, now - 50 * day),
            )
        )
        if (firstRun) return@runBlocking
        app.db.todos().insertAll(
            listOf(
                Todo("t1", "Pay electricity bill", c1, createdAt = now - 1_000, recurringId = "r1", dueDate = tomorrow.toString()),
                Todo("t2", "Renew car insurance", c1, createdAt = now - 2 * hour, recurringId = "r2", dueDate = today.toString()),
                Todo("t3", "Call mom about the weekend plans", c0, createdAt = now - 3 * hour),
                Todo("t4", "Book dentist appointment", c0, createdAt = now - 5 * hour),
                Todo("t5", "Review the Q4 budget draft and send comments to the team", c2, createdAt = now - 8 * hour),
                Todo("t6", "Order new running shoes", c0, createdAt = now - day),
                Todo("t7", "Pick up dry cleaning", c0, done = true, createdAt = now - day, completedAt = now - 2 * hour),
                Todo("t8", "Send invoice to client", c2, done = true, createdAt = now - 2 * day, completedAt = now - 5 * hour),
                // Archive
                Todo("a1", "Renew passport", c2, done = true, createdAt = now - 40 * day, completedAt = now - 9 * day, archivedAt = now - 2 * day, archivedBy = "auto"),
                Todo("a2", "Water the plants", c0, done = true, createdAt = now - 12 * day, completedAt = now - 10 * day, archivedAt = now - 3 * day, archivedBy = "auto"),
                Todo("a3", "Pay water bill", c1, done = true, createdAt = now - 15 * day, completedAt = now - 6 * day, archivedAt = now - 6 * day, archivedBy = "cleared", recurringId = "r1"),
                Todo("a4", "Return library books", c0, done = true, createdAt = now - 40 * day, completedAt = now - 35 * day, archivedAt = now - 34 * day, archivedBy = "cleared"),
                Todo("a5", "Submit expense report", c2, done = true, createdAt = now - 45 * day, completedAt = now - 40 * day, archivedAt = now - 33 * day, archivedBy = "auto"),
                Todo("a6", "Old errand", "errands", done = true, createdAt = now - 60 * day, completedAt = now - 50 * day, archivedAt = now - 43 * day, archivedBy = "auto"),
            )
        )
    }

    // ---------- Helpers ----------

    private fun waitFor(text: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }

    private fun show(content: @Composable () -> Unit) {
        compose.setContent { ListoTheme { content() } }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    private fun clickChip(label: String) {
        compose.onAllNodes(hasText(label) and hasClickAction())[0].performClick()
        compose.waitForIdle()
    }

    // ---------- Tasks ----------

    @Test @Config(qualifiers = S24)
    fun tasksS24() {
        seed(); show { TasksScreen(go = {}) }; waitFor("Pay electricity bill"); capture("01-tasks-s24")
    }

    @Test @Config(qualifiers = PIXEL10)
    fun tasksPixel10() {
        seed(); show { TasksScreen(go = {}) }; waitFor("Pay electricity bill"); capture("02-tasks-pixel10")
    }

    @Test @Config(qualifiers = PIXEL10_DARK)
    fun tasksPixel10Dark() {
        seed(); show { TasksScreen(go = {}) }; waitFor("Pay electricity bill"); capture("03-tasks-pixel10-dark")
    }

    @Test @Config(qualifiers = S24)
    fun tasksFilteredS24() {
        seed(); show { TasksScreen(go = {}) }; waitFor("Pay electricity bill"); clickChip("bl"); capture("04-tasks-filtered-s24")
    }

    @Test @Config(qualifiers = S24)
    fun tasksFirstRunS24() {
        seed(categories = listOf("Personal", "Work"), firstRun = true); show { TasksScreen(go = {}) }; waitFor("Choose folder"); capture("05-tasks-first-run-s24")
    }

    @Test @Config(qualifiers = S24_DARK)
    fun tasksLongCategoriesS24Dark() {
        seed(categories = listOf("Personal", "Work", "Errands", "Health")); show { TasksScreen(go = {}) }; waitFor("Pay electricity bill")
        capture("06-tasks-long-categories-s24-dark")
    }

    // ---------- Recurring ----------

    @Test @Config(qualifiers = S24)
    fun recurringS24() {
        seed(); show { RecurringScreen(go = {}) }; waitFor("File quarterly taxes"); capture("07-recurring-s24")
    }

    @Test @Config(qualifiers = PIXEL10_DARK)
    fun recurringPixel10Dark() {
        seed(); show { RecurringScreen(go = {}) }; waitFor("File quarterly taxes"); capture("08-recurring-pixel10-dark")
    }

    @Test @Config(qualifiers = S24)
    fun ruleEditorS24() {
        seed(); show { RuleEditorScreen(ruleId = null, back = {}) }; waitFor("Repeats")
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("Pay the rent")
        capture("09-rule-editor-s24")
    }

    // ---------- Archive ----------

    @Test @Config(qualifiers = S24)
    fun archiveS24() {
        seed(); show { ArchiveScreen(go = {}) }; waitFor("Renew passport"); capture("10-archive-s24")
    }

    @Test @Config(qualifiers = PIXEL10_DARK)
    fun archiveFilteredPixel10Dark() {
        seed(); show { ArchiveScreen(go = {}) }; waitFor("Renew passport"); clickChip("pw"); capture("11-archive-filtered-pixel10-dark")
    }

    // ---------- Settings, help, quick add ----------

    @Test @Config(qualifiers = S24)
    fun settingsS24() {
        seed(); show { SettingsScreen(go = {}, back = {}) }; waitFor("Categories"); capture("12-settings-s24")
    }

    @Test @Config(qualifiers = PIXEL10)
    fun helpPixel10() {
        seed(); show { HelpScreen(back = {}) }; waitFor("Recurring tasks"); capture("13-help-pixel10")
    }

    @Test @Config(qualifiers = PIXEL10)
    fun quickAddPixel10() {
        seed(); show { QuickAddSheet(onClose = {}) }; waitFor("New task"); capture("14-quick-add-pixel10")
    }

    // ---------- Widget ----------

    @OptIn(ExperimentalGlanceApi::class)
    private fun widgetShot(name: String, width: Dp, height: Dp) {
        val file = File("build/screenshots/$name.png")
        try {
            val views = runBlocking { TodoWidget().compose(app, size = DpSize(width, height)) }
            val activity = compose.activity
            val density = activity.resources.displayMetrics.density
            val pad = (16 * density).toInt()
            val frame = FrameLayout(activity).apply {
                setPadding(pad, pad, pad, pad)
                // A mid-grey "wallpaper" so the widget's corners and edges show.
                setBackgroundColor(if (activity.resources.configuration.isNightModeActive) Color.rgb(40, 44, 52) else Color.rgb(176, 184, 196))
            }
            compose.runOnUiThread {
                val widget = views.apply(activity, frame)
                frame.addView(widget, FrameLayout.LayoutParams((width.value * density).toInt(), (height.value * density).toInt()))
                activity.setContentView(frame, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
            compose.waitForIdle()
            frame.captureRoboImage(file.path)
        } catch (t: Throwable) {
            File("build/screenshots/$name-error.txt").apply { parentFile?.mkdirs() }.writeText(t.stackTraceToString())
            throw t
        }
    }

    @Test @Config(qualifiers = S24)
    fun widgetS24() {
        seed(); widgetShot("15-widget-s24-full-page", 360.dp, 600.dp)
    }

    @Test @Config(qualifiers = PIXEL10_DARK)
    fun widgetPixel10Dark() {
        seed(); widgetShot("16-widget-pixel10-dark-full-page", 384.dp, 640.dp)
    }

    @Test @Config(qualifiers = PIXEL10)
    fun widgetPixel10SmallLongNames() {
        seed(categories = listOf("Personal", "Work", "Errands", "Health")); widgetShot("17-widget-pixel10-4x3-long-names", 384.dp, 330.dp)
    }

    @Test @Config(qualifiers = S24)
    fun widgetEmptyS24() {
        seed(firstRun = true); widgetShot("18-widget-s24-empty", 360.dp, 300.dp)
    }
}
