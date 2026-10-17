package com.robpolus.commskills.domain

enum class ExamStatus { IN_PROGRESS, PASSED, FAILED }

/**
 * Examen met een vast aantal vragen en levens. Een fout antwoord kost een leven;
 * geslaagd zodra alle vragen beantwoord zijn zonder alle levens te verliezen.
 */
class ExamEngine(val totalQuestions: Int, val lives: Int = LIVES) {

  var livesLeft: Int = lives
    private set
  var answered: Int = 0
    private set
  var correctCount: Int = 0
    private set

  val status: ExamStatus
    get() = when {
      livesLeft <= 0 -> ExamStatus.FAILED
      answered >= totalQuestions -> ExamStatus.PASSED
      else -> ExamStatus.IN_PROGRESS
    }

  private var lastWasMistake = false

  fun record(correct: Boolean): ExamStatus {
    if (status != ExamStatus.IN_PROGRESS) return status
    answered++
    if (correct) correctCount++ else livesLeft--
    lastWasMistake = !correct
    return status
  }

  /** "Toch juist": het laatste foute antwoord telt alsnog als juist; het leven komt terug. */
  fun forgiveLast(): Boolean {
    if (!lastWasMistake) return false
    lastWasMistake = false
    livesLeft++
    correctCount++
    return true
  }

  /** Een leven kwijt zonder dat een vraag afgewerkt is (fout koppelen bij matchen). */
  fun loseLife(): ExamStatus {
    if (status != ExamStatus.IN_PROGRESS) return status
    livesLeft--
    lastWasMistake = false
    return status
  }

  companion object {
    const val LIVES = 3
    const val VOCABULARY_QUESTIONS = 12
    const val GRAMMAR_QUESTIONS = 21

    fun vocabulary() = ExamEngine(VOCABULARY_QUESTIONS)
    fun grammar() = ExamEngine(GRAMMAR_QUESTIONS)
  }
}
