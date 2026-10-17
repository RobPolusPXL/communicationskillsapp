package com.robpolus.commskills.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.robpolus.commskills.CommSkillsApp
import com.robpolus.commskills.domain.AppData
import com.robpolus.commskills.importer.DocxListImporter
import com.robpolus.commskills.notify.ReminderScheduler
import com.robpolus.commskills.importer.ImportException
import com.robpolus.commskills.importer.ImportedList
import com.robpolus.commskills.importer.ListMerge
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Wat de gebruiker te zien krijgt na het kiezen van een Word-bestand. */
sealed class ImportState {
  data class Preview(val imported: ImportedList, val replacesExisting: Boolean) : ImportState()
  data class Failed(val message: String) : ImportState()
}

open class AppViewModel(application: Application) : AndroidViewModel(application) {

  protected val repository = (application as CommSkillsApp).repository

  val data: StateFlow<AppData> = repository.data

  fun today(): LocalDate = LocalDate.now()

  fun update(transform: (AppData) -> AppData) = repository.update(transform)

  // --- herinneringen -----------------------------------------------------------------------------

  fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
    repository.update { it.copy(settings = it.settings.copy(reminderEnabled = enabled, reminderHour = hour, reminderMinute = minute)) }
    ReminderScheduler.schedule(getApplication(), repository.data.value)
  }

  // --- lijsten importeren ------------------------------------------------------------------------

  private val _import = MutableStateFlow<ImportState?>(null)
  val importState: StateFlow<ImportState?> = _import

  fun importDocx(uri: Uri) {
    viewModelScope.launch {
      _import.value = withContext(Dispatchers.IO) {
        try {
          val resolver = getApplication<Application>().contentResolver
          val name = resolver.query(uri, null, null, null, null)?.use { c ->
            val col = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (col >= 0 && c.moveToFirst()) c.getString(col) else null
          } ?: "nieuwe_lijst.docx"
          val imported = resolver.openInputStream(uri)!!.use {
            DocxListImporter.parse(it, DocxListImporter.titleFromFileName(name))
          }
          ImportState.Preview(imported, ListMerge.exists(data.value, imported.title))
        } catch (e: ImportException) {
          ImportState.Failed(e.message ?: "Importeren is mislukt.")
        } catch (e: Exception) {
          ImportState.Failed("Het bestand kon niet gelezen worden.")
        }
      }
    }
  }

  fun renameImport(title: String) {
    val state = _import.value as? ImportState.Preview ?: return
    _import.value = state.copy(
      imported = state.imported.copy(title = title),
      replacesExisting = ListMerge.exists(data.value, title),
    )
  }

  fun confirmImport() {
    val state = _import.value as? ImportState.Preview ?: return
    repository.update { ListMerge.apply(it, state.imported) }
    _import.value = null
  }

  fun cancelImport() {
    _import.value = null
  }
}
