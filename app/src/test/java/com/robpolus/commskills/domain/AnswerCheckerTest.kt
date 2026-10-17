package com.robpolus.commskills.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerCheckerTest {

  private val checker = AnswerChecker()

  private fun word(en: String, vararg nl: String, type: String? = null) =
    Word(id = en, en = en, type = type, definition = "", nl = nl.toList())

  private fun correct(vararg notes: Note) = AnswerResult.Correct(notes.toSet())

  // --- EN → NL ------------------------------------------------------------------------------

  @Test
  fun exactAnswerIsCorrect() {
    val w = word("to pursue", "nastreven")
    assertEquals(correct(), checker.checkWord("nastreven", w, Direction.EN_NL))
  }

  @Test
  fun capitalsAreIgnoredWhenReferenceHasNone() {
    val w = word("to pursue", "nastreven")
    assertEquals(correct(), checker.checkWord("Nastreven", w, Direction.EN_NL))
    assertEquals(correct(), checker.checkWord("  nastreven  ", w, Direction.EN_NL))
  }

  @Test
  fun oneTranslationIsEnough() {
    val w = word("consignment", "consignatiezending", "verzending", "depotgoederen")
    assertTrue(checker.checkWord("verzending", w, Direction.EN_NL).isCorrect)
    assertTrue(checker.checkWord("depotgoederen", w, Direction.EN_NL).isCorrect)
  }

  @Test
  fun wrongAnswerShowsAllCorrectTranslations() {
    val w = word("consignment", "consignatiezending", "verzending")
    val result = checker.checkWord("pakket", w, Direction.EN_NL)
    assertEquals(AnswerResult.Wrong("consignatiezending, verzending"), result)
  }

  @Test
  fun missingAccentIsCorrectWithNote() {
    val w = word("egotistical", "egoïstisch")
    assertEquals(correct(Note.MISSING_ACCENT), checker.checkWord("egoistisch", w, Direction.EN_NL))
    assertEquals(correct(), checker.checkWord("egoïstisch", w, Direction.EN_NL))
  }

  @Test
  fun hyphenAndSpaceAreInterchangeable() {
    val w = word("risk-averse", "risico-avers")
    assertTrue(checker.checkWord("risico avers", w, Direction.EN_NL).isCorrect)
    assertTrue(checker.checkWord("risico-avers", w, Direction.EN_NL).isCorrect)
  }

  // --- NL → EN ------------------------------------------------------------------------------

  @Test
  fun missingToIsCorrectWithNote() {
    val w = word("to pursue", "nastreven", type = "v")
    assertEquals(correct(Note.MISSING_TO), checker.checkWord("pursue", w, Direction.NL_EN))
  }

  @Test
  fun withToHasNoNote() {
    val w = word("to pursue", "nastreven", type = "v")
    assertEquals(correct(), checker.checkWord("to pursue", w, Direction.NL_EN))
    assertEquals(correct(), checker.checkWord("To Pursue", w, Direction.NL_EN))
  }

  @Test
  fun articleIsOptionalWithoutNote() {
    val w = word("a trolley", "een tram")
    assertEquals(correct(), checker.checkWord("trolley", w, Direction.NL_EN))
    assertEquals(correct(), checker.checkWord("a trolley", w, Direction.NL_EN))
  }

  @Test
  fun textInParenthesesIsOptionalInEnglish() {
    val w = word("touch (upon)", "aanstippen")
    assertEquals(correct(), checker.checkWord("touch", w, Direction.NL_EN))
    assertEquals(correct(), checker.checkWord("touch upon", w, Direction.NL_EN))
    assertEquals(AnswerResult.Wrong("touch (upon)"), checker.checkWord("touch on", w, Direction.NL_EN))
  }

  @Test
  fun textInParenthesesIsOptionalInDutch() {
    val w = word("to pursue", "(na)streven", "streven(d) naar")
    assertTrue(checker.checkWord("nastreven", w, Direction.EN_NL).isCorrect)
    assertTrue(checker.checkWord("streven", w, Direction.EN_NL).isCorrect)
    assertTrue(checker.checkWord("strevend naar", w, Direction.EN_NL).isCorrect)
    assertTrue(checker.checkWord("streven naar", w, Direction.EN_NL).isCorrect)
  }

  @Test
  fun optionalToInsideParenthesesWithVerb() {
    val w = word("to aspire (to)", "streven naar")
    assertEquals(correct(), checker.checkWord("to aspire to", w, Direction.NL_EN))
    assertEquals(correct(), checker.checkWord("to aspire", w, Direction.NL_EN))
    assertEquals(correct(Note.MISSING_TO), checker.checkWord("aspire to", w, Direction.NL_EN))
  }

  @Test
  fun multiWordVerbWithTo() {
    val w = word("to be used to", "gewend zijn aan")
    assertEquals(correct(Note.MISSING_TO), checker.checkWord("be used to", w, Direction.NL_EN))
    assertEquals(correct(), checker.checkWord("to be used to", w, Direction.NL_EN))
  }

  @Test
  fun wrongEnglishAnswerShowsWordWithTo() {
    val w = word("to pursue", "nastreven")
    assertEquals(AnswerResult.Wrong("to pursue"), checker.checkWord("follow", w, Direction.NL_EN))
  }

  // --- Hoofdletters -------------------------------------------------------------------------

  @Test
  fun requiredCapitalMissingIsWrong() {
    val w = word("Monday", "maandag")
    assertFalse(checker.checkWord("monday", w, Direction.NL_EN).isCorrect)
    assertEquals(correct(), checker.checkWord("Monday", w, Direction.NL_EN))
  }

  @Test
  fun capitalInReferenceDoesNotMakeOtherWordsCaseSensitive() {
    val w = word("the United States", "de Verenigde Staten")
    assertTrue(checker.checkWord("The United States", w, Direction.NL_EN).isCorrect)
    assertFalse(checker.checkWord("the united States", w, Direction.NL_EN).isCorrect)
  }

  // --- Typo's -------------------------------------------------------------------------------

  @Test
  fun neighbourKeySubstitutionIsATypo() {
    val w = word("to offer", "aanbieden")
    // z ligt naast a op AZERTY
    assertEquals(correct(Note.TYPO), checker.checkWord("zanbieden", w, Direction.EN_NL))
  }

  @Test
  fun nonNeighbourSubstitutionIsWrong() {
    val w = word("to offer", "aanbieden")
    assertFalse(checker.checkWord("manbieden", w, Direction.EN_NL).isCorrect)
  }

  @Test
  fun swappedLettersAreWrong() {
    val road = word("road", "weg")
    assertFalse(checker.checkWord("raod", road, Direction.NL_EN).isCorrect)
    assertFalse(checker.checkWord("gew", road, Direction.EN_NL).isCorrect)
  }

  @Test
  fun missingOrExtraLetterIsWrong() {
    val w = word("to offer", "aanbieden")
    assertFalse(checker.checkWord("aanbiedn", w, Direction.EN_NL).isCorrect)
    assertFalse(checker.checkWord("aanbiedden", w, Direction.EN_NL).isCorrect)
  }

  @Test
  fun typoInRequiredCapitalKeepsCapitalRule() {
    val w = word("Monday", "maandag")
    assertFalse(checker.checkWord("monfay", w, Direction.NL_EN).isCorrect)
    assertEquals(correct(Note.TYPO), checker.checkWord("Monfay", w, Direction.NL_EN))
  }

  // --- Grammar ------------------------------------------------------------------------------

  @Test
  fun grammarAnswerIsExact() {
    assertEquals(correct(), checker.check("were", listOf("were")))
    assertFalse(checker.check("was", listOf("were")).isCorrect)
  }

  @Test
  fun grammarAcceptsVariants() {
    val accepted = listOf("would have gone", "'d have gone")
    assertTrue(checker.check("would have gone", accepted).isCorrect)
    assertTrue(checker.check("d have gone", accepted).isCorrect)
  }

  @Test
  fun grammarDoesNotStripToOrArticles() {
    assertFalse(checker.check("to be", listOf("be")).isCorrect)
  }

  @Test
  fun emptyAnswerIsWrong() {
    assertFalse(checker.check("   ", listOf("were")).isCorrect)
  }
}
