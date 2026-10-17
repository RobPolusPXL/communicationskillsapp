package com.robpolus.commskills.domain

import kotlin.random.Random
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class GrammarExamTest {
  private val exercises = (0..3).flatMap { t -> (1..25).map { GrammarExercise("c$t-$it", t, "x ___", "(h)", listOf("a"), "") } }

  @Test
  fun examHasAtMost15QuestionsOfOneType() {
    val q = Grammar.examExercises(exercises, 2, random = Random(1))
    assertEquals(SessionBuilder.TYPE_SESSION_SIZE, q.size)
    assertTrue(q.all { it.type == 2 })
    assertEquals(q.size, q.map { it.id }.toSet().size)
  }

  @Test
  fun examOpensAt70PercentKnownOfThatType() {
    var d = AppData()
    assertFalse(Grammar.examOpen(d, exercises, 1))
    // 17 van 25 zinnen twee keer juist = 68%: nog dicht.
    exercises.filter { it.type == 1 }.take(17).forEach { e -> repeat(2) { d = Grammar.recordAnswer(d, exercises, e, true) } }
    assertFalse(Grammar.examOpen(d, exercises, 1))
    // 18 van 25 = 72%: open, maar enkel voor dit type.
    exercises.filter { it.type == 1 }[17].let { e -> repeat(2) { d = Grammar.recordAnswer(d, exercises, e, true) } }
    assertTrue(Grammar.examOpen(d, exercises, 1))
    assertFalse(Grammar.examOpen(d, exercises, 0))
  }

  @Test
  fun medalKeepsExamOpenAndIsPerType() {
    var d = AppData()
    d = Learning.awardMedal(d, Levels.GRAMMAR, Grammar.groupId(3))
    assertEquals(1, Grammar.medal(d, 3))
    assertEquals(0, Grammar.medal(d, 0))
    assertTrue(Grammar.examOpen(d, exercises, 3))
    assertTrue(Levels.GRAMMAR in d.examPassed)
  }

  @Test
  fun mistakesAllowedByMedal() {
    assertEquals(3, ChoiceBuilder.lives(0))
    assertEquals(2, ChoiceBuilder.lives(1))
    assertEquals(1, ChoiceBuilder.lives(2))
  }
}
