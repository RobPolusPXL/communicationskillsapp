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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

private val InkColor = Color(0xFF1B1F27)
private val SoftColor = Color(0xFF5C6573)
private val DayNames = listOf("ma", "di", "wo", "do", "vr", "za", "zo")

@Composable
fun HomeScreen(
  data: AppData,
  today: LocalDate,
  onVocabulary: () -> Unit,
  onGrammar: () -> Unit,
  onTrophies: () -> Unit,
  onSettings: () -> Unit,
  grammarPercentage: Double = 0.0,
) {
  val match = Learning.vocabularyView(data, Levels.VOCAB_MATCH).percentage()
  val type = Learning.vocabularyView(data, Levels.VOCAB_TYPE).percentage()
  val vocabAverage = (match + type) / 2
  Column(Modifier.fillMaxSize().systemBarsPadding()) {
    Column(
      Modifier.weight(1f).padding(horizontal = 20.dp).padding(top = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      val days = Stats.effectiveStreak(data.streak, today)
      Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFFFFE9CC)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Streak, modifier = Modifier.size(20.dp))
        Text(" $days ${if (days == 1) "dag" else "dagen"}", style = MaterialTheme.typography.titleSmall, color = Color(0xFF8A4B00))
      }

      WeekCard(data, today)

      Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.SpaceAround) {
        PathNode(
          "Vocabulary", "Matchen ${(match * 100).toInt()}% · Typen ${(type * 100).toInt()}%", "Aa",
          Blue, vocabAverage.toFloat(), Alignment.Start, onVocabulary,
        )
        PathNode(
          "Grammar", "Type 0 t/m 3 + passief · ${(grammarPercentage * 100).toInt()}%", "If",
          Orange2, grammarPercentage.toFloat(), Alignment.End, onGrammar,
        )
      }
    }
    BottomNav(onTrophies, onSettings)
  }
}

@Composable
private fun WeekCard(data: AppData, today: LocalDate) {
  val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
  val seconds = Stats.todaySeconds(data, today)
  val reached = seconds >= Stats.DAILY_GOAL_SECONDS
  Column(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Line).padding(bottom = 5.dp)
      .clip(RoundedCornerShape(18.dp)).background(Color.White).padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      for (i in 0 until 7) {
        val day = monday.plusDays(i.toLong())
        val done = (data.daily[day.toString()]?.activeSeconds ?: 0) > 0
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(DayNames[i], style = MaterialTheme.typography.labelMedium, color = SoftColor, fontWeight = FontWeight.ExtraBold)
          Box(
            Modifier.size(26.dp).clip(CircleShape)
              .background(if (done) Orange2 else Color.White)
              .then(if (day == today && !done) Modifier.background(Color.White) else Modifier)
              .drawBehind {
                if (!done) drawCircle(if (day == today) InkColor else Line, radius = size.minDimension / 2 - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
              },
          )
        }
      }
    }
    Text(
      if (reached) "Dagdoel gehaald! ${formatMinutes(seconds)} min" else "Vandaag ${formatMinutes(seconds)} van 10:00 min",
      style = MaterialTheme.typography.titleSmall,
      color = if (reached) Correct else SoftColor,
      modifier = Modifier.fillMaxWidth(),
      textAlign = TextAlign.Center,
    )
    Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFFE8EBF2))) {
      Box(
        Modifier.fillMaxWidth((seconds.toFloat() / Stats.DAILY_GOAL_SECONDS).coerceIn(0.02f, 1f)).height(14.dp)
          .clip(RoundedCornerShape(7.dp)).background(if (reached) Correct else Orange2),
      )
    }
  }
}

@Composable
private fun PathNode(
  title: String,
  subtitle: String,
  letters: String,
  color: Color,
  progress: Float,
  side: Alignment.Horizontal,
  onClick: () -> Unit,
) {
  Column(Modifier.fillMaxWidth(), horizontalAlignment = side) {
    Column(
      Modifier.padding(horizontal = 24.dp).clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        Modifier.size(112.dp).drawBehind {
          val stroke = 12.dp.toPx()
          val arc = Size(size.width - stroke, size.height - stroke)
          val tl = Offset(stroke / 2, stroke / 2)
          drawArc(Color(0xFFE8EBF2), 0f, 360f, false, tl, arc, style = Stroke(stroke))
          if (progress > 0f) drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, tl, arc, style = Stroke(stroke, cap = StrokeCap.Round))
        },
        contentAlignment = Alignment.Center,
      ) {
        Box(Modifier.size(78.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
          Text(letters, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Black)
        }
      }
      Spacer(Modifier.height(6.dp))
      Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
      Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = SoftColor)
    }
  }
}

@Composable
private fun BottomNav(onTrophies: () -> Unit, onSettings: () -> Unit) {
  Row(
    Modifier.fillMaxWidth().background(Color.White).drawBehind {
      drawLine(Line, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 2.dp.toPx())
    }.padding(vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceAround,
  ) {
    NavItem("Start", Icons.Filled.Home, true, null)
    NavItem("Trofeeën", Icons.Filled.EmojiEvents, false, onTrophies)
    NavItem("Instellingen", Icons.Filled.Settings, false, onSettings)
  }
}

@Composable
private fun NavItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: (() -> Unit)?) {
  val tint = if (selected) Blue else Color(0xFF8A93A3)
  Column(
    Modifier.clip(RoundedCornerShape(12.dp))
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
      .semantics(mergeDescendants = true) { contentDescription = label }
      .padding(horizontal = 18.dp, vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
    Text(label, style = MaterialTheme.typography.labelMedium, color = tint, fontWeight = FontWeight.ExtraBold)
  }
}

