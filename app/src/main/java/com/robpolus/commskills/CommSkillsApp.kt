package com.robpolus.commskills

import android.app.Application
import com.robpolus.commskills.data.AppRepository
import com.robpolus.commskills.data.BundledLists
import com.robpolus.commskills.data.GrammarParser
import com.robpolus.commskills.domain.GrammarExercise
import com.robpolus.commskills.notify.ReminderScheduler
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CommSkillsApp : Application() {

  val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  lateinit var repository: AppRepository
    private set

  /** De bundel-oefeningen voor Grammar (if-zinnen). */
  val grammar: List<GrammarExercise> by lazy {
    try {
      assets.open("grammar/conditionals.json").use { GrammarParser.parse(it.readBytes().toString(Charsets.UTF_8)) }
    } catch (e: Exception) {
      emptyList()
    }
  }

  override fun onCreate() {
    super.onCreate()
    if (::repository.isInitialized) return // onCreate mag veilig twee keer aangeroepen worden (bv. in tests)
    repository = AppRepository(File(filesDir, "appdata.json"), appScope)
    appScope.launch(Dispatchers.IO) {
      repository.update { BundledLists.seed(assets, it) }
      ReminderScheduler.schedule(this@CommSkillsApp, repository.data.value)
    }
  }
}
