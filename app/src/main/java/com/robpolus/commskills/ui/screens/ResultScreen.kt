package com.robpolus.commskills.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.robpolus.commskills.ui.ResultUi
import com.robpolus.commskills.ui.theme.Correct
import com.robpolus.commskills.ui.theme.Gold
import com.robpolus.commskills.ui.theme.Wrong

@Composable
fun ResultScreen(result: ResultUi?, onDone: () -> Unit, onExam: ((String) -> Unit)? = null) {
  if (result == null) return
  Column(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp)) {
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      item {
        Text(
          result.title,
          style = MaterialTheme.typography.headlineMedium,
          color = if (result.perfect) Gold else MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth(),
        )
        Text(
          when {
            result.isExam -> "${result.mistakes.size} fout${if (result.mistakes.size == 1) "" else "e"} antwoord${if (result.mistakes.size == 1) "" else "en"}."
            result.perfect -> "100%: alle ${result.total} zonder fout."
            else -> "${result.total - result.mistakes.size} van ${result.total} meteen goed."
          },
          modifier = Modifier.fillMaxWidth(),
          textAlign = TextAlign.Center,
          style = MaterialTheme.typography.bodyLarge,
        )
        result.message?.let {
          Spacer(Modifier.height(8.dp))
          Text(it, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        }
        result.levelPercentage?.let {
          Spacer(Modifier.height(8.dp))
          Text(
            "Niveau: ${(it * 100).toInt()}%" + if (result.examAvailable) " — je mag het examen doen!" else "",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = if (result.examAvailable) Correct else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      if (result.newTrophies.isNotEmpty()) {
        item { Text("Nieuwe trofeeën", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
        items(result.newTrophies, key = { it.id }) { t ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Gold)
            Text("  ${t.title}", style = MaterialTheme.typography.bodyLarge)
          }
        }
      }
      if (result.mistakes.isNotEmpty()) {
        item { Text("Om te herhalen", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
        items(result.mistakes, key = { it.key }) { e ->
          Column {
            Text(e.title, style = MaterialTheme.typography.titleSmall, color = Wrong)
            Text(e.detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(Modifier.padding(top = 6.dp))
          }
        }
      }
    }
    Spacer(Modifier.height(8.dp))
    if (result.examAvailable && onExam != null) {
      Button(onClick = { onExam(result.level) }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Naar het examen") }
      Spacer(Modifier.height(8.dp))
    }
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Klaar") }
  }
}
