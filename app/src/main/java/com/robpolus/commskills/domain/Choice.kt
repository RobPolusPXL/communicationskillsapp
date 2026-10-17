package com.robpolus.commskills.domain

import kotlin.random.Random

/** Eén meerkeuzevraag: één woord, vijf opties. */
data class ChoiceQuestion(
  val entry: Entry,
  val direction: Direction,
  val options: List<String>,
  val correctIndex: Int,
) {
  val group get() = entry.group
  val id get() = entry.word.id
  val prompt get() = if (direction == Direction.EN_NL) entry.word.en else entry.word.nl.joinToString(", ")
  val label get() = if (direction == Direction.EN_NL) "Engels → Nederlands" else "Nederlands → Engels"
  val correctText get() = options[correctIndex]
}

object ChoiceBuilder {
  const val OPTIONS = 5

  /** Levens bij een examen: brons en zilver mogen één fout hebben, goud geen. */
  fun lives(currentMedal: Int) = if (currentMedal >= 2) 1 else 2

  /**
   * Alle woorden van [part] als vraag, door elkaar. De helft EN→NL, de helft NL→EN.
   * Foute opties komen eerst uit dezelfde lijst, daarna uit de rest; synoniemen worden nooit als fout getoond.
   */
  fun build(part: Part, pool: List<Entry>, random: Random = Random.Default): List<ChoiceQuestion> {
    val entries = part.words.map { Entry(part.listId, it) }.shuffled(random)
    val dirs = entries.indices.map { if (it % 2 == 0) Direction.EN_NL else Direction.NL_EN }.shuffled(random)
    val shuffledPool = pool.shuffled(random)
    return entries.mapIndexed { i, e ->
      val dir = dirs[i]
      val w = e.word
      val forbiddenNl = w.nl.map { it.lowercase() }.toSet()
      val candidates = (shuffledPool.filter { it.group == e.group } + shuffledPool.filter { it.group != e.group })
        .filter { it.word.id != w.id || it.group != e.group }
        .filter { c -> c.word.nl.none { it.lowercase() in forbiddenNl } && !c.word.en.equals(w.en, true) }
      val correct = if (dir == Direction.EN_NL) w.nl.first() else w.en
      val seen = mutableSetOf(correct.lowercase())
      val wrong = mutableListOf<String>()
      for (c in candidates) {
        val text = if (dir == Direction.EN_NL) c.word.nl.first() else c.word.en
        if (seen.add(text.lowercase())) wrong += text
        if (wrong.size == OPTIONS - 1) break
      }
      val options = (wrong + correct).shuffled(random)
      ChoiceQuestion(e, dir, options, options.indexOf(correct))
    }
  }
}

/** Een meerkeuze-examen over één deel. Eén woord tegelijk; elke vraag één keer. */
class ChoiceExamSession(val questions: List<ChoiceQuestion>, val engine: ExamEngine) {
  var index = 0
    private set
  var picked: Int? = null
    private set

  private val wrongOnes = LinkedHashSet<ChoiceQuestion>()
  val mistakes: List<ChoiceQuestion> get() = wrongOnes.toList()

  val total get() = questions.size
  val question: ChoiceQuestion? get() = questions.getOrNull(index)
  val finished: Boolean get() = engine.status != ExamStatus.IN_PROGRESS

  /** @return of het antwoord juist was; `null` als er al gekozen was. */
  fun pick(option: Int): Boolean? {
    val q = question ?: return null
    if (picked != null) return null
    picked = option
    val ok = option == q.correctIndex
    if (!ok) wrongOnes += q
    engine.record(ok)
    return ok
  }

  fun next() {
    picked = null
    index++
  }
}
