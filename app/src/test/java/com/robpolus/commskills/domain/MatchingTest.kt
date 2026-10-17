package com.robpolus.commskills.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchingTest {

  private val words = listOf(w("one", "een"), w("two", "twee"), w("three", "drie"))

  @Test
  fun correctPairIsMatchedFromEitherSide() {
    val r = MatchRound(words, Random(1))
    assertEquals(MatchEvent.Selected, r.select(Side.EN, "one"))
    assertEquals(MatchEvent.Matched("one", "one"), r.select(Side.NL, "one"))
    assertEquals(MatchEvent.Selected, r.select(Side.NL, "two"))
    assertEquals(MatchEvent.Matched("two", "two"), r.select(Side.EN, "two"))
    assertFalse(r.isComplete)
    r.select(Side.EN, "three"); r.select(Side.NL, "three")
    assertTrue(r.isComplete)
    assertTrue(r.mistakenIds.isEmpty())
  }

  @Test
  fun wrongPairIsMistakeForBothWords() {
    val r = MatchRound(words, Random(1))
    r.select(Side.EN, "one")
    assertEquals(MatchEvent.Mismatch("one", "two"), r.select(Side.NL, "two"))
    assertEquals(setOf("one", "two"), r.mistakenIds)
    assertFalse(r.isMatched(Side.EN, "one"))
  }

  @Test
  fun matchedTileIsIgnored() {
    val r = MatchRound(words, Random(1))
    r.select(Side.EN, "one"); r.select(Side.NL, "one")
    assertEquals(MatchEvent.Ignored, r.select(Side.EN, "one"))
  }

  @Test
  fun selectingSameSideAgainReplacesSelection() {
    val r = MatchRound(words, Random(1))
    r.select(Side.EN, "one")
    r.select(Side.EN, "two")
    assertEquals(MatchEvent.Matched("two", "two"), r.select(Side.NL, "two"))
  }

  @Test
  fun sameEnglishTextWithDifferentTypeAcceptsEitherDutchTile() {
    val noun = Word("decrease_n", "decrease", "n", "", listOf("afname", "daling"))
    val verb = Word("decrease_v", "decrease", "v", "", listOf("afnemen", "dalen"))
    val r = MatchRound(listOf(noun, verb), Random(1))
    // het naamwoord-tegeltje aan het werkwoord koppelen is ook juist
    r.select(Side.EN, "decrease_n")
    assertTrue(r.select(Side.NL, "decrease_v") is MatchEvent.Matched)
    r.select(Side.EN, "decrease_v")
    assertTrue(r.select(Side.NL, "decrease_n") is MatchEvent.Matched)
    assertTrue(r.isComplete)
    assertTrue(r.mistakenIds.isEmpty())
    assertEquals("decrease (n)", r.enLabel(noun))
    assertEquals("decrease (v)", r.enLabel(verb))
  }

  @Test
  fun typeIsOnlyShownWhenEnglishTextIsDuplicated() {
    val a = Word("a_n", "chair", "n", "", listOf("stoel"))
    val r = MatchRound(listOf(a, w("table", "tafel")), Random(1))
    assertEquals("chair", r.enLabel(a))
  }

  @Test
  fun identicalDutchTextAcceptsEitherTile() {
    val twin = listOf(w("improve", "verbeteren"), w("enhance", "verbeteren"))
    val r = MatchRound(twin, Random(1))
    r.select(Side.EN, "improve")
    assertTrue(r.select(Side.NL, "enhance") is MatchEvent.Matched)
    r.select(Side.EN, "enhance")
    assertTrue(r.select(Side.NL, "improve") is MatchEvent.Matched)
    assertTrue(r.isComplete)
  }

  private fun solve(s: MatchSession, failOn: Set<String> = emptySet()): List<RoundOutcome> {
    val outcomes = mutableListOf<RoundOutcome>()
    while (!s.finished) {
      val round = s.round!!
      for (word in round.words.sortedBy { it.id !in failOn }) {
        if (word.id in failOn && word.id !in round.mistakenIds) {
          val other = round.words.first { it.id != word.id && !round.isMatched(Side.NL, it.id) }
          s.select(Side.EN, word.id); s.select(Side.NL, other.id) // bewust fout
        }
        s.select(Side.EN, word.id); s.select(Side.NL, word.id)
      }
      outcomes += s.advance()
    }
    return outcomes
  }

  private fun entries(n: Int) = (1..n).map { Entry("a", w("w$it", "n$it")) }

  @Test
  fun perfectSessionHasNoRepeat() {
    val s = MatchSession(entries(8), random = Random(1))
    val out = solve(s)
    assertEquals(2, out.size)
    assertTrue(s.isPerfect)
    assertTrue(out.flatMap { it.scored }.all { it.second })
    assertEquals(mapOf("a" to true), s.perfectByGroup())
  }

  @Test
  fun mistakenWordIsRepeatedOnceAndOnlyFirstTryScores() {
    val s = MatchSession(entries(8), random = Random(2))
    val out = solve(s, failOn = setOf("w3"))
    val scored = out.flatMap { it.scored }
    assertEquals(8, scored.size)
    // een foute koppeling telt als fout voor beide betrokken woorden
    assertEquals(2, scored.count { !it.second })
    assertTrue("w3" in scored.filter { !it.second }.map { it.first.word.id })
    assertEquals(3, out.size) // twee rondes + één herhaling
    assertFalse(s.isPerfect)
    assertEquals(mapOf("a" to false), s.perfectByGroup())
  }

  @Test
  fun examSessionHasNoRepeat() {
    val s = MatchSession(entries(8), repeatMistakes = false, random = Random(3))
    val out = solve(s, failOn = setOf("w1"))
    assertEquals(2, out.size)
    assertTrue(s.finished)
  }

  @Test
  fun emptySessionIsFinished() {
    assertTrue(MatchSession(emptyList()).finished)
  }

  @Test
  fun partialOutcomeOnlyHasTouchedWords() {
    val s = MatchSession(entries(4), repeatMistakes = false, random = Random(4))
    val round = s.round!!
    val ids = round.words.map { it.id }
    s.select(Side.EN, ids[0]); s.select(Side.NL, ids[0])           // juist
    s.select(Side.EN, ids[1]); s.select(Side.NL, ids[2])           // fout: ids[1] en ids[2]
    val partial = s.partialOutcome().associate { it.first.word.id to it.second }
    assertEquals(mapOf(ids[0] to true, ids[1] to false, ids[2] to false), partial)
  }
}
