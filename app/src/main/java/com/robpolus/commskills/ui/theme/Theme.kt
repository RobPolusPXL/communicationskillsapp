package com.robpolus.commskills.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
  primary = Blue,
  onPrimary = Color.White,
  primaryContainer = BlueContainer,
  onPrimaryContainer = BlueDark,
  background = Color.White,
  onBackground = Ink,
  surface = Color.White,
  onSurface = Ink,
  onSurfaceVariant = InkSoft,
  outline = Line,
  error = Wrong,
)

@Composable
fun CommSkillsTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = LightColors,
    typography = Typography(),
    content = content,
  )
}
