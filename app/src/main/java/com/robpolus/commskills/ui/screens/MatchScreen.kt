package com.robpolus.commskills.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.robpolus.commskills.ui.theme.Blue
import com.robpolus.commskills.ui.theme.BlueDark
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.robpolus.commskills.domain.Side
import com.robpolus.commskills.ui.MatchUi
import com.robpolus.commskills.ui.Tile
import com.robpolus.commskills.ui.theme.Correct
import com.robpolus.commskills.ui.theme.Line
import com.robpolus.commskills.ui.theme.Wrong

@Composable
fun MatchScreen(
  ui: MatchUi?,
  onSelect: (Side, String) -> Unit,
  onQuit: () -> Unit,
  topExtra: @Composable () -> Unit = {},
) {
  var confirmQuit by remember { mutableStateOf(false) }
  var showDefinition by remember { mutableStateOf(false) }
  BackHandler { confirmQuit = true }
  if (ui == null) return

  Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = { confirmQuit = true }) { Icon(Icons.Filled.Close, contentDescription = "Stoppen") }
      LinearProgressIndicator(
        progress = { ((ui.round - 1).toFloat() / ui.rounds).coerceIn(0f, 1f) },
        modifier = Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)),
        color = Correct,
        trackColor = Line,
      )
      topExtra()
    }
    Spacer(Modifier.height(8.dp))
    Text(
      if (ui.repeat) "Herhaling van foute woorden" else "Verbind de woorden die bij elkaar horen",
      style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.height(12.dp))
    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      TileColumn(ui.en, ui.selectedEn, ui.flash?.first, Modifier.weight(1f)) { onSelect(Side.EN, it) }
      TileColumn(ui.nl, ui.selectedNl, ui.flash?.second, Modifier.weight(1f)) { onSelect(Side.NL, it) }
    }
    Spacer(Modifier.height(8.dp))
    TextButton(
      onClick = { showDefinition = true },
      enabled = ui.selectedDefinition != null,
      modifier = Modifier.align(Alignment.CenterHorizontally),
    ) {
      Icon(Icons.Filled.Info, contentDescription = null)
      Text("  Uitleg van het gekozen Engelse woord")
    }
  }

  if (showDefinition) {
    AlertDialog(
      onDismissRequest = { showDefinition = false },
      title = { Text("Uitleg") },
      text = { Text(ui.selectedDefinition?.ifBlank { "Geen uitleg beschikbaar." } ?: "Kies eerst een Engels woord.") },
      confirmButton = { TextButton(onClick = { showDefinition = false }) { Text("OK") } },
    )
  }
  if (confirmQuit) {
    AlertDialog(
      onDismissRequest = { confirmQuit = false },
      title = { Text("Stoppen?") },
      text = { Text("Je voortgang tot nu toe blijft bewaard.") },
      confirmButton = { TextButton(onClick = { confirmQuit = false; onQuit() }) { Text("Stoppen") } },
      dismissButton = { TextButton(onClick = { confirmQuit = false }) { Text("Verder spelen") } },
    )
  }
}

@Composable
private fun TileColumn(
  tiles: List<Tile>,
  selected: String?,
  flash: String?,
  modifier: Modifier,
  onClick: (String) -> Unit,
) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
    for (tile in tiles) {
      val isSelected = tile.id == selected
      val isFlash = tile.id == flash
      val border = when {
        tile.matched -> Correct
        isFlash -> Wrong
        isSelected -> Blue
        else -> Line
      }
      val container = when {
        tile.matched -> Correct.copy(alpha = 0.12f)
        isFlash -> Wrong.copy(alpha = 0.12f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.White
      }
      val shape = RoundedCornerShape(16.dp)
      val edge = when {
        tile.matched -> Correct
        isFlash -> Wrong
        isSelected -> Blue
        else -> Line
      }
      Box(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(shape).background(edge)
          .clickable(enabled = !tile.matched) { onClick(tile.id) },
      ) {
        Box(
          Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(bottom = 4.dp).clip(shape)
            .background(container).border(2.dp, border, shape).padding(12.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            tile.text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected || tile.matched) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            color = when {
              tile.matched -> Correct
              isFlash -> Wrong
              isSelected -> BlueDark
              else -> MaterialTheme.colorScheme.onSurface
            },
          )
        }
      }
    }
  }
}
