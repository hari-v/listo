package com.hariv.listo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hariv.listo.App
import com.hariv.listo.data.Repository
import com.hariv.listo.data.Rule
import com.hariv.listo.recurring.Recurrence
import com.hariv.listo.recurring.RepeatUnit
import com.hariv.listo.recurring.Schedule
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(go: (Screen) -> Unit, back: () -> Unit) {
    val context = LocalContext.current
    val app = remember { App.get(context) }
    val rules by app.repo.rules.collectAsState(initial = null)
    var toDelete by remember { mutableStateOf<Rule?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Recurring") }, navigationIcon = { BackButton(back) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { go(Screen.EditRule(null)) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add rule") },
            )
        },
    ) { padding ->
        val list = rules
        when {
            list == null -> Unit
            list.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No recurring todos yet.\n\nTap \"Add rule\" for things like bills or birthdays. " +
                        "Each one appears on your list the day before it's due.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(list, key = { it.id }) { rule ->
                    RuleCard(
                        rule = rule,
                        onEdit = { go(Screen.EditRule(rule.id)) },
                        onPause = { app.appScope.launch { app.repo.setRulePaused(rule.id, !rule.paused) } },
                        onDelete = { toDelete = rule },
                    )
                }
            }
        }
    }

    toDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete \"${rule.text}\"?") },
            text = { Text("It stops repeating. Todos it already created stay on your list.") },
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
private fun RuleCard(rule: Rule, onEdit: () -> Unit, onPause: () -> Unit, onDelete: () -> Unit) {
    val schedule = remember(rule) { runCatching { rule.schedule() }.getOrNull() }
    val next = remember(rule) { schedule?.let { Recurrence.nextDue(it, rule.lastDue()) } }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 4.dp)) {
            Text(rule.text, style = MaterialTheme.typography.titleMedium)
            Text(
                "${rule.category} · ${schedule?.let { Recurrence.describe(it) } ?: "Schedule can't be read"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = when {
                    rule.paused -> "Paused"
                    next != null -> "Next due ${Fmt.shortDate(next)} · on your list from ${Fmt.shortDate(next.minusDays(1))}"
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onPause) { Text(if (rule.paused) "Resume" else "Pause") }
                TextButton(onClick = onDelete) { Text("Delete") }
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

    var existing by remember { mutableStateOf<Rule?>(null) }
    var text by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(app.prefs.lastCategory) }
    var unit by rememberSaveable { mutableStateOf(RepeatUnit.MONTH.key) }
    var everyText by rememberSaveable { mutableStateOf("1") }
    var weekday by rememberSaveable { mutableIntStateOf(today.dayOfWeek.value) }
    var dayText by rememberSaveable { mutableStateOf("1") }
    var month by rememberSaveable { mutableIntStateOf(today.monthValue) }
    var startIso by rememberSaveable { mutableStateOf(today.toString()) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(ruleId) {
        if (ruleId != null) {
            repo.getRule(ruleId)?.let { r ->
                existing = r
                text = r.text
                category = r.category
                unit = r.unit
                everyText = r.every.toString()
                r.weekday?.let { weekday = it }
                r.day?.let { dayText = it.toString() }
                r.month?.let { month = it }
                startIso = r.startDate
            }
        }
    }

    val repeat = RepeatUnit.of(unit)
    val every: Int? = if (repeat == RepeatUnit.YEAR) 1 else everyText.toIntOrNull()
    val day: Int? = dayText.toIntOrNull()
    val start = runCatching { LocalDate.parse(startIso) }.getOrDefault(today)
    val error: String? = when {
        text.isBlank() -> "Enter what to do."
        every == null || every !in 1..99 -> "\"Every\" must be a number from 1 to 99."
        repeat != RepeatUnit.WEEK && (day == null || day !in 1..31) -> "Day must be a number from 1 to 31."
        repeat == RepeatUnit.YEAR && day != null && day > Month.of(month).maxLength() ->
            "${monthName(month)} has at most ${Month.of(month).maxLength()} days."
        else -> null
    }
    val schedule: Schedule? = if (error == null && every != null) {
        Schedule(
            unit = repeat,
            every = every,
            weekday = if (repeat == RepeatUnit.WEEK) weekday else null,
            day = if (repeat == RepeatUnit.WEEK) null else day,
            month = if (repeat == RepeatUnit.YEAR) month else null,
            startDate = start,
        )
    } else {
        null
    }
    // New rules never create todos for dates already past; edited rules keep their history.
    val createdUpTo: LocalDate? = existing?.lastDue() ?: today.minusDays(1)
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
            lastDueDate = base?.lastDueDate ?: createdUpTo?.toString(),
            paused = base?.paused ?: false,
            createdAt = base?.createdAt ?: System.currentTimeMillis(),
        )
        app.appScope.launch { repo.saveRule(rule) }
        back()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (ruleId == null) "New recurring todo" else "Edit recurring todo") },
                navigationIcon = { BackButton(back) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(Repository.MAX_TEXT) },
                label = { Text("What to do (e.g. Pay electricity bill)") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Label("Category")
            CategoryPicker(selected = category, onSelect = { category = it })

            Label("Repeats")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = repeat == RepeatUnit.WEEK, onClick = { unit = RepeatUnit.WEEK.key }, label = { Text("Weeks") })
                FilterChip(selected = repeat == RepeatUnit.MONTH, onClick = { unit = RepeatUnit.MONTH.key }, label = { Text("Months") })
                FilterChip(selected = repeat == RepeatUnit.YEAR, onClick = { unit = RepeatUnit.YEAR.key }, label = { Text("Year") })
            }

            if (repeat != RepeatUnit.YEAR) {
                val unitWord = if (repeat == RepeatUnit.WEEK) "weeks" else "months"
                OutlinedTextField(
                    value = everyText,
                    onValueChange = { v -> everyText = v.filter { it.isDigit() }.take(2) },
                    label = { Text("Every how many $unitWord?") },
                    supportingText = {
                        Text(if (repeat == RepeatUnit.WEEK) "1 = every week, 2 = every other week" else "1 = monthly, 3 = quarterly, 6 = twice a year")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            when (repeat) {
                RepeatUnit.WEEK -> {
                    Label("On")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..7).forEach { d ->
                            FilterChip(
                                selected = weekday == d,
                                onClick = { weekday = d },
                                label = { Text(DayOfWeek.of(d).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)) },
                            )
                        }
                    }
                }
                RepeatUnit.MONTH -> DayField(dayText, "Day of the month (1–31)") { dayText = it }
                RepeatUnit.YEAR -> {
                    Label("Month")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..12).forEach { m ->
                            FilterChip(selected = month == m, onClick = { month = m }, label = { Text(monthName(m)) })
                        }
                    }
                    DayField(dayText, "Day (1–31)") { dayText = it }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Starting from ${Fmt.longDate(start)}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "For quarterly bills, pick the date of the first one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { showDatePicker = true }) { Text("Change") }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (error != null) {
                        Text(error, color = MaterialTheme.colorScheme.error)
                    } else if (schedule != null) {
                        Text(Recurrence.describe(schedule), style = MaterialTheme.typography.titleSmall)
                        Text("Next due: " + preview.joinToString(", ") { Fmt.shortDate(it) })
                        Text(
                            "Each one appears on your list the day before it's due.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { save() }, enabled = error == null) { Text("Save") }
                TextButton(onClick = back) { Text("Cancel") }
            }
            Spacer(Modifier.padding(bottom = 24.dp))
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
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun DayField(value: String, label: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(2)) },
        label = { Text(label) },
        supportingText = { Text("29–31: shorter months use their last day") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun monthName(m: Int): String = Month.of(m.coerceIn(1, 12)).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
