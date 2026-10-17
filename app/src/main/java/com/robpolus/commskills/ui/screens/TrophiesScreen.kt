package com.robpolus.commskills.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.robpolus.commskills.domain.AppData
import com.robpolus.commskills.domain.Trophies
import com.robpolus.commskills.ui.theme.Gold
import com.robpolus.commskills.ui.theme.Line

@Composable
fun TrophiesScreen(data: AppData, onBack: () -> Unit) {
  val trophies = Trophies.all(data)
  ScreenScaffold("Trofeeën", onBack) { padding ->
    LazyColumn(
      modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      item {
        Text(
          "${trophies.count { it.earned }} van ${trophies.size} behaald",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(vertical = 8.dp),
        )
      }
      items(trophies, key = { it.id }) { t ->
        ChunkyCard(
          edge = if (t.earned) Gold else Line,
          fill = if (t.earned) Color(0xFFFFF6D6) else Color.White,
        ) {
          Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Filled.EmojiEvents,
              contentDescription = null,
              tint = if (t.earned) Gold else Line,
              modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
              Text(t.title, style = MaterialTheme.typography.titleMedium)
              Text(
                t.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            if (t.earned && t.count > 0) {
              Text("×${t.count}", style = MaterialTheme.typography.titleMedium)
            }
          }
        }
      }
      item { Spacer(Modifier.size(16.dp)) }
    }
  }
}
