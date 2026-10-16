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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.robpolus.commskills.ui.theme.Blue
import com.robpolus.commskills.ui.theme.BlueDark
import com.robpolus.commskills.ui.theme.Purple
import com.robpolus.commskills.ui.theme.PurpleDark
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LinearProgressIndicator
import com.robpolus.commskills.domain.AppData
import com.robpolus.commskills.domain.Stats
import com.robpolus.commskills.ui.theme.Correct
import com.robpolus.commskills.ui.theme.Line
import java.time.LocalDate
import com.robpolus.commskills.ui.theme.Streak

@Composable
fun HomeScreen(
  data: AppData,
  today: LocalDate,
  onVocabulary: () -> Unit,
  onGrammar: () -> Unit,
  onTrophies: () -> Unit,
  onSettings: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .systemBarsPadding()
      .padding(horizontal = 20.dp, vertical = 12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Streak)
        val days = Stats.effectiveStreak(data.streak, today)
        Text(" $days ${if (days == 1) "dag" else "dagen"}", style = MaterialTheme.typography.titleMedium)
      }
      Row {
        IconButton(onClick = onTrophies) {
          Icon(Icons.Filled.EmojiEvents, contentDescription = "Trofeeën")
        }
        IconButton(onClick = onSettings) {
          Icon(Icons.Filled.Settings, contentDescription = "Instellingen")
        }
      }
    }

    Spacer(Modifier.height(8.dp))
    val seconds = Stats.todaySeconds(data, today)
    val reached = seconds >= Stats.DAILY_GOAL_SECONDS
    Text(
      if (reached) "Dagdoel gehaald! ${formatMinutes(seconds)} min" else "Dagdoel: ${formatMinutes(seconds)} / 10:00 min",
      style = MaterialTheme.typography.bodyMedium,
      color = if (reached) Correct else MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(6.dp))
    LinearProgressIndicator(
      progress = { (seconds.toFloat() / Stats.DAILY_GOAL_SECONDS).coerceIn(0f, 1f) },
      modifier = Modifier.fillMaxWidth().height(8.dp),
      color = if (reached) Correct else MaterialTheme.colorScheme.primary,
      trackColor = Line,
    )

    Spacer(Modifier.height(24.dp))
    RouteCard("Vocabulary", "Woorden leren: matchen en typen", Blue, BlueDark, onVocabulary)
    Spacer(Modifier.height(16.dp))
    RouteCard("Grammar", "If-zinnen: type 0, 1, 2 en 3", Purple, PurpleDark, onGrammar)
  }
}

@Composable
private fun RouteCard(title: String, subtitle: String, color: Color, edge: Color, onClick: () -> Unit) {
  val shape = RoundedCornerShape(20.dp)
  Box(Modifier.fillMaxWidth().clip(shape).background(edge).clickable(onClick = onClick)) {
    Column(Modifier.fillMaxWidth().padding(bottom = 5.dp).clip(shape).background(color).padding(22.dp)) {
      Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
      Spacer(Modifier.height(4.dp))
      Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
    }
  }
}
