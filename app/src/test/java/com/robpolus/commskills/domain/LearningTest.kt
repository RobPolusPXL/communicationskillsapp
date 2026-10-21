package com.robpolus.commskills.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningTest {

  private val level = Levels.VOCAB_MATCH

  @Test
  fun recordAnswerUpdatesScoreAndCounters() {
    var d = sampleData()
    d = Learning.recordAnswer(d, level, "a", "one", true)
    d = Learning.recordAnswer(d, level, "a", "one", false)
    val p = d.progress[AppData.progressKey(level, "a", "one")]!!
    assertEquals(1, p.correct)
    assertEquals(1, p.wrong)
    assertTrue(p.played)
    assertEquals(0.5 * 0.5 + 0.5 * 0.0, p.score, 1e-9)
  }

  @Test
  fun progressIsPerLevel() {
    var d = sampleData()
    d = Learning.recordAnswer(d, Levels.VOCAB_MATCH, "a", "one", true)
    val typeView = Learning.vocabularyView(d, Levels.VOCAB_TYPE)
    assertEquals(0.0, typeView.score("a", "one"), 0.0)
    assertFalse(typeView.played("a", "one"))
  }

  @Test
  fun percentageCountsUnplayedAsZero() {
    var d = sampleData() // 5 woorden
    repeat(20) { for ((g, ids) in Learning.vocabularyGroups(d)) ids.forEach { d = Learning.recordAnswer(d, level, g, it, true) } }
    assertEquals(1.0, Learning.vocabularyView(d, level).percentage(), 0.01)
    var one = sampleData()
    repeat(2) { one = Learning.recordAnswer(one, level, "a", "one", true) }
    assertEquals(1.0 / 5, Learning.vocabularyView(one, level).percentage(), 1e-9)
  }

  @Test
  fun levelReachedWhenPercentageHitsSeventy() {
    var d = AppData(lists = listOf(WordList("a", "A", listOf(w("one", "een")))))
    assertFalse(level in d.levelReached)
    d = Learning.recordAnswer(d, level, "a", "one", true) // 0.5
    assertFalse(level in d.levelReached)
    d = Learning.recordAnswer(d, level, "a", "one", true) // 0.75
    assertTrue(level in d.levelReached)
    assertTrue(Learning.vocabularyView(d, level).examAvailable())
  }

  @Test
  fun levelsUnlockOneByOneThroughExams() {
    var d = sampleData()
    assertTrue(Learning.levelUnlocked(d, Levels.VOCAB_MATCH))
    assertFalse(Learning.levelUnlocked(d, Levels.VOCAB_CHOICE))
    assertFalse(Learning.levelUnlocked(d, Levels.VOCAB_TYPE))
    d = Learning.markExamPassed(d, Levels.VOCAB_MATCH)
    assertTrue(Learning.levelUnlocked(d, Levels.VOCAB_CHOICE))
    assertFalse(Learning.levelUnlocked(d, Levels.VOCAB_TYPE))
    d = Learning.markExamPassed(d, Levels.VOCAB_CHOICE)
    assertTrue(Learning.levelUnlocked(d, Levels.VOCAB_TYPE))
  }

  @Test
  fun mixedUnlocksAfterAllListsPlayed() {
    var d = sampleData()
    assertFalse(Learning.vocabularyView(d, level).mixedUnlocked())
    d = Learning.finishSession(d, level, mapOf("a" to true))
    assertFalse(Learning.vocabularyView(d, level).mixedUnlocked())
    d = Learning.finishSession(d, level, mapOf("b" to false))
    assertTrue(Learning.vocabularyView(d, level).mixedUnlocked())
  }

  @Test
  fun combinedSessionCountsForEachList() {
    val d = Learning.finishSession(sampleData(), level, mapOf("a" to true, "b" to false))
    val v = Learning.vocabularyView(d, level)
    assertTrue(v.groupPlayed("a"))
    assertTrue(v.groupPlayed("b"))
    assertEquals(ListColor.GOLD, v.groupColor("a"))
    assertEquals(ListColor.RED, v.groupColor("b"))
  }

  @Test
  fun goldIsLostAfterImperfectSession() {
    var d = Learning.finishSession(sampleData(), level, mapOf("a" to true))
    d = Learning.finishSession(d, level, mapOf("a" to false))
    val s = Learning.vocabularyView(d, level).groupSession("a")
    assertEquals(2, s.completed)
    assertFalse(s.lastPerfect)
    assertEquals(1, s.perfectCount)
  }

  @Test
  fun untouchedListIsGrey() {
    assertEquals(ListColor.GREY, Learning.vocabularyView(sampleData(), level).groupColor("a"))
  }

  @Test
  fun wordColorFollowsScore() {
    var d = sampleData()
    d = Learning.recordAnswer(d, level, "a", "one", false)
    assertEquals(WordColor.RED, Learning.vocabularyView(d, level).wordColor("a", "one"))
    repeat(6) { d = Learning.recordAnswer(d, level, "a", "one", true) }
    assertEquals(WordColor.GREEN, Learning.vocabularyView(d, level).wordColor("a", "one"))
    assertEquals(WordColor.GREY, Learning.vocabularyView(d, level).wordColor("a", "two"))
  }
}
