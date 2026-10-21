package com.robpolus.commskills.domain

import kotlin.random.Random

/** Eén te typen vraag (woord of zin). */
sealed interface Question {
  val group: String
  val id: String

  /** Wat getoond wordt boven het invoerveld. */
  val prompt: String
  val label: String
  val hint: String?
  val answerLabel: String

  /** De oplossing die bij een fout (en ter info bij juist) getoond wordt. */
  val correctText: String
  val explanation: String

  fun check(checker: AnswerChecker, given: String): AnswerResult
}

data class VocabQuestion(
  val entry: Entry,
  val direction: Direction,
  /** Andere woorden met een gelijke Nederlandse vertaling; bij NL→EN zijn hun Engelse woorden ook goed. */
  val synonyms: List<Word> = emptyList(),
  /** Andere woorden met dezelfde Engelse tekst (decrease n/v): hun Nederlandse vertalingen zijn ook goed. */
  val sameEnglish: List<Word> = emptyList(),
  /** Antwoorden die de gebruiker eerder toch juist vond, in de richting van deze vraag. */
  val extra: List<String> = emptyList(),
) : Question {
  val extraKey get() = AppData.extraKey(if (toDutch) "nl" else "en", entry.group, word.id)

  private val word get() = entry.word
  private val toDutch get() = direction == Direction.EN_NL

  override val group get() = entry.group
  override val id get() = word.id
  override val prompt get() = if (toDutch) word.en else word.nl.joinToString(", ")
  override val label get() = if (toDutch) "Engels → Nederlands" else "Nederlands → Engels"
  override val hint get() = if (toDutch) null else word.type
  override val answerLabel get() = if (toDutch) "Nederlandse vertaling" else "Engelse vertaling"
  override val correctText get() = if (toDutch) word.nl.joinToString(", ") else word.en
  override val explanation get() = word.definition

  override fun check(checker: AnswerChecker, given: String): AnswerResult {
    val own = checker.checkWord(given, word, direction)
    if (own.isCorrect) return own
    if (extra.isNotEmpty()) {
      val r = if (toDutch) checker.check(given, extra.flatMap { Variants.expandOptional(it) }, dutch = true)
      else checker.check(given, extra.flatMap { Variants.expandOptional(it) }, lenientPrefixes = true)
      if (r is AnswerResult.Correct) return r
    }
    if (direction == Direction.EN_NL) {
      for (other in sameEnglish) {
        val r = checker.checkWord(given, other, Direction.EN_NL)
        if (r is AnswerResult.Correct) return r
      }
      return own
    }
    for (other in synonyms) {
      val r = checker.checkWord(given, other, Direction.NL_EN)
      if (r is AnswerResult.Correct) return AnswerResult.Correct(r.notes + Note.SYNONYM)
    }
    return own
  }

  companion object {
    /** [pool]: alle bekende woorden, om synoniemen (zelfde Nederlandse vertaling) te vinden. */
    fun from(
      entries: List<Entry>,
      choice: DirectionChoice,
      random: Random = Random.Default,
      pool: List<Word> = entries.map { it.word },
      extras: Map<String, List<String>> = emptyMap(),
    ): List<VocabQuestion> {
      val byDutch = HashMap<String, MutableList<Word>>()
      for (w in pool) for (nl in w.nl) byDutch.getOrPut(nl.lowercase()) { mutableListOf() }.add(w)
      return entries.map { e ->
        val dir = when (choice) {
          DirectionChoice.EN_NL -> Direction.EN_NL
          DirectionChoice.NL_EN -> Direction.NL_EN
          DirectionChoice.MIXED -> if (random.nextBoolean()) Direction.EN_NL else Direction.NL_EN
        }
        val synonyms = e.word.nl.flatMap { byDutch[it.lowercase()].orEmpty() }
          .filter { it.id != e.word.id || it.en != e.word.en }
          .distinctBy { it.en }
        val same = pool.filter { it.en.equals(e.word.en, ignoreCase = true) && it.id != e.word.id }.distinctBy { it.id }
        val extraKey = AppData.extraKey(if (dir == Direction.EN_NL) "nl" else "en", e.group, e.word.id)
        VocabQuestion(e, dir, synonyms, same, extras[extraKey].orEmpty())
      }
    }
  }
}

