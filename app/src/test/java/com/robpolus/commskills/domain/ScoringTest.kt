package com.robpolus.commskills.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringTest {

  @Test
  fun correctAnswersRaiseTheScore() {
    var score = 0.0
    repeat(3) { score = Scoring.update(score, true) }
    assertEquals(1 - Math.pow(0.7, 3.0), score, 1e-9)
  }

  @Test
  fun wrongAnswerLowersTheScore() {
    assertEquals(0.7, Scoring.update(1.0, false), 1e-9)
    assertEquals(0.0, Scoring.update(0.0, false), 1e-9)
  }

  @Test
  fun weakWordsHaveHigherWeight() {
    assertTrue(Scoring.weight(0.0) > Scoring.weight(0.5))
    assertTrue(Scoring.weight(0.5) > Scoring.weight(1.0))
    assertEquals(4.0, Scoring.weight(0.0), 1e-9)
    assertEquals(1.0, Scoring.weight(1.0), 1e-9)
  }

  @Test
  fun levelPercentageCountsUnplayedWordsAsZero() {
    assertEquals(0.5, Scoring.levelPercentage(listOf(1.0, 0.0, 1.0, 0.0)), 1e-9)
    assertEquals(0.0, Scoring.levelPercentage(emptyList()), 1e-9)
  }

  @Test
  fun examAvailableFromSeventyPercent() {
    assertFalse(Scoring.examAvailable(0.69))
    assertTrue(Scoring.examAvailable(0.7))
    assertTrue(Scoring.examAvailable(0.95))
  }

  @Test
  fun wordColors() {
    assertEquals(WordColor.GREY, Scoring.wordColor(0.0, played = false))
    assertEquals(WordColor.RED, Scoring.wordColor(0.3, played = true))
    assertEquals(WordColor.ORANGE, Scoring.wordColor(0.6, played = true))
    assertEquals(WordColor.GREEN, Scoring.wordColor(0.8, played = true))
  }

  @Test
  fun listColors() {
    assertEquals(ListColor.GREY, Scoring.listColor(0.0, anyPlayed = false, lastSessionPerfect = false))
    assertEquals(ListColor.RED, Scoring.listColor(0.4, anyPlayed = true, lastSessionPerfect = false))
    assertEquals(ListColor.ORANGE, Scoring.listColor(0.6, anyPlayed = true, lastSessionPerfect = false))
    assertEquals(ListColor.GREEN, Scoring.listColor(0.7, anyPlayed = true, lastSessionPerfect = false))
    assertEquals(ListColor.GOLD, Scoring.listColor(0.9, anyPlayed = true, lastSessionPerfect = true))
  }
}
