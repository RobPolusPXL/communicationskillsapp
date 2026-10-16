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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.robpolus.commskills.domain.SessionBuilder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.robpolus.commskills.domain.AppData
import com.robpolus.commskills.domain.Learning
import com.robpolus.commskills.domain.Levels
import com.robpolus.commskills.domain.Scoring
import com.robpolus.commskills.domain.DirectionChoice
import com.robpolus.commskills.ui.theme.Line

private val levelTitles = mapOf(
  Levels.VOCAB_MATCH to "Matchen",
  Levels.VOCAB_TYPE to "Typen",
)

@Composable
fun VocabularyScreen(
  data: AppData,
  onBack: () -> Unit,
  onPlay: (level: String, groupIds: List<String>?, direction: DirectionChoice) -> Unit,
  onOverview: (level: String, listId: String) -> Unit,
  onExam: ((level: String) -> Unit)? = null,
) {
  var level by rememberSaveable { mutableStateOf(Levels.VOCAB_MATCH) }
  var confirmed by rememberSaveable { mutableStateOf(setOf<String>()) }
  var pendingLevel by rememberSaveable { mutableStateOf<String?>(null) }
  var selected by rememberSaveable { mutableStateOf(setOf<String>()) }
  var direction by rememberSaveable { mutableStateOf(DirectionChoice.MIXED) }

  val view = Learning.vocabularyView(data, level)
  val unlocked = Learning.levelUnlocked(data, level) || level in confirmed

  ScreenScaffold("Vocabulary", onBack) { padding ->
    Column(Modifier.padding(padding)) {
      LazyColumn(
        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
            for (l in Levels.vocabulary) {
              val v = Learning.vocabularyView(data, l)
              LevelTile(
                title = levelTitles.getValue(l),
                percentage = v.percentage(),
                locked = !(Learning.levelUnlocked(data, l) || l in confirmed),
                selected = l == level,
                modifier = Modifier.weight(1f),
                onClick = {
                  if (Learning.levelUnlocked(data, l) || l in confirmed) {
                    level = l
                    selected = emptySet()
                  } else {
                    pendingLevel = l
                  }
                },
              )
            }
          }
        }
        item {
          Text(
            "Niveau ${levelTitles.getValue(level)}: ${(view.percentage() * 100).toInt()}% (examen vanaf 70%)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        item { Text("Kies één of meer lijsten", style = MaterialTheme.typography.titleMedium) }
        items(data.lists, key = { it.id }) { list ->
          val checked = list.id in selected
          ChunkyCard(
            edge = if (checked) com.robpolus.commskills.ui.theme.Blue else Line,
            fill = if (checked) com.robpolus.commskills.ui.theme.BlueContainer else androidx.compose.ui.graphics.Color.White,
            onClick = { selected = if (checked) selected - list.id else selected + list.id },
          ) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
              Checkbox(checked = checked, onCheckedChange = { selected = if (it) selected + list.id else selected - list.id })
              Column(Modifier.weight(1f)) {
                Text(list.title, style = MaterialTheme.typography.titleSmall)
                Text(
                  "${list.words.size} woorden · ${(view.groupAverage(list.id) * 100).toInt()}%",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Box(Modifier.size(18.dp).clip(CircleShape).background(view.groupColor(list.id).color()))
              IconButton(onClick = { onOverview(level, list.id) }) {
                Icon(Icons.Filled.Info, contentDescription = "Woordenoverzicht")
              }
            }
          }
        }
        item {
          val mixedOpen = view.mixedUnlocked()
          ChunkyCard(
            edge = if (mixedOpen) com.robpolus.commskills.ui.theme.Teal else Line,
            fill = if (mixedOpen) com.robpolus.commskills.ui.theme.Teal.copy(alpha = 0.10f) else androidx.compose.ui.graphics.Color.White,
            onClick = if (mixedOpen && unlocked) ({ onPlay(level, null, direction) }) else null,
          ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
              Column(Modifier.weight(1f)) {
                Text("Gemixt", style = MaterialTheme.typography.titleSmall)
                Text(
                  if (mixedOpen) "Alle lijsten door elkaar" else "Speel eerst alle lijsten minstens één keer",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              if (!mixedOpen) Icon(Icons.Filled.Lock, contentDescription = "Vergrendeld")
            }
          }
        }
        if (level == Levels.VOCAB_TYPE) {
          item {
            Text("Richting", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              FilterChip(direction == DirectionChoice.EN_NL, { direction = DirectionChoice.EN_NL }, { Text("EN → NL") })
              FilterChip(direction == DirectionChoice.NL_EN, { direction = DirectionChoice.NL_EN }, { Text("NL → EN") })
              FilterChip(direction == DirectionChoice.MIXED, { direction = DirectionChoice.MIXED }, { Text("Door elkaar") })
            }
          }
        }
        item { Spacer(Modifier.height(8.dp)) }
      }

      Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val words = data.lists.filter { it.id in selected }.sumOf { it.words.size }
        ChunkyButton(
          text = when {
            selected.isEmpty() -> "Kies een lijst om te starten"
            selected.size == 1 -> "Oefenen (${minOf(words, SessionBuilder.VOCAB_SESSION_SIZE).let { if (it < words) "$it van $words" else "$words" }} woorden)"
            else -> "${selected.size} lijsten combineren (${minOf(words, SessionBuilder.VOCAB_SESSION_SIZE).let { if (it < words) "$it van $words" else "$words" }} woorden)"
          },
          onClick = { onPlay(level, data.lists.map { it.id }.filter { it in selected }, direction) },
          enabled = selected.isNotEmpty() && unlocked,
        )
        if (onExam != null) ChunkyOutlineButton(
          text = if (view.examAvailable()) "Examen doen" else "Examen vanaf 70% (nu ${(view.percentage() * 100).toInt()}%)",
          onClick = { onExam(level) },
          enabled = view.examAvailable() && unlocked,
        )
      }
    }
  }

  val pending = pendingLevel
  if (pending != null) {
    AlertDialog(
      onDismissRequest = { pendingLevel = null },
      title = { Text("Ben je zeker?") },
      text = { Text("Je bent mogelijk nog niet klaar voor dit niveau. Slaag eerst voor het examen van het vorige niveau om het te ontgrendelen.") },
      confirmButton = {
        TextButton(onClick = {
          confirmed = confirmed + pending
          level = pending
          selected = emptySet()
          pendingLevel = null
        }) { Text("Ja, toch openen") }
      },
      dismissButton = { TextButton(onClick = { pendingLevel = null }) { Text("Nee") } },
    )
  }
}

@Composable
private fun LevelTile(
  title: String,
  percentage: Double,
  locked: Boolean,
  selected: Boolean,
  modifier: Modifier,
  onClick: () -> Unit,
) {
  ChunkyCard(
    modifier = modifier,
    edge = if (selected) com.robpolus.commskills.ui.theme.Blue else Line,
    fill = if (selected) com.robpolus.commskills.ui.theme.BlueContainer else androidx.compose.ui.graphics.Color.White,
    onClick = onClick,
  ) {
    Column(Modifier.padding(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (locked) Icon(Icons.Filled.Lock, contentDescription = "Vergrendeld", modifier = Modifier.size(18.dp))
      }
      Spacer(Modifier.height(8.dp))
      LinearProgressIndicator(
        progress = { percentage.toFloat().coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().height(6.dp),
        trackColor = Line,
      )
      Spacer(Modifier.width(1.dp))
      Text(
        "${(percentage * 100).toInt()}%",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
      )
    }
  }
}
