package com.robpolus.commskills.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.robpolus.commskills.ui.theme.Line
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import com.robpolus.commskills.domain.ListColor
import com.robpolus.commskills.domain.WordColor
import com.robpolus.commskills.ui.theme.Correct
import com.robpolus.commskills.ui.theme.Gold
import com.robpolus.commskills.ui.theme.Wrong

val Orange = Color(0xFFF59E0B)
val Grey = Color(0xFFB8BFCC)

fun ListColor.color(): Color = when (this) {
  ListColor.GREY -> Grey
  ListColor.RED -> Wrong
  ListColor.ORANGE -> Orange
  ListColor.GREEN -> Correct
  ListColor.GOLD -> Gold
}

fun WordColor.color(): Color = when (this) {
  WordColor.GREY -> Grey
  WordColor.RED -> Wrong
  WordColor.ORANGE -> Orange
  WordColor.GREEN -> Correct
}

/** Scherm met titelbalk en terugknop. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
  title: String,
  onBack: () -> Unit,
  accent: Color = com.robpolus.commskills.ui.theme.Blue,
  content: @Composable (PaddingValues) -> Unit,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(title) },
        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
          containerColor = accent,
          titleContentColor = Color.White,
          navigationIconContentColor = Color.White,
          actionIconContentColor = Color.White,
        ),
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug")
          }
        },
      )
    },
    content = content,
  )
}

fun formatMinutes(seconds: Int): String = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

/** Levens en voortgang bovenaan een examen. */
@Composable
fun ExamHearts(exam: com.robpolus.commskills.ui.ExamUi?) {
  if (exam == null) return
  androidx.compose.foundation.layout.Row(
    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    modifier = Modifier.padding(start = 12.dp),
  ) {
    repeat(3) { i ->
      Icon(
        androidx.compose.material.icons.Icons.Filled.Favorite,
        contentDescription = null,
        tint = if (i < exam.lives) Wrong else Grey,
        modifier = Modifier.padding(horizontal = 1.dp),
      )
    }
  }
}


private val ChunkyShape = RoundedCornerShape(18.dp)

/** Knop met een dikke onderrand (voelt ingedrukt bij het tikken). */
@Composable
fun ChunkyButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  color: Color = com.robpolus.commskills.ui.theme.Blue,
  edge: Color = com.robpolus.commskills.ui.theme.BlueDark,
  textColor: Color = Color.White,
  enabled: Boolean = true,
) {
  val c = if (enabled) color else Color(0xFFE8EBF2)
  val e = if (enabled) edge else com.robpolus.commskills.ui.theme.EdgeGrey
  val t = if (enabled) textColor else Color(0xFF9AA3B2)
  Box(modifier.fillMaxWidth().clip(ChunkyShape).background(e).clickable(enabled = enabled, onClick = onClick)) {
    Box(
      Modifier.fillMaxWidth().padding(bottom = 5.dp).clip(ChunkyShape).background(c).padding(vertical = 15.dp, horizontal = 12.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(text, color = t, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    }
  }
}

/** Lichte knop (wit met grijze onderrand) voor secundaire acties. */
@Composable
fun ChunkyOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) =
  ChunkyButton(
    text, onClick, modifier,
    color = Color.White,
    edge = com.robpolus.commskills.ui.theme.EdgeGrey,
    textColor = com.robpolus.commskills.ui.theme.Blue,
    enabled = enabled,
  )

/** Kaart met dikke onderrand; [edge] kleurt rand en onderkant, [fill] de vulling. */
@Composable
fun ChunkyCard(
  modifier: Modifier = Modifier,
  edge: Color = Line,
  fill: Color = Color.White,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val base = modifier.fillMaxWidth().clip(ChunkyShape).background(edge)
  Box(if (onClick != null) base.clickable(onClick = onClick) else base) {
    Column(
      Modifier.fillMaxWidth().padding(bottom = 4.dp).clip(ChunkyShape).background(Color.White).background(fill).border(2.dp, edge, ChunkyShape),
      content = content,
    )
  }
}
