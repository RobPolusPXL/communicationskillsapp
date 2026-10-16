package com.robpolus.commskills.data

import com.robpolus.commskills.domain.AppData
import com.robpolus.commskills.domain.DailyStat
import com.robpolus.commskills.domain.SessionStat
import com.robpolus.commskills.domain.Settings
import com.robpolus.commskills.domain.StreakState
import com.robpolus.commskills.domain.Word
import com.robpolus.commskills.domain.WordList
import com.robpolus.commskills.domain.WordProgress
import org.junit.Assert.assertEquals
import org.junit.Test

class CodecTest {

  private val full = AppData(
    lists = listOf(
      WordList(
        "a", "A",
        listOf(
          Word("pursue_v", "to pursue", "v", "follow \"quoted\" \n text", listOf("nastreven", "(na)streven")),
          Word("road", "road", null, "", listOf("weg")),
        ),
      ),
    ),
    progress = mapOf("vocab.match|a|road" to WordProgress(0.51, 3, 1, true)),
    sessions = mapOf("vocab.match|a" to SessionStat(2, true, 1)),
    examPassed = setOf("vocab.match"),
    levelReached = setOf("vocab.match", "vocab.type"),
    streak = StreakState(4, 9, "2026-10-16"),
    daily = mapOf("2026-10-16" to DailyStat(612, 14)),
    settings = Settings(true, 20, 15),
    seededAssets = setOf("x.docx"),
    updatedAt = 12345L,
  )

  @Test
  fun roundTripKeepsEverything() {
    assertEquals(full, Codec.decode(Codec.encode(full)))
  }

  @Test
  fun emptyDataRoundTrips() {
    assertEquals(AppData(), Codec.decode(Codec.encode(AppData())))
  }

  @Test
  fun nullStreakDateRoundTrips() {
    val d = AppData(streak = StreakState(0, 0, null))
    assertEquals(d, Codec.decode(Codec.encode(d)))
  }

  @Test
  fun missingFieldsFallBackToDefaults() {
    assertEquals(AppData(), Codec.decode("{\"version\":1}"))
  }
}
