package com.robpolus.commskills.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Tijdelijk scherm: de echte Google-login volgt in een volgende stap. */
@Composable
fun LoginScreen(onContinue: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .systemBarsPadding()
      .padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("Comm Skills", style = MaterialTheme.typography.displaySmall)
    Spacer(Modifier.height(8.dp))
    Text(
      "Leer de woorden en grammatica van Communication Skills 2.",
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(32.dp))
    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
      Text("Doorgaan")
    }
    Spacer(Modifier.height(8.dp))
    Text(
      "Inloggen met Google komt in een volgende stap.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
