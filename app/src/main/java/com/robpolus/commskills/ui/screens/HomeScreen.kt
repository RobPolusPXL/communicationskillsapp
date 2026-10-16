package com.robpolus.commskills.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.robpolus.commskills.domain.AppData
import com.robpolus.commskills.domain.Learning
import com.robpolus.commskills.domain.Levels
import com.robpolus.commskills.domain.Stats
import com.robpolus.commskills.ui.theme.Blue
import com.robpolus.commskills.ui.theme.BlueDark
import com.robpolus.commskills.ui.theme.Correct
import com.robpolus.commskills.ui.theme.Line
import com.robpolus.commskills.ui.theme.Orange2
import com.robpolus.commskills.ui.theme.Orange2Dark
import com.robpolus.commskills.ui.theme.Streak
import java.time.LocalDate

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
        Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Streak, modifier = Modifier.size(28.dp))
        val days = Stats.effectiveStreak(data.streak, today)
        Text(
          " $days ${if (days == 1) "dag" else "dagen"}",
          style = MaterialTheme.typography.titleLarge,
          color = Streak,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        RoundIcon(Color(0xFFFFF1C2), "Trofeeën", onTrophies) {
          Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Color(0xFFE0A000))
        }
        RoundIcon(Color(0xFFD9F3EF), "Instellingen", onSettings) {
          Icon(Icons.Filled.Settings, contentDescription = null, tint = Color(0xFF0C7D70))
        }
      }
    }

    Spacer(Modifier.height(18.dp))
    val seconds = Stats.todaySeconds(data, today)
    val reached = seconds >= Stats.DAILY_GOAL_SECONDS
    Text(
      if (reached) "Dagdoel gehaald! ${formatMinutes(seconds)} min" else "Dagdoel: ${formatMinutes(seconds)} / 10:00 min",
      style = MaterialTheme.typography.titleSmall,
      color = if (reached) Correct else MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(6.dp))
    Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(Line)) {
      Box(
        Modifier
          .fillMaxWidth((seconds.toFloat() / Stats.DAILY_GOAL_SECONDS).coerceIn(0.02f, 1f))
          .height(14.dp)
          .clip(RoundedCornerShape(7.dp))
          .background(if (reached) Correct else Orange2),
      )
    }

    Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
      val match = Learning.vocabularyView(data, Levels.VOCAB_MATCH).percentage()
      val type = Learning.vocabularyView(data, Levels.VOCAB_TYPE).percentage()
      RouteCard(
        "Vocabulary", "Woorden leren: matchen en typen",
        "Matchen ${(match * 100).toInt()}% · Typen ${(type * 100).toInt()}%",
        Blue, BlueDark, onVocabulary,
      )
      Spacer(Modifier.height(20.dp))
      RouteCard("Grammar", "If-zinnen: type 0, 1, 2 en 3", null, Orange2, Orange2Dark, onGrammar)
    }
  }
}

@Composable
private fun RoundIcon(color: Color, description: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
  Box(
    Modifier.size(44.dp).clip(CircleShape).background(color).clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) { icon() }
}

@Composable
private fun RouteCard(title: String, subtitle: String, badge: String?, color: Color, edge: Color, onClick: () -> Unit) {
  val shape = RoundedCornerShape(24.dp)
  Box(Modifier.fillMaxWidth().clip(shape).background(edge).clickable(onClick = onClick)) {
    Column(
      Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(shape).background(color).padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(title, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Black)
      Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.92f))
      if (badge != null) {
        Spacer(Modifier.height(8.dp))
        Text(
          badge,
          style = MaterialTheme.typography.labelLarge,
          color = Color.White,
          modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.25f)).padding(horizontal = 12.dp, vertical = 4.dp),
        )
      }
    }
  }
}
