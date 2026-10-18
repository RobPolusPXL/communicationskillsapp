package com.robpolus.commskills.domain

import org.junit.Test
import org.junit.Assert.assertEquals

class TipsTest {
  @Test
  fun patternRevealsLettersFromTheStart() {
    assertEquals("_ _ _ _ _ _", Tips.pattern("afname", 0))
    assertEquals("a _ _ _ _ _", Tips.pattern("afname", 1))
    assertEquals("a f n _ _ _", Tips.pattern("afname", 3))
  }

  @Test
  fun neverRevealsTheWholeAnswer() {
    assertEquals(5, Tips.maxReveal("afname"))
    assertEquals("a f n a m _", Tips.pattern("afname", 99))
  }

  @Test
  fun keepsWordsApartAndCountsLettersOnly() {
    assertEquals("t o   _ _ _ _ _ _", Tips.pattern("to pursue", 2))
    assertEquals(7, Tips.maxReveal("to pursue"))
  }
}
