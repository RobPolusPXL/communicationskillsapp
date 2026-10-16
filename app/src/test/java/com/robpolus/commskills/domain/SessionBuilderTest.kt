package com.robpolus.commskills.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionBuilderTest {

  @Test
  fun roundsAreBalanced() {
    assertEquals(listOf(6, 6, 5), SessionBuilder.rounds((1..17).toList()).map { it.size })
    assertEquals(listOf(4, 4, 4), SessionBuilder.rounds((1..12).toList(), 5).map { it.size })
    assertEquals(listOf(6), SessionBuilder.rounds((1..6).toList()).map { it.size })
    assertEquals(listOf(4, 3), SessionBuilder.rounds((1..7).toList()).map { it.size })
    assertTrue(SessionBuilder.rounds(emptyList<Int>()).isEmpty())
  }

  @Test
  fun entriesCoverChosenListsOnly() {
    val e = SessionBuilder.vocabularyEntries(sampleData(), Levels.VOCAB_TYPE, listOf("b"), Random(1))
    assertEquals(setOf("four", "five"), e.map { it.word.id }.toSet())
    val all = SessionBuilder.vocabularyEntries(sampleData(), Levels.VOCAB_TYPE, listOf("a", "b"), Random(1))
    assertEquals(5, all.size)
  }

  @Test
  fun weakWordsComeEarlierOnAverage() {
    var d = AppData(lists = listOf(WordList("a", "A", (1..30).map { w("w$it", "n$it") })))
    // alle woorden goed behalve w1..w5
    for (i in 6..30) repeat(10) { d = Learning.recordAnswer(d, Levels.VOCAB_TYPE, "a", "w$i", true) }
    for (i in 1..5) d = Learning.recordAnswer(d, Levels.VOCAB_TYPE, "a", "w$i", false)
    var weakSum = 0.0
    var strongSum = 0.0
    repeat(200) { run ->
      val order = SessionBuilder.vocabularyEntries(d, Levels.VOCAB_TYPE, listOf("a"), Random(run)).map { it.word.id }
      weakSum += (1..5).map { order.indexOf("w$it") }.average()
      strongSum += (6..30).map { order.indexOf("w$it") }.average()
    }
    assertTrue(weakSum / 200 < strongSum / 200 - 3)
  }

  @Test
  fun examEntriesAreUnique() {
    val e = SessionBuilder.examEntries(sampleData(), Levels.VOCAB_MATCH, 4, Random(2))
    assertEquals(4, e.size)
    assertEquals(4, e.map { it.word.id }.toSet().size)
  }
}
