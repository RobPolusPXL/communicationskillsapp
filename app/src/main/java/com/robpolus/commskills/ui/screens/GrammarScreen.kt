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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.robpolus.commskills.ui.theme.Blue
import com.robpolus.commskills.ui.theme.Correct
import com.robpolus.commskills.ui.theme.Orange2
import com.robpolus.commskills.ui.theme.Orange2Dark
import com.robpolus.commskills.ui.theme.Teal
import com.robpolus.commskills.ui.theme.Wrong

private val OrangeLight = Color(0xFFFFF0DB)
private val TealLight = Color(0xFFDCF4F1)
private val typeColors = listOf(Correct, Blue, Orange2, Wrong)

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

  ScreenScaffold("Grammar", onBack, accent = Orange2Dark) { padding ->
    Column(Modifier.padding(padding)) {
      LazyColumn(
        modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        item { Spacer(Modifier.height(2.dp)) }
        item { LevelProgressCard("If-zinnen", view.percentage(), Orange2, OrangeLight) }
        item { Text("Kies één of meer types", style = MaterialTheme.typography.titleSmall, color = Color(0xFF44526A)) }
        items(Grammar.types, key = { it }) { type ->
          val group = Grammar.groupId(type)
          val checked = group in selected
          ChunkyCard(
            edge = if (checked) Orange2 else Line,
            fill = if (checked) OrangeLight else Color.White,
            onClick = { selected = if (checked) selected - group else selected + group },
          ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 26.dp), verticalAlignment = Alignment.CenterVertically) {
              Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(typeColors[type]),
                contentAlignment = Alignment.Center,
              ) { Text("$type", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp) }
              Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(Grammar.title(type), style = MaterialTheme.typography.titleSmall)
                Text(
                  Grammar.subtitle(type),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val avg = view.groupAverage(group).toFloat()
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFE8EBF2))) {
                  Box(
                    Modifier.fillMaxWidth(avg.coerceIn(0f, 1f)).height(6.dp).clip(RoundedCornerShape(3.dp))
                      .background(view.groupColor(group).color()),
                  )
                }
              }
              IconButton(onClick = { onOverview(type) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Info, contentDescription = "Overzicht", tint = Color(0xFF9AA3B2))
              }
              SelectTick(checked, Orange2)
            }
          }
        }
        item {
          val open = view.mixedUnlocked()
          ChunkyCard(
            edge = if (open) Teal else Line,
            fill = if (open) TealLight else Color.White,
            onClick = if (open) ({ onPlay(null) }) else null,
          ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 26.dp), verticalAlignment = Alignment.CenterVertically) {
              Column(Modifier.weight(1f)) {
                Text("Gemixt", style = MaterialTheme.typography.titleSmall)
                Text(
                  if (open) "Alle types door elkaar (het moeilijkste)" else "Speel eerst alle vier de types minstens één keer",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              if (!open) Icon(Icons.Filled.Lock, contentDescription = "Vergrendeld", tint = Color(0xFF9AA3B2))
            }
          }
        }
        item { Spacer(Modifier.height(4.dp)) }
      }
      BottomActions {
        val count = exercises.count { Grammar.groupId(it.type) in selected }
        val shown = minOf(count, SessionBuilder.SESSION_SIZE).let { if (it < count) "$it van $count" else "$count" }
        ChunkyButton(
          text = when {
            selected.isEmpty() -> "Kies een type om te starten"
            selected.size == 1 -> "Oefenen ($shown zinnen)"
            else -> "${selected.size} types combineren ($shown zinnen)"
          },
          onClick = { onPlay(Grammar.types.map { Grammar.groupId(it) }.filter { it in selected }) },
          enabled = selected.isNotEmpty(),
          color = Orange2,
          edge = Orange2Dark,
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
