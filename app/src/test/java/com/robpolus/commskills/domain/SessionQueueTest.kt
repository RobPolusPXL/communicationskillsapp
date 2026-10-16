package com.robpolus.commskills.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionQueueTest {

  @Test
  fun perfectSession() {
    val q = SessionQueue(listOf("a", "b", "c"), Random(1))
    repeat(3) { q.answer(true) }
    assertTrue(q.isFinished)
    assertTrue(q.isPerfect)
    assertEquals(3, q.completed)
    assertEquals(0, q.mistakes)
    assertNull(q.current())
  }

  @Test
  fun wrongItemComesBackLater() {
    val q = SessionQueue((1..10).map { "w$it" }, Random(2))
    assertEquals("w1", q.current())
    q.answer(false)
    // w1 zit 3 à 5 posities verder in de resterende rij
    val upcoming = generateSequence { q.current()?.also { q.answer(true) } }.toList()
    val index = upcoming.indexOf("w1")
    assertTrue(index in 3..5)
  }

  @Test
  fun onlyFirstTryCountsAsMistake() {
    val q = SessionQueue(listOf("a", "b"), Random(3))
    q.answer(false) // a fout (eerste poging)
    q.answer(true) // b juist
    assertEquals("a", q.current())
    q.answer(false) // a opnieuw fout: telt niet dubbel
    assertEquals(1, q.mistakes)
    assertFalse(q.isFinished)
    q.answer(true)
    assertTrue(q.isFinished)
    assertFalse(q.isPerfect)
    assertEquals(1, q.mistakes)
  }

  @Test
  fun progressCountsCompletedItems() {
    val q = SessionQueue(listOf("a", "b"), Random(4))
    q.answer(false)
    assertEquals(0, q.completed)
    q.answer(true)
    assertEquals(1, q.completed)
    assertEquals(2, q.total)
  }

  @Test
  fun weightedOrderKeepsAllItems() {
    val items = (1..20).toList()
    val ordered = Sessions.weightedOrder(items, { 1.0 }, Random(5))
    assertEquals(items.toSet(), ordered.toSet())
    assertEquals(20, ordered.size)
  }

  @Test
  fun heavyItemsComeEarlierOnAverage() {
    val items = (1..40).toList()
    // items 1..10 hebben gewicht 4, de rest gewicht 1
    var heavySum = 0.0
    var lightSum = 0.0
    val runs = 300
    repeat(runs) { run ->
      val ordered = Sessions.weightedOrder(items, { if (it <= 10) 4.0 else 1.0 }, Random(run))
      heavySum += ordered.withIndex().filter { it.value <= 10 }.map { it.index }.average()
      lightSum += ordered.withIndex().filter { it.value > 10 }.map { it.index }.average()
    }
    assertTrue(heavySum / runs < lightSum / runs - 5)
  }

  @Test
  fun examQuestionsAreUniqueAndLimited() {
    val items = (1..30).toList()
    val questions = Sessions.examQuestions(items, 12, { 1.0 }, Random(6))
    assertEquals(12, questions.size)
    assertEquals(12, questions.toSet().size)
  }
}
