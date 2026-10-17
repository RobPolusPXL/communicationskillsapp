package com.robpolus.commskills.domain

import kotlin.random.Random
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class PartsTest {
  private fun list(id: String, n: Int) = WordList(id, id.uppercase(), (1..n).map { w("$id$it", "nl$id$it") })

  @Test
  fun splitsEvenlyInPartsOfAtMost40() {
    val sizes = listOf(111 to 3, 169 to 5, 168 to 5, 55 to 2, 40 to 1, 12 to 1, 41 to 2)
    for ((n, parts) in sizes) {
      val p = Parts.of(list("x", n))
      assertEquals(parts, p.size)
      assertEquals(n, p.sumOf { it.words.size })
      assertTrue(p.all { it.words.size <= 40 })
      assertTrue(p.maxOf { it.words.size } - p.minOf { it.words.size } <= 1)
    }
    assertEquals(listOf(37, 37, 37), Parts.of(list("x", 111)).map { it.words.size })
  }

  @Test
  fun singlePartKeepsListIdAndOrderIsKept() {
    val single = Parts.of(list("a", 5)).single()
    assertEquals("a", single.id)
    val multi = Parts.of(list("b", 60))
    assertEquals(listOf("b#1", "b#2"), multi.map { it.id })
    assertEquals("b1", multi[0].words.first().id)
    assertEquals("b31", multi[1].words.first().id)
  }

  @Test
  fun wordIsKnownAfterTwoCorrectInARowAndMistakeResets() {
    var d = sampleData()
    d = Learning.recordAnswer(d, Levels.VOCAB_MATCH, "a", "one", true)
    assertFalse(Learning.vocabularyView(d, Levels.VOCAB_MATCH).known("a", "one"))
    d = Learning.recordAnswer(d, Levels.VOCAB_MATCH, "a", "one", true)
    assertTrue(Learning.vocabularyView(d, Levels.VOCAB_MATCH).known("a", "one"))
    d = Learning.recordAnswer(d, Levels.VOCAB_MATCH, "a", "one", false)
    assertFalse(Learning.vocabularyView(d, Levels.VOCAB_MATCH).known("a", "one"))
  }

  @Test
  fun partTurnsGreenAt70PercentKnown() {
    var d = AppData(lists = listOf(list("a", 10)))
    val part = Parts.all(d).single()
    fun know(i: Int) { repeat(2) { d = Learning.recordAnswer(d, Levels.VOCAB_MATCH, "a", "a$i", true) } }
    for (i in 1..6) know(i)
    assertEquals(PartColor.ORANGE, Parts.color(d, Levels.VOCAB_MATCH, part))
    assertFalse(Parts.examOpen(d, Levels.VOCAB_MATCH, part))
    know(7)
    assertEquals(PartColor.GREEN, Parts.color(d, Levels.VOCAB_MATCH, part))
    assertTrue(Parts.examOpen(d, Levels.VOCAB_MATCH, part))
    assertTrue(Learning.vocabularyView(d, Levels.VOCAB_MATCH).examAvailable())
  }

  @Test
  fun unplayedIsGreyAndFewKnownIsRed() {
    var d = AppData(lists = listOf(list("a", 10)))
    val part = Parts.all(d).single()
    assertEquals(PartColor.GREY, Parts.color(d, Levels.VOCAB_MATCH, part))
    d = Learning.recordAnswer(d, Levels.VOCAB_MATCH, "a", "a1", true)
    assertEquals(PartColor.RED, Parts.color(d, Levels.VOCAB_MATCH, part))
  }

  @Test
  fun medalsClimbToGoldAndUnlockNextLevel() {
    var d = AppData(lists = listOf(list("a", 10)))
    val part = Parts.all(d).single()
    assertFalse(Learning.levelUnlocked(d, Levels.VOCAB_TYPE))
    d = Learning.awardMedal(d, Levels.VOCAB_MATCH, part.id)
    assertEquals(PartColor.BRONZE, Parts.color(d, Levels.VOCAB_MATCH, part))
    assertTrue(Learning.levelUnlocked(d, Levels.VOCAB_TYPE))
    d = Learning.awardMedal(d, Levels.VOCAB_MATCH, part.id)
    assertEquals(PartColor.SILVER, Parts.color(d, Levels.VOCAB_MATCH, part))
    d = Learning.awardMedal(d, Levels.VOCAB_MATCH, part.id)
    d = Learning.awardMedal(d, Levels.VOCAB_MATCH, part.id)
    assertEquals(PartColor.GOLD, Parts.color(d, Levels.VOCAB_MATCH, part))
    assertEquals(PartColor.GREY, Parts.color(d, Levels.VOCAB_TYPE, part))
  }

  @Test
  fun levelPercentageIsShareOfKnownWords() {
    var d = AppData(lists = listOf(list("a", 10)))
    repeat(2) { d = Learning.recordAnswer(d, Levels.VOCAB_MATCH, "a", "a1", true) }
    assertEquals(0.1, Learning.vocabularyView(d, Levels.VOCAB_MATCH).percentage(), 1e-9)
  }

  @Test
  fun choiceQuestionsCoverPartWithFiveDistinctOptions() {
    val d = AppData(lists = listOf(list("a", 12), list("b", 30)))
    val part = Parts.all(d).first { it.listId == "a" }
    val pool = d.lists.flatMap { l -> l.words.map { Entry(l.id, it) } }
    val qs = ChoiceBuilder.build(part, pool, Random(1))
    assertEquals(12, qs.size)
    assertEquals(12, qs.map { it.id }.toSet().size)
    for (q in qs) {
      assertEquals(5, q.options.size)
      assertEquals(5, q.options.map { it.lowercase() }.toSet().size)
      val correct = if (q.direction == Direction.EN_NL) q.entry.word.nl.first() else q.entry.word.en
      assertEquals(correct, q.correctText)
    }
    assertTrue(qs.any { it.direction == Direction.EN_NL } && qs.any { it.direction == Direction.NL_EN })
  }

  @Test
  fun synonymsNeverAppearAsWrongOption() {
    val words = listOf(w("big", "groot"), w("large", "groot"), w("small", "klein"), w("tiny", "piepklein"),
      w("fast", "snel"), w("slow", "traag"), w("red", "rood"))
    val d = AppData(lists = listOf(WordList("a", "A", words)))
    val part = Parts.all(d).single()
    val pool = words.map { Entry("a", it) }
    repeat(30) { seed ->
      for (q in ChoiceBuilder.build(part, pool, Random(seed)).filter { it.entry.word.en == "big" && it.direction == Direction.NL_EN }) {
        assertFalse("large" in q.options)
      }
    }
  }

  @Test
  fun goldGivesOneLifeOthersTwo() {
    assertEquals(2, ChoiceBuilder.lives(0))
    assertEquals(2, ChoiceBuilder.lives(1))
    assertEquals(1, ChoiceBuilder.lives(2))
    assertEquals(1, ChoiceBuilder.lives(3))
  }

  private fun session(lives: Int, n: Int = 6): ChoiceExamSession {
    val d = AppData(lists = listOf(list("a", n)))
    val part = Parts.all(d).single()
    val qs = ChoiceBuilder.build(part, d.lists.flatMap { l -> l.words.map { Entry(l.id, it) } }, Random(3))
    return ChoiceExamSession(qs, ExamEngine(qs.size, lives))
  }

  @Test
  fun bronzeExamAllowsOneMistake() {
    val s = session(2)
    var first = true
    while (!s.finished) {
      val q = s.question!!
      val pick = if (first) (q.correctIndex + 1) % q.options.size else q.correctIndex
      first = false
      s.pick(pick)
      if (!s.finished) s.next()
    }
    assertEquals(ExamStatus.PASSED, s.engine.status)
    assertEquals(1, s.mistakes.size)
  }

  @Test
  fun bronzeExamFailsOnSecondMistake() {
    val s = session(2)
    repeat(2) {
      val q = s.question!!
      s.pick((q.correctIndex + 1) % q.options.size)
      if (!s.finished) s.next()
    }
    assertEquals(ExamStatus.FAILED, s.engine.status)
  }

  @Test
  fun goldExamIsGameOverOnFirstMistake() {
    val s = session(1)
    val q = s.question!!
    s.pick((q.correctIndex + 1) % q.options.size)
    assertTrue(s.finished)
    assertEquals(ExamStatus.FAILED, s.engine.status)
  }

  @Test
  fun perfectExamPassesAndAnswerCanOnlyBePickedOnce() {
    val s = session(1)
    while (!s.finished) {
      val q = s.question!!
      assertEquals(true, s.pick(q.correctIndex))
      assertEquals(null as Boolean?, s.pick(q.correctIndex))
      if (!s.finished) s.next()
    }
    assertEquals(ExamStatus.PASSED, s.engine.status)
  }
}
