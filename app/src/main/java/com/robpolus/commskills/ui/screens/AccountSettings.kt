package com.robpolus.commskills.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AccountSettings(configured: Boolean, email: String?, onSignIn: () -> Unit, onSignOut: () -> Unit) {
  Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Account", style = MaterialTheme.typography.titleMedium)
    when {
      !configured -> Text(
        "Inloggen is nog niet ingesteld: de app mist het bestand google-services.json (zie README, stap Firebase). Je voortgang staat voorlopig alleen op dit toestel.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      email != null -> {
        Text("Ingelogd als $email — je voortgang wordt online bewaard.")
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Uitloggen") }
      }
      else -> {
        Text("Niet ingelogd — je voortgang staat alleen op dit toestel.")
        OutlinedButton(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) { Text("Inloggen met Google") }
      }
    }
  }
}
