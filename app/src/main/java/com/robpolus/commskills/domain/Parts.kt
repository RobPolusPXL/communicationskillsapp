package com.robpolus.commskills.domain

/** Een deel van een woordenlijst: hoogstens [Parts.MAX] woorden, in de volgorde van de lijst. */
data class Part(
  val id: String,
  val listId: String,
  val title: String,
  val number: Int,
  val count: Int,
  val words: List<Word>,
)

enum class PartColor { GREY, PROGRESS, RED, GREEN, BRONZE, SILVER, GOLD }

object Parts {
  const val MAX = 40

  /** Lijsten tot 40 woorden blijven één deel; langere worden gelijkmatig verdeeld (111 → 37, 37, 37). */
  fun of(list: WordList): List<Part> {
    val chunks = SessionBuilder.rounds(list.words, MAX)
    return chunks.mapIndexed { i, words ->
      val single = chunks.size == 1
      Part(
        id = if (single) list.id else "${list.id}#${i + 1}",
        listId = list.id,
        title = if (single) list.title else "${list.title} · deel ${i + 1}",
        number = i + 1,
        count = chunks.size,
        words = words,
      )
    }
  }

  fun all(data: AppData): List<Part> = data.lists.flatMap { of(it) }

  fun known(view: LevelView, part: Part): Int = part.words.count { view.known(part.listId, it.id) }

  /** Woorden waarvan het laatste antwoord juist was / alle woorden: juist is juist, fout is fout. */
  fun percent(view: LevelView, part: Part): Double =
    if (part.words.isEmpty()) 0.0 else part.words.count { view.correctNow(part.listId, it.id) }.toDouble() / part.words.size

  /** Gekende woorden (twee keer na elkaar juist) / alle woorden: bepaalt wanneer het deel groen wordt. */
  fun knownPercent(view: LevelView, part: Part): Double =
    if (part.words.isEmpty()) 0.0 else known(view, part).toDouble() / part.words.size

  fun ready(view: LevelView, part: Part): Boolean = knownPercent(view, part) >= Scoring.READY - 1e-9

  fun medal(data: AppData, level: String, part: Part): Int = data.medals[AppData.medalKey(level, part.id)] ?: 0

  /** Mag het examen van dit deel: groen, of al een medaille. */
  fun examOpen(data: AppData, level: String, part: Part): Boolean =
    medal(data, level, part) > 0 || ready(Learning.vocabularyView(data, level), part)

  fun color(data: AppData, level: String, part: Part): PartColor {
    when (medal(data, level, part)) {
      1 -> return PartColor.BRONZE
      2 -> return PartColor.SILVER
      3 -> return PartColor.GOLD
    }
    val view = Learning.vocabularyView(data, level)
    val anyPlayed = part.words.any { view.played(part.listId, it.id) }
    val allPlayed = part.words.all { view.played(part.listId, it.id) }
    return when {
      ready(view, part) -> PartColor.GREEN
      !anyPlayed -> PartColor.GREY
      allPlayed -> PartColor.RED
      else -> PartColor.PROGRESS
    }
  }
}
