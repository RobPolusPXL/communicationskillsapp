package com.robpolus.commskills.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsTest {
  private val d1 = LocalDate.of(2026, 10, 1)

  @Test
  fun firstExerciseStartsStreak() {
    val d = Stats.registerExercise(AppData(), d1)
    assertEquals(1, d.streak.current)
    assertEquals(1, d.daily[d1.toString()]!!.exercises)
  }

  @Test
  fun sameDayDoesNotIncreaseStreak() {
    var d = Stats.registerExercise(AppData(), d1)
    d = Stats.registerExercise(d, d1)
    assertEquals(1, d.streak.current)
    assertEquals(2, d.daily[d1.toString()]!!.exercises)
  }

  @Test
  fun consecutiveDaysIncreaseStreakAndGapResets() {
    var d = Stats.registerExercise(AppData(), d1)
    d = Stats.registerExercise(d, d1.plusDays(1))
    d = Stats.registerExercise(d, d1.plusDays(2))
    assertEquals(3, d.streak.current)
    d = Stats.registerExercise(d, d1.plusDays(5))
    assertEquals(1, d.streak.current)
    assertEquals(3, d.streak.longest)
  }

  @Test
  fun effectiveStreakIsZeroAfterMissedDay() {
    val d = Stats.registerExercise(Stats.registerExercise(AppData(), d1), d1.plusDays(1))
    assertEquals(2, Stats.effectiveStreak(d.streak, d1.plusDays(1)))
    assertEquals(2, Stats.effectiveStreak(d.streak, d1.plusDays(2)))
    assertEquals(0, Stats.effectiveStreak(d.streak, d1.plusDays(3)))
  }

  @Test
  fun dailyGoalAtTenMinutes() {
    var d = Stats.addActiveSeconds(AppData(), d1, 599)
    assertFalse(Stats.dailyGoalReached(d, d1))
    d = Stats.addActiveSeconds(d, d1, 1)
    assertTrue(Stats.dailyGoalReached(d, d1))
    assertEquals(600, Stats.todaySeconds(d, d1))
    assertFalse(Stats.dailyGoalReached(d, d1.plusDays(1)))
  }

  @Test
  fun activeTimerCapsPausesAtThirtySeconds() {
    val t = ActiveTimer()
    assertEquals(0, t.activity(0))
    assertEquals(10, t.activity(10_000))
    assertEquals(30, t.activity(10_000 + 120_000)) // lange pauze telt max. 30 s
  }

  @Test
  fun activeTimerCarriesFractions() {
    val t = ActiveTimer()
    t.activity(0)
    assertEquals(0, t.activity(600))
    assertEquals(1, t.activity(1_200))
    t.reset()
    assertEquals(0, t.activity(50_000))
  }

  @Test
  fun practicedToday() {
    assertFalse(Stats.practicedToday(AppData(), d1))
    assertTrue(Stats.practicedToday(Stats.registerExercise(AppData(), d1), d1))
  }
}
