package com.hariv.listo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hariv.listo.App
import com.hariv.listo.data.Repository
import com.hariv.listo.data.Rule
import com.hariv.listo.recurring.Recurrence
import com.hariv.listo.recurring.RepeatUnit
import com.hariv.listo.recurring.Schedule
import com.hariv.listo.ui.theme.ListoIcons
import com.hariv.listo.ui.theme.categoryColor
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun RecurringScreen(go: (Screen) -> Unit) {
    val context = LocalContext.current
    val app = remember { App.get(context) }
    val rules by app.repo.rules.collectAsState(initial = null)
    val categories by app.repo.categories.list.collectAsState()
    var toDelete by remember { mutableStateOf<Rule?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { ListoNavBar(Screen.Recurring, go) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { go(Screen.EditRule(null)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(ListoIcons.Plus, contentDescription = null) },
                text = { Text("New rule") },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PageHeader("Recurring", "Each one appears on your list the day before it's due.")
            val list = rules
            when {
                list == null -> Unit
                list.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(ListoIcons.Repeat, "No recurring tasks", "Add bills, birthdays, quarterly reviews, anything that repeats.")
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                ) {
                    itemsIndexed(list, key = { _, r -> r.id }) { index, rule ->
                        GroupRow(index, list.size, Modifier.animateItem(), dividerInset = 16.dp) {
                            RuleRow(
                                rule = rule,
                                categories = categories,
                                onEdit = { go(Screen.EditRule(rule.id)) },
                                onPause = { app.appScope.launch { app.repo.setRulePaused(rule.id, !rule.paused) } },
                                onDelete = { toDelete = rule },
                            )
                        }
                    }
                }
            }
        }
    }

    toDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete \"${rule.text}\"?") },
            text = { Text("It stops repeating. Tasks it already added stay on your list.") },
            confirmButton = {
                TextButton(onClick = {
                    toDelete = null
                    app.appScope.launch { app.repo.deleteRule(rule.id) }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun RuleRow(rule: Rule, categories: List<String>, onEdit: () -> Unit, onPause: () -> Unit, onDelete: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val schedule = remember(rule) { runCatching { rule.schedule() }.getOrNull() }
    val next = remember(rule) { schedule?.let { Recurrence.nextDue(it, rule.lastDue()) } }
    var menu by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(rule.text, style = MaterialTheme.typography.bodyLarge, color = if (rule.paused) scheme.onSurfaceVariant else scheme.onSurface)
            Text(
                schedule?.let { Recurrence.describe(it) } ?: "This schedule can't be read. Edit it to fix.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryTag(categories, rule.category)
                Spacer(Modifier.width(14.dp))
                if (rule.paused) {
                    Icon(ListoIcons.Pause, contentDescription = null, tint = scheme.outline, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Paused", style = MaterialTheme.typography.labelMedium, color = scheme.outline)
                } else if (next != null) {
                    Icon(ListoIcons.Calendar, contentDescription = null, tint = scheme.tertiary, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Next due ${Fmt.shortDate(next)}", style = MaterialTheme.typography.labelMedium, color = scheme.tertiary)
                }
            }
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(ListoIcons.More, contentDescription = "More options") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    leadingIcon = { Icon(ListoIcons.Pencil, null, modifier = Modifier.size(18.dp)) },
                    onClick = { menu = false; onEdit() },
                )
                DropdownMenuItem(
                    text = { Text(if (rule.paused) "Resume" else "Pause") },
                    leadingIcon = { Icon(if (rule.paused) ListoIcons.Play else ListoIcons.Pause, null, modifier = Modifier.size(18.dp)) },
                    onClick = { menu = false; onPause() },
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = { Icon(ListoIcons.Trash, null, modifier = Modifier.size(18.dp)) },
                    onClick = { menu = false; onDelete() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuleEditorScreen(ruleId: String?, back: () -> Unit) {
    val context = LocalContext.current
    val app = remember { App.get(context) }
    val repo = app.repo
    val today = remember { LocalDate.now() }
    val categories by repo.categories.list.collectAsState()

    var existing by remember { mutableStateOf<Rule?>(null) }
    var text by rememberSaveable { mutableStateOf("") }
    var categoryChoice by rememberSaveable { mutableStateOf(repo.categories.defaultCategory()) }
    var unit by rememberSaveable { mutableStateOf(RepeatUnit.MONTH.key) }
    var everyText by rememberSaveable { mutableStateOf("1") }
    var weekday by rememberSaveable { mutableStateOf(today.dayOfWeek.value.toString()) }
    var dayText by rememberSaveable { mutableStateOf("1") }
    var month by rememberSaveable { mutableStateOf(today.monthValue.toString()) }
    var startIso by rememberSaveable { mutableStateOf(today.toString()) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(ruleId) {
        if (ruleId != null) {
            repo.getRule(ruleId)?.let { r ->
                existing = r
                text = r.text
                categoryChoice = r.category
                unit = r.unit
                everyText = r.every.toString()
                r.weekday?.let { weekday = it.toString() }
                r.day?.let { dayText = it.toString() }
                r.month?.let { month = it.toString() }
                startIso = r.startDate
            }
        }
    }

    // A rule may use a category that was since renamed away; show it but let the user pick a current one.
    val category = categoryChoice
    val repeat = RepeatUnit.of(unit)
    val monthNumber = month.toIntOrNull()?.coerceIn(1, 12) ?: 1
    val every: Int? = if (repeat == RepeatUnit.YEAR) 1 else everyText.toIntOrNull()
    val day: Int? = dayText.toIntOrNull()
    val start = runCatching { LocalDate.parse(startIso) }.getOrDefault(today)
    val error: String? = when {
        text.isBlank() -> "Describe the task."
        every == null || every !in 1..99 -> "\"Every\" needs a number from 1 to 99."
        repeat != RepeatUnit.WEEK && (day == null || day !in 1..31) -> "The day needs a number from 1 to 31."
        repeat == RepeatUnit.YEAR && day != null && day > Month.of(monthNumber).maxLength() ->
            "${monthName(monthNumber)} has at most ${Month.of(monthNumber).maxLength()} days."
        else -> null
    }
    val schedule: Schedule? = if (error == null && every != null) {
        Schedule(
            unit = repeat,
            every = every,
            weekday = if (repeat == RepeatUnit.WEEK) weekday.toIntOrNull() ?: 1 else null,
            day = if (repeat == RepeatUnit.WEEK) null else day,
            month = if (repeat == RepeatUnit.YEAR) monthNumber else null,
            startDate = start,
        )
    } else {
        null
    }
    // New rules never add tasks for dates already past; edited rules keep their history.
    val createdUpTo: LocalDate = existing?.lastDue() ?: today.minusDays(1)
    val preview = schedule?.let { Recurrence.upcoming(it, createdUpTo, 3) }.orEmpty()

    fun save() {
        val s = schedule ?: return
        val base = existing
        val rule = Rule(
            id = base?.id ?: repo.newRuleId(),
            text = text.trim().take(Repository.MAX_TEXT),
            category = category,
            unit = repeat.key,
            every = s.every,
            weekday = s.weekday,
            day = s.day,
            month = s.month,
            startDate = start.toString(),
            lastDueDate = base?.lastDueDate ?: createdUpTo.toString(),
            paused = base?.paused ?: false,
            createdAt = base?.createdAt ?: System.currentTimeMillis(),
        )
        app.appScope.launch { repo.saveRule(rule) }
        back()
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
    )

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
            PageHeader(if (ruleId == null) "New recurring task" else "Edit recurring task", onBack = back)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                FormLabel("Task")
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(Repository.MAX_TEXT) },
                    placeholder = { Text("e.g. Pay electricity bill") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = MaterialTheme.shapes.medium,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )

                FormLabel("Category")
                val categoryOptions = if (category in categories) categories else categories + category
                ChoiceRow(
                    options = categoryOptions,
                    selected = category,
                    onSelect = { categoryChoice = it },
                    dotColor = { categoryColor(categories, it) },
                )

                FormLabel("Repeats")
                ChoiceRow(
                    options = listOf(RepeatUnit.WEEK.key, RepeatUnit.MONTH.key, RepeatUnit.YEAR.key),
                    selected = repeat.key,
                    onSelect = { unit = it },
                    label = {
                        when (RepeatUnit.of(it)) {
                            RepeatUnit.WEEK -> "Weekly"
                            RepeatUnit.MONTH -> "Monthly"
                            RepeatUnit.YEAR -> "Yearly"
                        }
                    },
                )

                if (repeat != RepeatUnit.YEAR) {
                    FormLabel("Every")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NumberField(everyText, { everyText = it }, fieldColors, Modifier.width(96.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (repeat == RepeatUnit.WEEK) "week(s)" else "month(s)",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Hint(if (repeat == RepeatUnit.WEEK) "2 means every other week." else "3 means quarterly, 6 means twice a year.")
                }

                when (repeat) {
                    RepeatUnit.WEEK -> {
                        FormLabel("On")
                        ChoiceRow(
                            options = (1..7).map { it.toString() },
                            selected = weekday,
                            onSelect = { weekday = it },
                            label = { DayOfWeek.of(it.toInt()).getDisplayName(TextStyle.SHORT, Locale.ENGLISH) },
                        )
                    }
                    RepeatUnit.MONTH -> {
                        FormLabel("Day of the month")
                        NumberField(dayText, { dayText = it }, fieldColors, Modifier.width(96.dp))
                        Hint("29 to 31 fall on the last day of shorter months.")
                    }
                    RepeatUnit.YEAR -> {
                        FormLabel("Month")
                        ChoiceRow(
                            options = (1..12).map { it.toString() },
                            selected = monthNumber.toString(),
                            onSelect = { month = it },
                            label = { monthName(it.toInt()) },
                        )
                        FormLabel("Day")
                        NumberField(dayText, { dayText = it }, fieldColors, Modifier.width(96.dp))
                        Hint("A 29 February birthday shows on 28 February in other years.")
                    }
                }

                FormLabel("Starting from")
                Surface(
                    onClick = { showDatePicker = true },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Icon(ListoIcons.Calendar, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(Fmt.longDate(start), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text("Change", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Hint("For a quarterly bill, pick the date of the first one.")

                Spacer(Modifier.height(20.dp))
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (error != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (error != null) {
                            Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        } else if (schedule != null) {
                            val ink = MaterialTheme.colorScheme.onTertiaryContainer
                            Text(Recurrence.describe(schedule), style = MaterialTheme.typography.titleSmall, color = ink)
                            Text("Next due " + preview.joinToString(", ") { Fmt.shortDate(it) }, style = MaterialTheme.typography.bodyMedium, color = ink)
                            Text("Each one appears on your list the day before.", style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = 0.8f))
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                OutlinedButton(onClick = back, modifier = Modifier.weight(1f).height(48.dp)) { Text("Cancel") }
                Button(onClick = { save() }, enabled = error == null, modifier = Modifier.weight(1f).height(48.dp)) {
                    Text(if (ruleId == null) "Add rule" else "Save changes")
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        startIso = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text("Set date") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 22.dp, bottom = 8.dp),
    )
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun NumberField(
    value: String,
    onChange: (String) -> Unit,
    colors: androidx.compose.material3.TextFieldColors,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(2)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = MaterialTheme.shapes.medium,
        colors = colors,
        modifier = modifier,
    )
}

private fun monthName(m: Int): String = Month.of(m.coerceIn(1, 12)).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
