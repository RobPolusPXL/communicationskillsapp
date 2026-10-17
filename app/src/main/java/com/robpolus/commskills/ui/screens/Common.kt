package com.robpolus.commskills.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.sp
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
    repeat(exam.maxLives) { i ->
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

/** Ronde voortgangsring met het percentage in het midden. */
@Composable
fun ProgressRing(progress: Float, color: Color, label: String, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 44.dp) {
  Box(
    modifier
      .size(size)
      .drawBehind {
        val stroke = 5.dp.toPx()
        val inset = stroke / 2
        val arcSize = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke)
        val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
        drawArc(Color(0xFFE8EBF2), 0f, 360f, false, topLeft, arcSize, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        if (progress > 0f) {
          drawArc(
            color, -90f, 360f * progress.coerceIn(0f, 1f), false, topLeft, arcSize,
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
          )
        }
      },
    contentAlignment = Alignment.Center,
  ) {
    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF44526A))
  }
}

/** Voortgangsbalk met een gestreepte lijn op 70% (de grens voor het examen). */
@Composable
fun ThresholdBar(progress: Float, color: Color, modifier: Modifier = Modifier) {
  Box(
    modifier
      .fillMaxWidth()
      .height(16.dp)
      .drawBehind {
        val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
        drawRoundRect(Color.White, cornerRadius = r)
        if (progress > 0f) {
          val w = (size.width * progress.coerceIn(0f, 1f)).coerceAtLeast(size.height)
          drawRoundRect(color, size = androidx.compose.ui.geometry.Size(w, size.height), cornerRadius = r)
        }
        val x = size.width * 0.7f
        drawLine(
          Color(0xFF243044).copy(alpha = 0.55f),
          androidx.compose.ui.geometry.Offset(x, -4.dp.toPx()),
          androidx.compose.ui.geometry.Offset(x, size.height + 4.dp.toPx()),
          strokeWidth = 3.dp.toPx(),
          pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
        )
      },
  )
}

/** Kaart bovenaan een niveau: titel, balk met examengrens en het percentage. */
@Composable
fun LevelProgressCard(title: String, progress: Double, color: Color, light: Color) {
  Column(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(light).padding(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
  ) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    ThresholdBar(progress.toFloat(), color)
    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
      Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = Color(0xFF44526A))
      Text("examen vanaf 70%", style = MaterialTheme.typography.labelMedium, color = Color(0xFF44526A))
    }
  }
}

/** Rond vinkje voor een gekozen item. */
@Composable
fun SelectTick(selected: Boolean, color: Color) {
  Box(
    Modifier.size(26.dp).clip(androidx.compose.foundation.shape.CircleShape)
      .background(if (selected) color else Color.White)
      .border(3.dp, if (selected) color else Line, androidx.compose.foundation.shape.CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    if (selected) Text("✓", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
  }
}

/** Vaste onderbalk met de hoofdknoppen. */
@Composable
fun BottomActions(content: @Composable ColumnScope.() -> Unit) {
  Column(
    Modifier.fillMaxWidth().background(Color.White)
      .drawBehind {
        drawLine(Color(0xFFE8EBF2), androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), strokeWidth = 4.dp.toPx())
      }
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    content = content,
  )
}
