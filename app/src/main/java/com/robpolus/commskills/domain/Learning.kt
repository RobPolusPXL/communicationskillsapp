package com.robpolus.commskills.domain

/**
 * Alle regels rond voortgang, kleuren en ontgrenden, als zuivere functies op [AppData].
 * [groups] = per groep (lijst of if-type) de ids van de items in dat niveau.
 */
class LevelView(
  val level: String,
  val groups: Map<String, List<String>>,
  val data: AppData,
) {
  private fun progress(group: String, item: String) =
    data.progress[AppData.progressKey(level, group, item)]

  fun score(group: String, item: String): Double = progress(group, item)?.score ?: 0.0

  fun played(group: String, item: String): Boolean = progress(group, item)?.played == true

  fun wordColor(group: String, item: String): WordColor =
    Scoring.wordColor(score(group, item), played(group, item))

  /** Een woord is gekend na twee juiste antwoorden na elkaar. */
  fun known(group: String, item: String): Boolean = (progress(group, item)?.streak ?: 0) >= Scoring.KNOWN_STREAK

  fun groupAverage(group: String): Double =
    groups[group].orEmpty().let { ids -> if (ids.isEmpty()) 0.0 else ids.map { score(group, it) }.average() }

  fun groupSession(group: String): SessionStat = data.sessions[AppData.sessionKey(level, group)] ?: SessionStat()

  /** Een groep telt als gespeeld zodra een volledige sessie ervan afgerond is. */
  fun groupPlayed(group: String): Boolean = groupSession(group).completed > 0

  fun groupColor(group: String): ListColor {
    val session = groupSession(group)
    val anyPlayed = session.completed > 0 || groups[group].orEmpty().any { played(group, it) }
    return Scoring.listColor(groupAverage(group), anyPlayed, session.lastPerfect)
  }

  /** Gemiddelde woordscore over het hele niveau; ongespeelde woorden tellen als 0. */
  fun percentage(): Double {
    val all = groups.flatMap { (group, ids) -> ids.map { group to it } }
    if (all.isEmpty()) return 0.0
    return if (Levels.isVocabulary(level)) all.count { (g, i) -> known(g, i) }.toDouble() / all.size
    else Scoring.levelPercentage(all.map { (g, i) -> score(g, i) })
  }

  /** Woordniveaus: minstens één deel is groen. Grammar: het niveaupercentage haalt 70%. */
  fun examAvailable(): Boolean =
    if (Levels.isVocabulary(level)) Parts.all(data).any { Parts.ready(this, it) } else Scoring.examAvailable(percentage())

  /** "Gemixt" gaat open nadat alle groepen minstens één keer gespeeld zijn. */
  fun mixedUnlocked(): Boolean = Unlocking.mixedUnlocked(groups.keys.map { groupPlayed(it) })

  /** Items met een gewicht dat bij lage scores hoger is (zie [Scoring.weight]). */
  fun weight(group: String, item: String): Double = Scoring.weight(score(group, item))
}

object Learning {

  fun levelUnlocked(data: AppData, level: String): Boolean = when (level) {
    Levels.VOCAB_TYPE -> Unlocking.nextLevelUnlocked(Levels.VOCAB_MATCH in data.examPassed)
    else -> true
  }

  /** Groepen van een woordniveau: elke lijst is een groep. */
  fun vocabularyGroups(data: AppData): Map<String, List<String>> =
    data.lists.associate { list -> list.id to list.words.map { it.id } }

  fun vocabularyView(data: AppData, level: String) = LevelView(level, vocabularyGroups(data), data)

  /** Verwerkt het eerste antwoord op een item. */
  fun recordAnswer(data: AppData, level: String, group: String, item: String, correct: Boolean): AppData {
    val key = AppData.progressKey(level, group, item)
    val old = data.progress[key] ?: WordProgress()
    val updated = WordProgress(
      score = Scoring.update(old.score, correct),
      correct = old.correct + if (correct) 1 else 0,
      wrong = old.wrong + if (correct) 0 else 1,
      played = true,
      streak = if (correct) old.streak + 1 else 0,
    )
    val next = data.copy(progress = data.progress + (key to updated))
    return markLevelReached(next, level)
  }

  /**
   * Een afgeronde sessie. [perfectByGroup]: per betrokken groep of die zonder fouten gespeeld werd.
   */
  fun finishSession(data: AppData, level: String, perfectByGroup: Map<String, Boolean>): AppData {
    var sessions = data.sessions
    for ((group, perfect) in perfectByGroup) {
      val key = AppData.sessionKey(level, group)
      val old = sessions[key] ?: SessionStat()
      sessions = sessions + (key to SessionStat(
        completed = old.completed + 1,
        lastPerfect = perfect,
        perfectCount = old.perfectCount + if (perfect) 1 else 0,
      ))
    }
    return data.copy(sessions = sessions)
  }

  /** Een geslaagd deel-examen: de volgende medaille (max. goud). Het eerste examen ontgrendelt het volgende niveau. */
  fun awardMedal(data: AppData, level: String, partId: String): AppData {
    val key = AppData.medalKey(level, partId)
    val next = minOf(3, (data.medals[key] ?: 0) + 1)
    return markExamPassed(data.copy(medals = data.medals + (key to next)), level)
  }

  fun markExamPassed(data: AppData, level: String): AppData =
    data.copy(examPassed = data.examPassed + level)

  private fun markLevelReached(data: AppData, level: String): AppData {
    if (level in data.levelReached) return data
    val groups = when {
      Levels.isVocabulary(level) -> vocabularyGroups(data)
      else -> return data // grammar: wordt door de grammatica-laag bijgehouden
    }
    return if (LevelView(level, groups, data).examAvailable()) data.copy(levelReached = data.levelReached + level) else data
  }

  /** Voor niveaus met een eigen groepsindeling (Grammar) na elke sessie aan te roepen. */
  fun markReachedIfAvailable(data: AppData, view: LevelView): AppData =
    if (view.level !in data.levelReached && view.examAvailable()) data.copy(levelReached = data.levelReached + view.level) else data
}
