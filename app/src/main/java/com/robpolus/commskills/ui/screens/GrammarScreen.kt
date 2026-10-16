package com.robpolus.commskills.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import com.robpolus.commskills.domain.SessionBuilder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.robpolus.commskills.domain.AppData
import com.robpolus.commskills.domain.Grammar
import com.robpolus.commskills.domain.GrammarExercise
import com.robpolus.commskills.ui.theme.Line

@Composable
fun GrammarScreen(
  data: AppData,
  exercises: List<GrammarExercise>,
  onBack: () -> Unit,
  onPlay: (groupIds: List<String>?) -> Unit,
  onOverview: (type: Int) -> Unit,
  onExam: ((String) -> Unit)? = null,
) {
  var selected by rememberSaveable { mutableStateOf(setOf<String>()) }
  val view = Grammar.view(data, exercises)

  ScreenScaffold("Grammar", onBack, accent = com.robpolus.commskills.ui.theme.Orange2Dark) { padding ->
    Column(Modifier.padding(padding)) {
      LazyColumn(
        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        item {
          Text(
            "If-zinnen: ${(view.percentage() * 100).toInt()}% (examen vanaf 70%)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp),
          )
          Spacer(Modifier.height(6.dp))
          LinearProgressIndicator(
            progress = { view.percentage().toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            trackColor = Line,
          )
        }
        item { Text("Kies één of meer types", style = MaterialTheme.typography.titleMedium) }
        items(Grammar.types, key = { it }) { type ->
          val group = Grammar.groupId(type)
          val checked = group in selected
          ChunkyCard(
            edge = if (checked) com.robpolus.commskills.ui.theme.Orange2 else Line,
            fill = if (checked) com.robpolus.commskills.ui.theme.Orange2.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.White,
            onClick = { selected = if (checked) selected - group else selected + group },
          ) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
              Checkbox(checked = checked, onCheckedChange = { selected = if (it) selected + group else selected - group })
              Column(Modifier.weight(1f)) {
                Text(Grammar.title(type), style = MaterialTheme.typography.titleSmall)
                Text(
                  "${Grammar.subtitle(type)} · ${(view.groupAverage(group) * 100).toInt()}%",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Box(Modifier.size(18.dp).clip(CircleShape).background(view.groupColor(group).color()))
              IconButton(onClick = { onOverview(type) }) { Icon(Icons.Filled.Info, contentDescription = "Overzicht") }
            }
          }
        }
        item {
          val open = view.mixedUnlocked()
          ChunkyCard(
            edge = if (open) com.robpolus.commskills.ui.theme.Teal else Line,
            fill = if (open) com.robpolus.commskills.ui.theme.Teal.copy(alpha = 0.10f) else androidx.compose.ui.graphics.Color.White,
            onClick = if (open) ({ onPlay(null) }) else null,
          ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
              Column(Modifier.weight(1f)) {
                Text("Gemixt", style = MaterialTheme.typography.titleSmall)
                Text(
                  if (open) "Alle types door elkaar (het moeilijkste)" else "Speel eerst alle vier de types minstens één keer",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              if (!open) Icon(Icons.Filled.Lock, contentDescription = "Vergrendeld")
            }
          }
        }
        item { Spacer(Modifier.height(8.dp)) }
      }
      Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val count = exercises.count { Grammar.groupId(it.type) in selected }
        ChunkyButton(
          text = when {
            selected.isEmpty() -> "Kies een type om te starten"
            selected.size == 1 -> "Oefenen (${minOf(count, SessionBuilder.SESSION_SIZE).let { if (it < count) "$it van $count" else "$count" }} zinnen)"
            else -> "${selected.size} types combineren (${minOf(count, SessionBuilder.SESSION_SIZE).let { if (it < count) "$it van $count" else "$count" }} zinnen)"
          },
          onClick = { onPlay(Grammar.types.map { Grammar.groupId(it) }.filter { it in selected }) },
          enabled = selected.isNotEmpty(),
          color = com.robpolus.commskills.ui.theme.Orange2,
          edge = com.robpolus.commskills.ui.theme.Orange2Dark,
        )
        if (onExam != null) ChunkyOutlineButton(
          text = if (view.examAvailable()) "Examen doen (21 vragen)" else "Examen vanaf 70% (nu ${(view.percentage() * 100).toInt()}%)",
          onClick = { onExam(com.robpolus.commskills.domain.Levels.GRAMMAR) },
          enabled = view.examAvailable(),
        )
      }
    }
  }
}

/** Alle zinnen van één type, per zin rood/oranje/groen, met het juiste antwoord ingevuld. */
@Composable
fun GrammarOverviewScreen(data: AppData, exercises: List<GrammarExercise>, type: Int, onBack: () -> Unit) {
  val view = Grammar.view(data, exercises)
  val group = Grammar.groupId(type)
  ScreenScaffold(Grammar.title(type), onBack, accent = com.robpolus.commskills.ui.theme.Orange2Dark) { padding ->
    LazyColumn(Modifier.padding(padding).padding(horizontal = 16.dp)) {
      item {
        Text(
          Grammar.subtitle(type),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 8.dp),
        )
      }
      items(exercises.filter { it.type == type }, key = { it.id }) { e ->
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
          Box(Modifier.size(14.dp).clip(CircleShape).background(view.wordColor(group, e.id).color()))
          Text(e.filled(), modifier = Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyMedium)
        }
        HorizontalDivider()
      }
    }
  }
}
