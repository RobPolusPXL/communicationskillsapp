package com.robpolus.commskills.domain

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemindersTest {
  private val now = LocalDateTime.of(2026, 10, 16, 18, 30)

  @Test
  fun triggersTodayWhenStillAhead() {
    assertEquals(LocalDateTime.of(2026, 10, 16, 19, 0), Reminders.nextTrigger(now, 19, 0))
  }

  @Test
  fun triggersTomorrowWhenPassedOrNow() {
    assertEquals(LocalDateTime.of(2026, 10, 17, 8, 15), Reminders.nextTrigger(now, 8, 15))
    assertEquals(LocalDateTime.of(2026, 10, 17, 18, 30), Reminders.nextTrigger(now, 18, 30))
  }

  @Test
  fun skipsWhenDisabledOrAlreadyPracticed() {
    val today = LocalDate.of(2026, 10, 16)
    val on = AppData(settings = Settings(reminderEnabled = true))
    assertTrue(Reminders.shouldNotify(on, today))
    assertFalse(Reminders.shouldNotify(AppData(), today))
    assertFalse(Reminders.shouldNotify(Stats.registerExercise(on, today), today))
    assertTrue(Reminders.shouldNotify(Stats.registerExercise(on, today.minusDays(1)), today))
  }
}
