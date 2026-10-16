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

data class VocabQuestion(val entry: Entry, val direction: Direction) : Question {
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

  override fun check(checker: AnswerChecker, given: String) = checker.checkWord(given, word, direction)

  companion object {
    fun from(entries: List<Entry>, choice: DirectionChoice, random: Random = Random.Default) =
      entries.map { e ->
        val dir = when (choice) {
          DirectionChoice.EN_NL -> Direction.EN_NL
          DirectionChoice.NL_EN -> Direction.NL_EN
          DirectionChoice.MIXED -> if (random.nextBoolean()) Direction.EN_NL else Direction.NL_EN
        }
        VocabQuestion(e, dir)
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

  fun answer(checker: AnswerChecker, given: String): TypedOutcome<Q>? {
    val q = current() ?: return null
    val result = q.check(checker, given)
    val correct = result.isCorrect
    val firstTry = attempted.add(q)
    if (firstTry && !correct) wrongFirstTry += q
    if (exam != null) {
      exam.record(correct)
      examIndex++
    } else {
      queue.answer(correct)
    }
    return TypedOutcome(q, result, firstTry)
  }

  /** Vragen die bij de eerste poging fout waren. */
  val mistakes: List<Q> get() = wrongFirstTry.toList()

  val isPerfect: Boolean get() = finished && wrongFirstTry.isEmpty()

  fun perfectByGroup(): Map<String, Boolean> =
    groups.associateWith { g -> wrongFirstTry.none { it.group == g } }
}
