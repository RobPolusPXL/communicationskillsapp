package com.robpolus.commskills.domain

data class TrophyStatus(
  val id: String,
  val title: String,
  val description: String,
  val earned: Boolean,
  /** Teller voor trofeeën die meerdere keren behaald worden; anders 0. */
  val count: Int = 0,
)

/** Trofeeën worden altijd afgeleid uit de bewaarde gegevens; er wordt niets apart opgeslagen. */
object Trophies {
  val streakThresholds = listOf(3, 7, 14, 30, 60, 100, 200, 365)

  val levelTitles = mapOf(
    Levels.VOCAB_MATCH to "Woorden matchen",
    Levels.VOCAB_TYPE to "Woorden typen",
    Levels.GRAMMAR to "If-zinnen",
  )

  fun all(data: AppData): List<TrophyStatus> {
    val list = mutableListOf<TrophyStatus>()
    val goalDays = data.daily.values.count { it.activeSeconds >= Stats.DAILY_GOAL_SECONDS }
    list += TrophyStatus("daily_goal", "Dagdoel", "10 minuten actief geoefend op één dag", goalDays > 0, goalDays)
    for (n in streakThresholds) {
      list += TrophyStatus("streak_$n", "Streak $n", "$n dagen na elkaar geoefend", data.streak.longest >= n)
    }
    for ((level, title) in levelTitles) {
      list += TrophyStatus("level_$level", "Niveau: $title", "70% gehaald in $title", level in data.levelReached)
      list += TrophyStatus("exam_$level", "Examen: $title", "Examen geslaagd voor $title", level in data.examPassed)
    }
    val medals = data.medals.size
    list += TrophyStatus("medal", "Medaille", "Een deel van een lijst met een medaille afgerond", medals > 0, medals)
    val perfect = data.sessions.values.sumOf { it.perfectCount }
    list += TrophyStatus("perfect", "100%", "Een lijst of type foutloos gespeeld", perfect > 0, perfect)
    return list
  }

  fun earnedIds(data: AppData): Set<String> = all(data).filter { it.earned }.map { it.id }.toSet()

  /** Trofeeën die er na een wijziging bij gekomen zijn (of waarvan de teller opliep). */
  fun newlyEarned(before: AppData, after: AppData): List<TrophyStatus> {
    val old = all(before).associateBy { it.id }
    return all(after).filter { new ->
      val prev = old[new.id]
      new.earned && (prev == null || !prev.earned || new.count > prev.count)
    }
  }
}
