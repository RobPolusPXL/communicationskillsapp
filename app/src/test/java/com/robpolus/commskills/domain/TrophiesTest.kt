package com.robpolus.commskills.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrophiesTest {
  private fun earned(d: AppData) = Trophies.earnedIds(d)

  @Test
  fun nothingEarnedAtStart() {
    assertTrue(earned(AppData()).isEmpty())
  }

  @Test
  fun streakTrophiesFollowLongestStreak() {
    val d = AppData(streak = StreakState(current = 1, longest = 8, lastDate = "2026-10-01"))
    assertTrue("streak_3" in earned(d))
    assertTrue("streak_7" in earned(d))
    assertFalse("streak_14" in earned(d))
  }

  @Test
  fun dailyGoalCountsDays() {
    val d = AppData(daily = mapOf("a" to DailyStat(600, 3), "b" to DailyStat(700, 1), "c" to DailyStat(100, 1)))
    val t = Trophies.all(d).first { it.id == "daily_goal" }
    assertTrue(t.earned)
    assertEquals(2, t.count)
  }

  @Test
  fun levelExamAndPerfectTrophies() {
    val d = AppData(
      levelReached = setOf(Levels.VOCAB_MATCH),
      examPassed = setOf(Levels.VOCAB_MATCH),
      sessions = mapOf("x|a" to SessionStat(2, true, 2), "x|b" to SessionStat(1, true, 1)),
    )
    val e = earned(d)
    assertTrue("level_${Levels.VOCAB_MATCH}" in e)
    assertTrue("exam_${Levels.VOCAB_MATCH}" in e)
    assertFalse("exam_${Levels.VOCAB_TYPE}" in e)
    assertEquals(3, Trophies.all(d).first { it.id == "perfect" }.count)
  }

  @Test
  fun newlyEarnedDetectsNewAndIncrementedCounters() {
    val before = AppData()
    val after = Stats.registerExercise(before, LocalDate.of(2026, 10, 1))
    assertTrue(Trophies.newlyEarned(before, after).isEmpty())

    val day1 = AppData(daily = mapOf("a" to DailyStat(600, 1)))
    val day2 = AppData(daily = mapOf("a" to DailyStat(600, 1), "b" to DailyStat(600, 1)))
    assertEquals(listOf("daily_goal"), Trophies.newlyEarned(AppData(), day1).map { it.id })
    assertEquals(listOf("daily_goal"), Trophies.newlyEarned(day1, day2).map { it.id })
    assertTrue(Trophies.newlyEarned(day2, day2).isEmpty())
  }
}
