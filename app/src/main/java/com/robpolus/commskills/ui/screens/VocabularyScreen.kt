package com.robpolus.commskills.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.robpolus.commskills.domain.AppData
import com.robpolus.commskills.domain.DirectionChoice
import com.robpolus.commskills.domain.Learning
import com.robpolus.commskills.domain.Levels
import com.robpolus.commskills.domain.ListColor
import com.robpolus.commskills.domain.SessionBuilder
import com.robpolus.commskills.ui.theme.Blue
import com.robpolus.commskills.ui.theme.BlueContainer
import com.robpolus.commskills.ui.theme.BlueDark
import com.robpolus.commskills.ui.theme.Line
import com.robpolus.commskills.ui.theme.Teal

private val levelTitles = mapOf(
  Levels.VOCAB_MATCH to "Matchen",
  Levels.VOCAB_TYPE to "Typen",
)

private val TealLight = Color(0xFFDCF4F1)

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
        modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
            for (l in Levels.vocabulary) {
              val v = Learning.vocabularyView(data, l)
              val open = Learning.levelUnlocked(data, l) || l in confirmed
              LevelTab(
                title = levelTitles.getValue(l),
                percentage = v.percentage(),
                locked = !open,
                passed = l in data.examPassed,
                selected = l == level,
                modifier = Modifier.weight(1f),
                onClick = {
                  if (open) {
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
        item { LevelProgressCard("Niveau ${levelTitles.getValue(level)}", view.percentage(), Blue, BlueContainer) }
        if (level == Levels.VOCAB_TYPE) {
          item {
            Text("Richting", style = MaterialTheme.typography.titleSmall, color = Color(0xFF44526A))
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              DirectionChip("EN → NL", direction == DirectionChoice.EN_NL, Modifier.weight(1f)) { direction = DirectionChoice.EN_NL }
              DirectionChip("NL → EN", direction == DirectionChoice.NL_EN, Modifier.weight(1f)) { direction = DirectionChoice.NL_EN }
              DirectionChip("Door elkaar", direction == DirectionChoice.MIXED, Modifier.weight(1f)) { direction = DirectionChoice.MIXED }
            }
          }
        }
        item { Text("Kies één of meer lijsten", style = MaterialTheme.typography.titleSmall, color = Color(0xFF44526A)) }
        items(data.lists, key = { it.id }) { list ->
          val checked = list.id in selected
          val color = view.groupColor(list.id)
          val perfect = color == ListColor.GOLD
          val played = color != ListColor.GREY
          ChunkyCard(
            edge = if (checked) Blue else Line,
            fill = if (checked) BlueContainer else Color.White,
            onClick = { selected = if (checked) selected - list.id else selected + list.id },
          ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 26.dp), verticalAlignment = Alignment.CenterVertically) {
              val avg = view.groupAverage(list.id)
              ProgressRing(avg.toFloat(), color.color(), "${(avg * 100).toInt()}%")
              Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(list.title, style = MaterialTheme.typography.titleSmall)
                Text(
                  "${list.words.size} woorden" + when {
                    perfect -> " · foutloos"
                    !played -> " · nog niet gespeeld"
                    else -> ""
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              IconButton(onClick = { onOverview(level, list.id) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Info, contentDescription = "Woordenoverzicht", tint = Color(0xFF9AA3B2))
              }
              SelectTick(checked, Blue)
            }
          }
        }
        item {
          val mixedOpen = view.mixedUnlocked()
          ChunkyCard(
            edge = if (mixedOpen) Teal else Line,
            fill = if (mixedOpen) TealLight else Color.White,
            onClick = if (mixedOpen && unlocked) ({ onPlay(level, null, direction) }) else null,
          ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 26.dp), verticalAlignment = Alignment.CenterVertically) {
              Column(Modifier.weight(1f)) {
                Text("Gemixt", style = MaterialTheme.typography.titleSmall)
                Text(
                  if (mixedOpen) "Alle lijsten door elkaar" else "Speel eerst alle lijsten minstens één keer",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              if (!mixedOpen) Icon(Icons.Filled.Lock, contentDescription = "Vergrendeld", tint = Color(0xFF9AA3B2))
            }
          }
        }
        item { Box(Modifier.size(4.dp)) }
      }

      BottomActions {
        val words = data.lists.filter { it.id in selected }.sumOf { it.words.size }
        val shown = minOf(words, SessionBuilder.VOCAB_SESSION_SIZE).let { if (it < words) "$it van $words" else "$words" }
        ChunkyButton(
          text = when {
            selected.isEmpty() -> "Kies een lijst om te starten"
            selected.size == 1 -> "Oefenen ($shown woorden)"
            else -> "${selected.size} lijsten combineren ($shown woorden)"
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
private fun LevelTab(
  title: String,
  percentage: Double,
  locked: Boolean,
  passed: Boolean,
  selected: Boolean,
  modifier: Modifier,
  onClick: () -> Unit,
) {
  ChunkyCard(
    modifier = modifier,
    edge = if (selected) Blue else Line,
    fill = if (selected) BlueContainer else Color.White,
    onClick = onClick,
  ) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = if (selected) BlueDark else Color(0xFF243044), modifier = Modifier.weight(1f))
        if (locked) Icon(Icons.Filled.Lock, contentDescription = "Vergrendeld", modifier = Modifier.size(16.dp), tint = Color(0xFF9AA3B2))
      }
      Text(
        "${(percentage * 100).toInt()}%" + when {
          passed -> " · geslaagd"
          locked -> " · vergrendeld"
          else -> ""
        },
        style = MaterialTheme.typography.labelMedium,
        color = Color(0xFF667085),
      )
      Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE8EBF2))) {
        Box(
          Modifier.fillMaxWidth(percentage.toFloat().coerceIn(0f, 1f)).height(8.dp)
            .clip(RoundedCornerShape(4.dp)).background(if (passed) com.robpolus.commskills.ui.theme.Correct else Blue),
        )
      }
    }
  }
}

@Composable
private fun DirectionChip(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
  val shape = RoundedCornerShape(12.dp)
  Box(
    modifier
      .clip(shape)
      .background(if (selected) BlueContainer else Color.White)
      .border(2.dp, if (selected) Blue else Line, shape)
      .clickable(onClick = onClick)
      .padding(vertical = 9.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text,
      style = MaterialTheme.typography.labelLarge,
      color = if (selected) BlueDark else Color(0xFF667085),
      textAlign = TextAlign.Center,
      fontWeight = FontWeight.ExtraBold,
    )
  }
}
