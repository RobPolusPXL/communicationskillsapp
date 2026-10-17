package com.robpolus.commskills.domain

import kotlin.random.Random

/** Een invulzin: `text` bevat precies één `___`, `hint` is het werkwoord tussen haakjes. */
data class GrammarExercise(
  val id: String,
  val type: Int,
  val text: String,
  val hint: String,
  val answers: List<String>,
  val explain: String,
) {
  /** De zin met het eerste juiste antwoord ingevuld. */
  fun filled(): String = text.replace(BLANK, answers.first())

  companion object {
    const val BLANK = "___"
  }
}

object Grammar {
  val types = listOf(0, 1, 2, 3)

  fun groupId(type: Int) = "type$type"

  fun title(type: Int) = "Type $type"

  fun subtitle(type: Int) = when (type) {
    0 -> "altijd waar (if + present, present)"
    1 -> "waarschijnlijk (if + present, will)"
    2 -> "onwaarschijnlijk (if + past, would)"
    else -> "onmogelijk, verleden (if + past perfect, would have)"
  }

  /** Per if-type de ids van de oefeningen. Alle vier de types staan er altijd in. */
  fun groups(exercises: List<GrammarExercise>): Map<String, List<String>> =
    types.associate { t -> groupId(t) to exercises.filter { it.type == t }.map { it.id } }

  fun view(data: AppData, exercises: List<GrammarExercise>) = LevelView(Levels.GRAMMAR, groups(exercises), data)

  /** Gewogen volgorde (zwakke zinnen eerst) over de gekozen types; `null` = alle types (gemixt). */
  fun sessionExercises(
    data: AppData,
    exercises: List<GrammarExercise>,
    groupIds: List<String>?,
    random: Random = Random.Default,
  ): List<GrammarExercise> {
    val view = view(data, exercises)
    val chosen = exercises.filter { groupIds == null || groupId(it.type) in groupIds }
    return Sessions.weightedOrder(chosen, { view.weight(groupId(it.type), it.id) }, random)
  }

  /** Examen per if-type: een willekeurige selectie van hoogstens [count] zinnen van dat type. */
  fun examExercises(
    exercises: List<GrammarExercise>,
    type: Int,
    count: Int = SessionBuilder.TYPE_SESSION_SIZE,
    random: Random = Random.Default,
  ): List<GrammarExercise> = exercises.filter { it.type == type }.shuffled(random).take(count)

  /** Zinnen van dit type waarvan het laatste antwoord juist was / alle zinnen van het type (één keer juist volstaat). */
  fun knownPercent(data: AppData, exercises: List<GrammarExercise>, type: Int): Double {
    val view = view(data, exercises)
    val ofType = exercises.filter { it.type == type }
    return if (ofType.isEmpty()) 0.0 else ofType.count { view.correctNow(groupId(type), it.id) }.toDouble() / ofType.size
  }

  fun ready(data: AppData, exercises: List<GrammarExercise>, type: Int): Boolean =
    knownPercent(data, exercises, type) >= Scoring.READY - 1e-9

  fun medal(data: AppData, type: Int): Int = data.medals[AppData.medalKey(Levels.GRAMMAR, groupId(type))] ?: 0

  /** Mag het examen van dit type: 70% gekend, of al een medaille. */
  fun examOpen(data: AppData, exercises: List<GrammarExercise>, type: Int): Boolean =
    medal(data, type) > 0 || ready(data, exercises, type)

  /** Voor niveaus met een eigen groepsindeling: verwerkt een antwoord en onthoudt of de 70% bereikt is. */
  fun recordAnswer(data: AppData, exercises: List<GrammarExercise>, exercise: GrammarExercise, correct: Boolean): AppData {
    val next = Learning.recordAnswer(data, Levels.GRAMMAR, groupId(exercise.type), exercise.id, correct)
    return Learning.markReachedIfAvailable(next, view(next, exercises))
  }
}
