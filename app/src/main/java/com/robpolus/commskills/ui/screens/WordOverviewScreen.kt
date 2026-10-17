package com.robpolus.commskills.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.robpolus.commskills.domain.AppData
import com.robpolus.commskills.domain.Learning

/** Alle woorden van een lijst, per woord rood/oranje/groen volgens de voortgang in dit niveau. */
@Composable
fun WordOverviewScreen(data: AppData, level: String, listId: String, onBack: () -> Unit, onRemoveExtra: (String, String) -> Unit = { _, _ -> }) {
  val list = data.lists.firstOrNull { it.id == listId }
  val view = Learning.vocabularyView(data, level)
  ScreenScaffold(list?.title ?: "Lijst", onBack) { padding ->
    if (list == null) return@ScreenScaffold
    LazyColumn(Modifier.padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      items(list.words, key = { it.id }) { word ->
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
          Box(Modifier.size(14.dp).clip(CircleShape).background(view.wordColor(list.id, word.id).color()))
          Column(Modifier.padding(start = 12.dp)) {
            Text(
              word.en + (word.type?.let { "  ($it)" } ?: ""),
              style = MaterialTheme.typography.titleSmall,
            )
            Text(
              word.nl.joinToString(", "),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            for (dir in listOf("nl", "en")) {
              val key = AppData.extraKey(dir, list.id, word.id)
              for (extra in data.extraAnswers[key].orEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text("Ook goed: $extra", style = MaterialTheme.typography.bodySmall, color = com.robpolus.commskills.ui.theme.Correct)
                  androidx.compose.material3.TextButton(onClick = { onRemoveExtra(key, extra) }) { Text("Verwijder") }
                }
              }
            }
          }
        }
        HorizontalDivider()
      }
    }
  }
}
