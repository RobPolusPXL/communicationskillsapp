package com.robpolus.commskills.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlockingTest {

  @Test
  fun mixedStaysLockedUntilAllListsArePlayed() {
    assertFalse(Unlocking.mixedUnlocked(listOf(true, false, true)))
    assertFalse(Unlocking.mixedUnlocked(emptyList()))
    assertTrue(Unlocking.mixedUnlocked(listOf(true, true, true)))
  }

  @Test
  fun nextLevelNeedsPassedExam() {
    assertFalse(Unlocking.nextLevelUnlocked(previousExamPassed = false))
    assertTrue(Unlocking.nextLevelUnlocked(previousExamPassed = true))
  }
}
