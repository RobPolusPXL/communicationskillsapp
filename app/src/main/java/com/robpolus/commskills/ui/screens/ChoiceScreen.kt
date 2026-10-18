package com.robpolus.commskills.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.robpolus.commskills.ui.ChoiceUi
import com.robpolus.commskills.ui.theme.Blue
import com.robpolus.commskills.ui.theme.BlueDark
import com.robpolus.commskills.ui.theme.Correct
import com.robpolus.commskills.ui.theme.CoralDark
import com.robpolus.commskills.ui.theme.GreenEdge
import com.robpolus.commskills.ui.theme.Line
import com.robpolus.commskills.ui.theme.Wrong

/** Meerkeuze (oefenen of examen): één woord, vijf opties. */
@Composable
fun ChoiceScreen(
  ui: ChoiceUi?,
  onPick: (Int) -> Unit,
  onNext: () -> Unit,
  onQuit: () -> Unit,
  topExtra: @Composable () -> Unit = {},
) {
  var confirmQuit by remember { mutableStateOf(false) }
  BackHandler { confirmQuit = true }
  if (ui == null) return

  Column(
    Modifier.fillMaxSize().systemBarsPadding().padding(16.dp).verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = { confirmQuit = true }) { Icon(Icons.Filled.Close, contentDescription = "Stoppen") }
      LinearProgressIndicator(
        progress = { ((ui.number - 1).toFloat() / ui.total.coerceAtLeast(1)).coerceIn(0f, 1f) },
        modifier = Modifier.weight(1f).height(14.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(7.dp)),
        color = Correct,
        trackColor = Line,
      )
      topExtra()
    }
    ChunkyCard(edge = BlueDark, fill = Blue) {
      Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          "${ui.label} · ${ui.number}/${ui.total}",
          style = MaterialTheme.typography.labelMedium,
          color = Color.White.copy(alpha = 0.85f),
        )
        Text(ui.prompt, style = MaterialTheme.typography.headlineMedium, color = Color.White)
      }
    }
    ui.options.forEachIndexed { i, text ->
      val answered = ui.picked != null
      when {
        answered && i == ui.correctIndex -> ChunkyButton(text, {}, color = Correct, edge = GreenEdge)
        answered && i == ui.picked -> ChunkyButton(text, {}, color = Wrong, edge = CoralDark)
        else -> ChunkyOutlineButton(text, onClick = { onPick(i) }, enabled = !answered)
      }
    }
    if (ui.picked != null) {
      val ok = ui.picked == ui.correctIndex
      Text(if (ok) "Juist!" else "Fout", style = MaterialTheme.typography.titleMedium, color = if (ok) Correct else Wrong)
      ChunkyButton("Volgende", onClick = onNext)
    }
  }

  if (confirmQuit) {
    AlertDialog(
      onDismissRequest = { confirmQuit = false },
      title = { Text("Stoppen?") },
      text = { Text(if (ui.exam) "Het examen wordt afgebroken." else "De sessie wordt afgebroken.") },
      confirmButton = { TextButton(onClick = { confirmQuit = false; onQuit() }) { Text("Stoppen") } },
      dismissButton = { TextButton(onClick = { confirmQuit = false }) { Text("Verder spelen") } },
    )
  }
}
