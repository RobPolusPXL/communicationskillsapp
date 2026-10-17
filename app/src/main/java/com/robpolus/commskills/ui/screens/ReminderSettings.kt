package com.robpolus.commskills.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.robpolus.commskills.domain.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettings(settings: Settings, onChange: (enabled: Boolean, hour: Int, minute: Int) -> Unit) {
  var picking by remember { mutableStateOf(false) }
  val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    onChange(granted, settings.reminderHour, settings.reminderMinute)
  }
  val time = "%02d:%02d".format(settings.reminderHour, settings.reminderMinute)

  Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Dagelijkse herinnering", style = MaterialTheme.typography.titleMedium)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
      Text("Herinner mij om te oefenen")
      Switch(
        checked = settings.reminderEnabled,
        onCheckedChange = { on ->
          if (on && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
          else onChange(on, settings.reminderHour, settings.reminderMinute)
        },
      )
    }
    OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) { Text("Tijdstip: $time") }
    Text(
      "Je krijgt enkel een melding als je die dag nog niet geoefend hebt.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }

  if (picking) {
    val state = rememberTimePickerState(settings.reminderHour, settings.reminderMinute, is24Hour = true)
    AlertDialog(
      onDismissRequest = { picking = false },
      title = { Text("Kies een tijdstip") },
      text = { TimePicker(state = state) },
      confirmButton = {
        TextButton(onClick = {
          picking = false
          onChange(settings.reminderEnabled, state.hour, state.minute)
        }) { Text("OK") }
      },
      dismissButton = { TextButton(onClick = { picking = false }) { Text("Annuleren") } },
    )
  }
}
