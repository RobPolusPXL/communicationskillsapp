package com.robpolus.commskills.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ExamEngineTest {

  @Test
  fun examSizesAndLives() {
    assertEquals(12, ExamEngine.vocabulary().totalQuestions)
    assertEquals(21, ExamEngine.grammar().totalQuestions)
    assertEquals(3, ExamEngine.vocabulary().livesLeft)
    assertEquals(3, ExamEngine.grammar().livesLeft)
  }

  @Test
  fun passesWhenAllQuestionsAnsweredWithLivesLeft() {
    val exam = ExamEngine.vocabulary()
    repeat(11) { assertEquals(ExamStatus.IN_PROGRESS, exam.record(true)) }
    assertEquals(ExamStatus.PASSED, exam.record(true))
  }

  @Test
  fun twoMistakesStillPass() {
    val exam = ExamEngine.vocabulary()
    exam.record(false)
    exam.record(false)
    repeat(9) { exam.record(true) }
    assertEquals(ExamStatus.PASSED, exam.record(true))
    assertEquals(1, exam.livesLeft)
  }

  @Test
  fun failsWhenAllLivesAreLost() {
    val exam = ExamEngine.grammar()
    exam.record(false)
    exam.record(true)
    exam.record(false)
    assertEquals(ExamStatus.FAILED, exam.record(false))
    assertEquals(0, exam.livesLeft)
  }

  @Test
  fun answersAfterTheEndAreIgnored() {
    val exam = ExamEngine(totalQuestions = 2)
    exam.record(true)
    exam.record(true)
    assertEquals(ExamStatus.PASSED, exam.record(false))
    assertEquals(2, exam.answered)
    assertEquals(3, exam.livesLeft)
  }
}