data class GrammarQuestion(val exercise: GrammarExercise) : Question {
  override val group get() = Grammar.groupId(exercise.type)
  override val id get() = exercise.id
  override val prompt get() = exercise.text
  override val label get() = "Vul de zin aan"
  override val hint get() = exercise.hint
  override val answerLabel get() = "Ontbrekend deel"
  override val correctText get() = exercise.answers.first()
  override val explanation get() = exercise.explain

  override fun check(checker: AnswerChecker, given: String) = checker.check(given, exercise.answers)
}

enum class DirectionChoice { EN_NL, NL_EN, MIXED }

/** Uitkomst van één antwoord. */
data class TypedOutcome<Q : Question>(
  val question: Q,
  val result: AnswerResult,
  /** Alleen het eerste antwoord op een vraag telt voor de score. */
  val firstTry: Boolean,
  /** Telt dit antwoord als juist voor score en wachtrij? Niet bij een antwoord na een tip. */
  val counts: Boolean = result.isCorrect,
)

/**
 * Een typsessie. Oefenen: een fout beantwoorde vraag komt later terug tot ze juist is.
 * Examen ([exam] gegeven): elke vraag komt één keer; fout = een leven kwijt.
 */
class TypingSession<Q : Question>(
  questions: List<Q>,
  val exam: ExamEngine? = null,
  random: Random = Random.Default,
) {
  private val queue = SessionQueue(questions, random)
  private val examQuestions = questions
  private var examIndex = 0
  private val attempted = mutableSetOf<Q>()
  private val wrongFirstTry = LinkedHashSet<Q>()
  private val groups = questions.map { it.group }.distinct()
  private var lastWrongQuestion: Q? = null
  var lastQuestionAnswered: Q? = null
    private set
  private var lastWrongFirst = false

  /** Het laatste foute antwoord toch juist rekenen; in een examen komt het leven terug. */
  fun forgiveLast(): Boolean {
    if (if (exam != null) !exam.forgiveLast() else !queue.forgiveLast()) return false
    val q = lastWrongQuestion
    if (q != null && lastWrongFirst) wrongFirstTry -= q
    lastWrongQuestion = null
    return true
  }

  val total: Int = questions.size

  val completed: Int get() = if (exam != null) examIndex else queue.completed

  val finished: Boolean
    get() = if (exam != null) {
      exam.status != ExamStatus.IN_PROGRESS || examIndex >= examQuestions.size
    } else {
      queue.isFinished
    }

  fun current(): Q? =
    if (exam != null) examQuestions.getOrNull(examIndex).takeIf { exam.status == ExamStatus.IN_PROGRESS }
    else queue.current()

  fun answer(checker: AnswerChecker, given: String, hinted: Boolean = false): TypedOutcome<Q>? {
    val q = current() ?: return null
    lastQuestionAnswered = q
    val result = q.check(checker, given).let { r ->
      if (hinted && r is AnswerResult.Correct) AnswerResult.Correct(r.notes + Note.HINT) else r
    }
    val correct = result.isCorrect && !hinted
    val firstTry = attempted.add(q)
    if (firstTry && !correct) wrongFirstTry += q
    if (!correct) { lastWrongQuestion = q; lastWrongFirst = firstTry }
    if (exam != null) {
      exam.record(correct)
      examIndex++
    } else {
      queue.answer(correct)
    }
    return TypedOutcome(q, result, firstTry, correct)
  }

  /** Vragen die bij de eerste poging fout waren. */
  val mistakes: List<Q> get() = wrongFirstTry.toList()

  val isPerfect: Boolean get() = finished && wrongFirstTry.isEmpty()

  fun perfectByGroup(): Map<String, Boolean> =
    groups.associateWith { g -> wrongFirstTry.none { it.group == g } }
}
