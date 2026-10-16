package com.robpolus.commskills.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.robpolus.commskills.domain.Note
import com.robpolus.commskills.ui.TypeFeedback
import com.robpolus.commskills.ui.TypeUi
import com.robpolus.commskills.ui.theme.Correct
import com.robpolus.commskills.ui.theme.Line
import com.robpolus.commskills.ui.theme.Wrong

fun noteText(note: Note): String = when (note) {
  Note.TYPO -> "Let op: er zit een typfout in."
  Note.MISSING_ACCENT -> "Let op: er ontbreekt een accent."
  Note.MISSING_TO -> "Voor 100% correct moet het ‘to …’ zijn."
  Note.SYNONYM -> "Ook goed: een ander woord met dezelfde vertaling."
}

@Composable
fun TypeScreen(
  ui: TypeUi?,
  onSubmit: (String) -> Unit,
  onNext: () -> Unit,
  onQuit: () -> Unit,
  topExtra: @Composable () -> Unit = {},
) {
  var confirmQuit by remember { mutableStateOf(false) }
  BackHandler { confirmQuit = true }
  if (ui == null) return
  var text by remember(ui.serial) { mutableStateOf("") }
  val focus = remember { FocusRequester() }
  LaunchedEffect(ui.serial) { runCatching { focus.requestFocus() } }

  Column(
    Modifier.fillMaxSize().systemBarsPadding().padding(16.dp).verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = { confirmQuit = true }) { Icon(Icons.Filled.Close, contentDescription = "Stoppen") }
      LinearProgressIndicator(
        progress = { (ui.completed.toFloat() / ui.total.coerceAtLeast(1)).coerceIn(0f, 1f) },
        modifier = Modifier.weight(1f).height(10.dp),
        trackColor = Line,
      )
      topExtra()
    }
    Text(ui.promptLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
      ui.prompt,
      style = if (ui.grammar) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
    )
    ui.hint?.let {
      Text(
        if (it.startsWith("(")) it else "($it)",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
      )
    }

    OutlinedTextField(
      value = text,
      onValueChange = { text = it },
      label = { Text(ui.answerLabel) },
      singleLine = true,
      enabled = ui.feedback == null,
      modifier = Modifier.fillMaxWidth().focusRequester(focus),
      keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Done),
      keyboardActions = KeyboardActions(onDone = { if (ui.feedback == null && text.isNotBlank()) onSubmit(text) }),
    )

    val feedback = ui.feedback
    if (feedback == null) {
      Button(onClick = { onSubmit(text) }, enabled = text.isNotBlank(), modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Text("Controleren")
      }
    } else {
      FeedbackCard(feedback)
      Button(
        onClick = onNext,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (feedback.correct) Correct else Wrong),
      ) { Text("Volgende") }
    }
  }

  if (confirmQuit) {
    AlertDialog(
      onDismissRequest = { confirmQuit = false },
      title = { Text("Stoppen?") },
      text = { Text("Je voortgang tot nu toe blijft bewaard.") },
      confirmButton = { TextButton(onClick = { confirmQuit = false; onQuit() }) { Text("Stoppen") } },
      dismissButton = { TextButton(onClick = { confirmQuit = false }) { Text("Verder spelen") } },
    )
  }
}

@Composable
private fun FeedbackCard(f: TypeFeedback) {
  val color = if (f.correct) Correct else Wrong
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
    border = BorderStroke(2.dp, color),
  ) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(if (f.correct) "Juist!" else "Fout", style = MaterialTheme.typography.titleMedium, color = color)
      f.notes.forEach { Text(noteText(it), style = MaterialTheme.typography.bodyMedium) }
      if (!f.correct) {
        Text("Jouw antwoord: ${f.given}", style = MaterialTheme.typography.bodyMedium)
        Text("Juiste antwoord: ${f.correctAnswer}", style = MaterialTheme.typography.titleSmall)
      } else if (f.correctAnswer.isNotBlank()) {
        Text("Antwoord: ${f.correctAnswer}", style = MaterialTheme.typography.bodyMedium)
      }
      if (f.explanation.isNotBlank()) {
        Spacer(Modifier.height(2.dp))
        Text(f.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}
