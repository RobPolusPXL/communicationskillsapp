package com.robpolus.commskills.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.robpolus.commskills.domain.AppData
import com.robpolus.commskills.ui.ImportState

private val docxTypes = arrayOf(
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  "application/octet-stream",
)

@Composable
fun SettingsScreen(
  data: AppData,
  importState: ImportState?,
  onPickFile: (android.net.Uri) -> Unit,
  onRename: (String) -> Unit,
  onConfirmImport: () -> Unit,
  onCancelImport: () -> Unit,
  onBack: () -> Unit,
  extra: @Composable () -> Unit = {},
) {
  val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if (uri != null) onPickFile(uri)
  }
  ScreenScaffold("Instellingen", onBack, accent = com.robpolus.commskills.ui.theme.Teal) { padding ->
    LazyColumn(
      modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      item { extra() }
      item {
        Text("Woordenlijsten", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(4.dp))
        ChunkyButton("Nieuwe lijst importeren (Word)", onClick = { picker.launch(docxTypes) })
        Spacer(Modifier.height(8.dp))
      }
      items(data.lists, key = { it.id }) { list ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(list.title)
          Text("${list.words.size} woorden", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider()
      }
    }
  }

  when (importState) {
    is ImportState.Preview -> AlertDialog(
      onDismissRequest = onCancelImport,
      title = { Text("Lijst importeren") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = importState.imported.title,
            onValueChange = onRename,
            label = { Text("Naam van de lijst") },
            singleLine = true,
          )
          Text("${importState.imported.words.size} woorden gevonden.")
          if (importState.replacesExisting) {
            Text(
              "Er bestaat al een lijst met deze naam. De woorden worden vervangen; je voortgang blijft bewaard.",
              color = MaterialTheme.colorScheme.error,
            )
          }
          if (importState.imported.warnings.isNotEmpty()) {
            Text("Meldingen (${importState.imported.warnings.size}):", style = MaterialTheme.typography.titleSmall)
            importState.imported.warnings.take(8).forEach {
              Text("• $it", style = MaterialTheme.typography.bodySmall)
            }
            if (importState.imported.warnings.size > 8) Text("… en nog ${importState.imported.warnings.size - 8}")
          }
        }
      },
      confirmButton = { TextButton(onClick = onConfirmImport, enabled = importState.imported.title.isNotBlank()) { Text("Importeren") } },
      dismissButton = { TextButton(onClick = onCancelImport) { Text("Annuleren") } },
    )
    is ImportState.Failed -> AlertDialog(
      onDismissRequest = onCancelImport,
      title = { Text("Importeren mislukt") },
      text = { Text(importState.message) },
      confirmButton = { TextButton(onClick = onCancelImport) { Text("OK") } },
    )
    null -> {}
  }
}
