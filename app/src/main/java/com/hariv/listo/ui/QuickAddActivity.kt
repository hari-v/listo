package com.hariv.listo.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.hariv.listo.App
import com.hariv.listo.data.Repository
import com.hariv.listo.ui.theme.ListoIcons
import com.hariv.listo.ui.theme.ListoTheme
import kotlinx.coroutines.launch

/** The small "add a task" sheet opened from the widget's + button, shown over the home screen. */
class QuickAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ListoTheme { QuickAddSheet(onClose = { finish() }) }
        }
    }
}

@Composable
private fun QuickAddSheet(onClose: () -> Unit) {
    val context = LocalContext.current
    val app = remember { App.get(context) }
    val categories by app.repo.categories.list.collectAsState()
    var text by rememberSaveable { mutableStateOf("") }
    var categoryChoice by rememberSaveable { mutableStateOf(app.repo.categories.defaultCategory()) }
    val category = if (categoryChoice in categories) categoryChoice else categories.first()
    val focus = remember { FocusRequester() }
    val scheme = MaterialTheme.colorScheme

    fun add() {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val cat = category
        app.appScope.launch {
            app.repo.add(clean, cat)
            app.repo.flushBackup()
        }
        onClose()
    }

    LaunchedEffect(Unit) { focus.requestFocus() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim: tap outside the sheet to close.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() },
        )
        Surface(
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            color = scheme.surface,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding(),
        ) {
            Column(Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp)) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(scheme.outlineVariant),
                )
                Spacer(Modifier.height(14.dp))
                Text("New task", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp))
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategorySelector(categories, category, { categoryChoice = it })
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(scheme.surfaceContainerHigh)
                            .padding(horizontal = 16.dp),
                    ) {
                        if (text.isEmpty()) Text("What needs doing?", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
                        BasicTextField(
                            value = text,
                            onValueChange = { text = it.take(Repository.MAX_TEXT) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                            cursorBrush = SolidColor(scheme.primary),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { add() }),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus),
                        )
                    }
                    FilledIconButton(
                        onClick = { add() },
                        enabled = text.isNotBlank(),
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = scheme.primary,
                            contentColor = scheme.onPrimary,
                            disabledContainerColor = scheme.surfaceContainerHigh,
                            disabledContentColor = scheme.outline,
                        ),
                    ) {
                        Icon(ListoIcons.ArrowUp, contentDescription = "Add task", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
