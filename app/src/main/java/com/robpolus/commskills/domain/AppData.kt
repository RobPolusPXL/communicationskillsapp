package com.robpolus.commskills.domain

/** Niveau-sleutels. Voortgang hoort bij een niveau: matchen en typen tellen apart. */
object Levels {
  const val VOCAB_MATCH = "vocab.match"
  const val VOCAB_TYPE = "vocab.type"
  const val GRAMMAR = "grammar.conditionals"

  val vocabulary = listOf(VOCAB_MATCH, VOCAB_TYPE)

  fun isVocabulary(level: String) = level in vocabulary
}

data class WordList(val id: String, val title: String, val words: List<Word>)

data class WordProgress(
  val score: Double = 0.0,
  val correct: Int = 0,
  val wrong: Int = 0,
  val played: Boolean = false,
  /** Aantal juiste antwoorden na elkaar; een fout zet dit terug op 0. */
  val streak: Int = 0,
)

/** Wat er met een groep (lijst of if-type) in een niveau gespeeld is. */
data class SessionStat(
  val completed: Int = 0,
  val lastPerfect: Boolean = false,
  val perfectCount: Int = 0,
)

data class StreakState(val current: Int = 0, val longest: Int = 0, val lastDate: String? = null)

data class DailyStat(val activeSeconds: Int = 0, val exercises: Int = 0)

data class Settings(
  val reminderEnabled: Boolean = false,
  val reminderHour: Int = 19,
  val reminderMinute: Int = 0,
)

/** Alles wat de app bewaart. Onveranderlijk: elke wijziging geeft een nieuwe [AppData]. */
data class AppData(
  val lists: List<WordList> = emptyList(),
  /** Sleutel: "niveau|groep|item". */
  val progress: Map<String, WordProgress> = emptyMap(),
  /** Sleutel: "niveau|groep". */
  val sessions: Map<String, SessionStat> = emptyMap(),
  val examPassed: Set<String> = emptySet(),
  /** Door de gebruiker goedgekeurde extra antwoorden. Sleutel: "nl|lijst|woord" (Nederlands) of "en|lijst|woord". */
  val extraAnswers: Map<String, List<String>> = emptyMap(),
  /** Sleutel: "niveau|deel". Waarde: 1 = brons, 2 = zilver, 3 = goud. */
  val medals: Map<String, Int> = emptyMap(),
  /** Niveaus waar de 70% ooit gehaald is. */
  val levelReached: Set<String> = emptySet(),
  val streak: StreakState = StreakState(),
  /** Sleutel: yyyy-MM-dd. */
  val daily: Map<String, DailyStat> = emptyMap(),
  val settings: Settings = Settings(),
  /** Bundel-lijsten die al ingelezen zijn (bestandsnamen). */
  val seededAssets: Set<String> = emptySet(),
  val updatedAt: Long = 0L,
) {
  companion object {
    fun progressKey(level: String, group: String, item: String) = "$level|$group|$item"
    fun sessionKey(level: String, group: String) = "$level|$group"
    fun extraKey(dir: String, group: String, id: String) = "$dir|$group|$id"
    fun medalKey(level: String, partId: String) = "$level|$partId"
  }
}
