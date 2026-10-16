package com.robpolus.commskills.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TypingTest {
  private val checker = AnswerChecker()

  private fun vq(en: String, nl: String, dir: Direction = Direction.EN_NL) =
    VocabQuestion(Entry("l", w(en, nl)), dir)

  @Test
  fun practiceRepeatsWrongQuestionUntilCorrect() {
    val s = TypingSession(listOf(vq("one", "een"), vq("two", "twee")), random = Random(1))
    val first = s.current()!!
    val out = s.answer(checker, "fout")!!
    assertFalse(out.result.isCorrect)
    assertTrue(out.firstTry)
    // alles juist beantwoorden tot klaar
    var guard = 0
    while (!s.finished && guard++ < 20) {
      val q = s.current()!!
      s.answer(checker, if (q.id == "one") "een" else "twee")
    }
    assertTrue(s.finished)
    assertEquals(listOf(first.id), s.mistakes.map { it.id })
    assertFalse(s.isPerfect)
    assertEquals(mapOf("l" to false), s.perfectByGroup())
  }

  @Test
  fun onlyFirstAttemptIsFlagged() {
    val s = TypingSession(listOf(vq("one", "een")), random = Random(2))
    assertTrue(s.answer(checker, "x")!!.firstTry)
    assertFalse(s.answer(checker, "een")!!.firstTry)
    assertTrue(s.finished)
  }

  @Test
  fun perfectSession() {
    val s = TypingSession(listOf(vq("one", "een"), vq("two", "twee")), random = Random(3))
    while (!s.finished) {
      val q = s.current()!!
      s.answer(checker, if (q.id == "one") "een" else "twee")
    }
    assertTrue(s.isPerfect)
    assertEquals(2, s.completed)
  }

  @Test
  fun examAsksEachQuestionOnceAndLosesLives() {
    val qs = (1..5).map { vq("w$it", "n$it") }
    val s = TypingSession(qs, ExamEngine(5), Random(4))
    repeat(2) { s.answer(checker, "fout") }
    assertEquals(1, s.exam!!.livesLeft)
    assertFalse(s.finished)
    s.answer(checker, "fout")
    assertEquals(ExamStatus.FAILED, s.exam.status)
    assertTrue(s.finished)
    assertNull(s.current())
    assertNull(s.answer(checker, "x"))
  }

  @Test
  fun examPassesWithOneLifeLeft() {
    val qs = (1..4).map { vq("w$it", "n$it") }
    val s = TypingSession(qs, ExamEngine(4), Random(5))
    s.answer(checker, "fout")
    s.answer(checker, "fout")
    while (!s.finished) {
      val q = s.current()!!
      s.answer(checker, q.id.replace("w", "n"))
    }
    assertEquals(ExamStatus.PASSED, s.exam!!.status)
    assertEquals(4, s.completed)
  }

  @Test
  fun directionsFollowChoice() {
    val entries = (1..10).map { Entry("l", w("w$it", "n$it")) }
    assertTrue(VocabQuestion.from(entries, DirectionChoice.EN_NL).all { it.direction == Direction.EN_NL })
    assertTrue(VocabQuestion.from(entries, DirectionChoice.NL_EN).all { it.direction == Direction.NL_EN })
    val mixed = VocabQuestion.from(entries, DirectionChoice.MIXED, Random(6)).map { it.direction }.toSet()
    assertEquals(2, mixed.size)
  }

  @Test
  fun vocabQuestionTexts() {
    val toDutch = vq("to pursue", "nastreven")
    assertEquals("to pursue", toDutch.prompt)
    assertEquals("nastreven", toDutch.correctText)
    val toEnglish = vq("to pursue", "nastreven", Direction.NL_EN)
    assertEquals("nastreven", toEnglish.prompt)
    assertEquals("to pursue", toEnglish.correctText)
  }

  private val ex = GrammarExercise("c2-01", 2, "If I ___ rich, I would help.", "(to be)", listOf("were"), "Type 2")

  @Test
  fun grammarQuestionChecksAnswers() {
    val q = GrammarQuestion(ex)
    assertEquals("type2", q.group)
    assertEquals("If I were rich, I would help.", ex.filled())
    assertTrue(q.check(checker, "were").isCorrect)
    assertFalse(q.check(checker, "was").isCorrect)
  }

  @Test
  fun grammarGroupsAlwaysHaveAllTypes() {
    val g = Grammar.groups(listOf(ex))
    assertEquals(listOf("type0", "type1", "type2", "type3"), g.keys.toList())
    assertEquals(listOf("c2-01"), g["type2"])
    assertTrue(g.getValue("type0").isEmpty())
  }

  @Test
  fun grammarProgressReachesLevelAndMixedUnlock() {
    val exercises = (0..3).flatMap { t -> (1..2).map { GrammarExercise("c$t-$it", t, "x ___", "(h)", listOf("a"), "") } }
    var d = AppData()
    repeat(5) { exercises.forEach { e -> d = Grammar.recordAnswer(d, exercises, e, true) } }
    assertTrue(Levels.GRAMMAR in d.levelReached)
    assertFalse(Grammar.view(d, exercises).mixedUnlocked())
    for (t in 0..3) d = Learning.finishSession(d, Levels.GRAMMAR, mapOf(Grammar.groupId(t) to true))
    assertTrue(Grammar.view(d, exercises).mixedUnlocked())
  }

  @Test
  fun grammarSessionsUseChosenTypesAndExamHasUniqueQuestions() {
    val exercises = (0..3).flatMap { t -> (1..25).map { GrammarExercise("c$t-$it", t, "x ___", "(h)", listOf("a"), "") } }
    val s = Grammar.sessionExercises(AppData(), exercises, listOf("type1", "type3"), Random(1))
    assertEquals(50, s.size)
    assertTrue(s.all { it.type == 1 || it.type == 3 })
    assertEquals(100, Grammar.sessionExercises(AppData(), exercises, null, Random(1)).size)
    val exam = Grammar.examExercises(AppData(), exercises, random = Random(2))
    assertEquals(21, exam.size)
    assertEquals(21, exam.map { it.id }.toSet().size)
    assertNotNull(exam.firstOrNull())
  }
}
