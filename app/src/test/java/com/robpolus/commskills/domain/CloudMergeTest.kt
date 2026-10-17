package com.robpolus.commskills.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CloudMergeTest {
  private val progress = mapOf("a|b|c" to WordProgress(0.3, 1, 0, true))

  @Test
  fun emptyCloudGetsLocal() {
    assertEquals(CloudMerge.Decision.PUSH_LOCAL, CloudMerge.decide(AppData(updatedAt = 5), null))
  }

  @Test
  fun freshDeviceTakesCloudEvenIfLocalIsNewer() {
    val local = AppData(updatedAt = 100) // alleen net ingelezen bundel-lijsten
    val cloud = AppData(progress = progress, updatedAt = 50)
    assertEquals(CloudMerge.Decision.USE_CLOUD, CloudMerge.decide(local, cloud))
  }

  @Test
  fun newerSideWins() {
    val older = AppData(progress = progress, updatedAt = 10)
    val newer = AppData(progress = progress, updatedAt = 20)
    assertEquals(CloudMerge.Decision.USE_CLOUD, CloudMerge.decide(older, newer))
    assertEquals(CloudMerge.Decision.PUSH_LOCAL, CloudMerge.decide(newer, older))
    assertEquals(CloudMerge.Decision.NOTHING, CloudMerge.decide(newer, newer))
  }

  @Test
  fun freshLocalAndFreshCloudFallBackToTimestamps() {
    assertEquals(CloudMerge.Decision.PUSH_LOCAL, CloudMerge.decide(AppData(updatedAt = 9), AppData(updatedAt = 3)))
  }
}
