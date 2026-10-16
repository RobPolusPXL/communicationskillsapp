package com.robpolus.commskills.ui.screens

import androidx.compose.foundation.layout.padding
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
fun ScreenScaffold(title: String, onBack: () -> Unit, content: @Composable (PaddingValues) -> Unit) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(title) },
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
