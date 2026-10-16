package com.robpolus.commskills.data

import com.robpolus.commskills.domain.AppData
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Houdt alle app-gegevens in het geheugen ([data]) en bewaart elke wijziging in een bestand.
 * Een [onSaved]-haak laat de cloud-synchronisatie meeliften.
 */
class AppRepository(
  private val file: File,
  private val scope: CoroutineScope,
  private val clock: () -> Long = System::currentTimeMillis,
  var onSaved: (AppData) -> Unit = {},
) {
  private val state = MutableStateFlow(load())
  val data: StateFlow<AppData> = state.asStateFlow()

  private val writeLock = Mutex()

  private fun load(): AppData {
    if (!file.exists()) return AppData()
    return try {
      Codec.decode(file.readText())
    } catch (e: Exception) {
      file.renameTo(File(file.parentFile, file.name + ".kapot"))
      AppData()
    }
  }

  /** Pas de gegevens aan en bewaar ze. */
  fun update(transform: (AppData) -> AppData) {
    state.update { current ->
      val next = transform(current)
      if (next === current) current else next.copy(updatedAt = clock())
    }
    save()
  }

  /** Vervangt alles door gegevens uit de cloud. */
  fun replace(newData: AppData) {
    state.value = newData
    save(notify = false)
  }

  private fun save(notify: Boolean = true) {
    scope.launch(Dispatchers.IO) {
      writeLock.withLock {
        val latest = state.value // altijd het nieuwste wegschrijven
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(Codec.encode(latest))
        if (!tmp.renameTo(file)) {
          file.delete()
          tmp.renameTo(file)
        }
        if (notify) onSaved(latest)
      }
    }
  }
}
