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

  fun examExercises(
    data: AppData,
    exercises: List<GrammarExercise>,
    count: Int = ExamEngine.GRAMMAR_QUESTIONS,
    random: Random = Random.Default,
  ): List<GrammarExercise> {
    val view = view(data, exercises)
    return Sessions.examQuestions(exercises, count, { view.weight(groupId(it.type), it.id) }, random)
  }

  /** Voor niveaus met een eigen groepsindeling: verwerkt een antwoord en onthoudt of de 70% bereikt is. */
  fun recordAnswer(data: AppData, exercises: List<GrammarExercise>, exercise: GrammarExercise, correct: Boolean): AppData {
    val next = Learning.recordAnswer(data, Levels.GRAMMAR, groupId(exercise.type), exercise.id, correct)
    return Learning.markReachedIfAvailable(next, view(next, exercises))
  }
}
