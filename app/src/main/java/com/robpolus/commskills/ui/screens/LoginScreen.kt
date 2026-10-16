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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Wordt enkel getoond als Firebase ingesteld is en je nog niet ingelogd bent. */
@Composable
fun LoginScreen(error: String?, busy: Boolean, onGoogle: () -> Unit, onLocal: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
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
    Button(onClick = onGoogle, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
      Text(if (busy) "Bezig…" else "Inloggen met Google")
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = onLocal, enabled = !busy, modifier = Modifier.fillMaxWidth().height(48.dp)) {
      Text("Zonder account (enkel op dit toestel)")
    }
    if (error != null) {
      Spacer(Modifier.height(12.dp))
      Text(error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
    }
  }
}
