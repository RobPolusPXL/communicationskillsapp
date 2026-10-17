package com.robpolus.commskills.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.robpolus.commskills.R

val Nunito = FontFamily(
  Font(R.font.nunito_regular, FontWeight.Normal),
  Font(R.font.nunito_bold, FontWeight.Medium),
  Font(R.font.nunito_bold, FontWeight.SemiBold),
  Font(R.font.nunito_bold, FontWeight.Bold),
  Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
  Font(R.font.nunito_black, FontWeight.Black),
)

private val base = Typography()

val AppTypography = Typography(
  displayLarge = base.displayLarge.copy(fontFamily = Nunito),
  displayMedium = base.displayMedium.copy(fontFamily = Nunito),
  displaySmall = base.displaySmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Black),
  headlineLarge = base.headlineLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.Black),
  headlineMedium = base.headlineMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Black),
  headlineSmall = base.headlineSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Black),
  titleLarge = base.titleLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.Black),
  titleMedium = base.titleMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold),
  titleSmall = base.titleSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold),
  bodyLarge = base.bodyLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.Medium),
  bodyMedium = base.bodyMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Medium),
  bodySmall = base.bodySmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Medium),
  labelLarge = base.labelLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold),
  labelMedium = base.labelMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
  labelSmall = base.labelSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
)
