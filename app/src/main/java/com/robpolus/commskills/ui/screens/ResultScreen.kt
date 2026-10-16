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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robpolus.commskills.ui.ResultUi
import com.robpolus.commskills.ui.theme.Correct
import com.robpolus.commskills.ui.theme.Coral
import com.robpolus.commskills.ui.theme.CoralDark
import com.robpolus.commskills.ui.theme.Gold
import com.robpolus.commskills.ui.theme.Sun

private val Cream = Color(0xFFFFF6DF)

/** Kleurig eindscherm: groen bij gelukt, geel bij foutloos, rood bij een niet gehaald examen, anders blauw. */
@Composable
fun ResultScreen(result: ResultUi?, onDone: () -> Unit, onExam: ((String) -> Unit)? = null) {
  if (result == null) return
  val failedExam = result.isExam && result.title.startsWith("Examen niet")
  val bg = when {
    failedExam -> Coral
    result.perfect -> Sun
    result.isExam -> Correct
    else -> com.robpolus.commskills.ui.theme.Blue
  }
  val edge = when {
    failedExam -> CoralDark
    result.perfect -> Color(0xFFD98F00)
    result.isExam -> Color(0xFF237A47)
    else -> com.robpolus.commskills.ui.theme.BlueDark
  }
  val good = result.total - result.mistakes.size
  val percent = if (result.total == 0) 0 else (good * 100 / result.total)

  Column(Modifier.fillMaxSize().background(bg).systemBarsPadding().padding(20.dp)) {
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      item {
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Box(Modifier.size(150.dp).clip(CircleShape).background(Cream), contentAlignment = Alignment.Center) {
            Text("$percent%", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = bg)
          }
        }
        Spacer(Modifier.height(16.dp))
        Text(
          result.title,
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth(),
        )
        result.message?.let {
          Text(it, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color.White, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(8.dp))
        StatRow("Meteen goed", "$good / ${result.total}", edge)
        Spacer(Modifier.height(2.dp))
        StatRow("Fouten", "${result.mistakes.size}", edge)
        result.levelPercentage?.let {
          Spacer(Modifier.height(2.dp))
          StatRow("Niveau", "${(it * 100).toInt()}%" + if (result.examAvailable) " · examen open!" else "", edge)
        }
      }
      if (result.newTrophies.isNotEmpty()) {
        item { Text("Nieuwe trofeeën", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.padding(top = 12.dp)) }
        items(result.newTrophies, key = { it.id }) { t ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Gold)
            Text("  ${t.title}", style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
          }
        }
      }
      if (result.mistakes.isNotEmpty()) {
        item { Text("Om te herhalen", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.padding(top = 12.dp)) }
        items(result.mistakes, key = { it.key }) { e ->
          Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(12.dp)) {
            Text(e.title, style = MaterialTheme.typography.titleSmall, color = CoralDark, fontWeight = FontWeight.Bold)
            Text(e.detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }
    Spacer(Modifier.height(8.dp))
    if (result.examAvailable && onExam != null) {
      WhiteButton("Naar het examen", bg) { onExam(result.level) }
      Spacer(Modifier.height(10.dp))
    }
    WhiteButton("Klaar", bg, onDone)
  }
}

@Composable
private fun StatRow(label: String, value: String, edge: Color) {
  Row(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(edge.copy(alpha = 0.55f)).padding(horizontal = 16.dp, vertical = 14.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label.uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
    Text(value, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
  }
}

/** Witte knop met onderrand, in de stijl van de tegels. */
@Composable
private fun WhiteButton(text: String, textColor: Color, onClick: () -> Unit) {
  val shape = RoundedCornerShape(18.dp)
  Box(Modifier.fillMaxWidth().clip(shape).background(Color(0xFFD9DEE7)).clickable(onClick = onClick)) {
    Box(
      Modifier.fillMaxWidth().padding(bottom = 4.dp).clip(shape).background(Color.White).padding(vertical = 16.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(text.uppercase(), color = textColor, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
    }
  }
}
