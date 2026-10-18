package com.robpolus.commskills.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.robpolus.commskills.CommSkillsApp
import com.robpolus.commskills.MainActivity
import com.robpolus.commskills.domain.Learning
import com.robpolus.commskills.domain.Levels
import com.robpolus.commskills.domain.Side
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/** Speelt volledige sessies en examens door de echte ViewModel en schermen. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h2600dp-xxhdpi")
class FlowTest {

  @get:Rule
  val rule = createAndroidComposeRule<MainActivity>()

  private val app get() = rule.activity.application as CommSkillsApp
  private val vm get() = ViewModelProvider(rule.activity)[PracticeViewModel::class.java]
  private val data get() = app.repository.data.value

  private fun waitForText(text: String) = rule.waitUntil(10_000) {
    rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
  }

  private fun dump() = "same=${vm.data === app.repository.data} vmProgress=${vm.data.value.progress.size} vmStreak=${vm.data.value.streak.current} lists=${data.lists.size} progress=${data.progress.size} sessions=${data.sessions.size} exam=${data.examPassed} streak=${data.streak} t=${data.updatedAt}"

  private fun idle() = ShadowLooper.idleMainLooper(3, TimeUnit.SECONDS)

  private fun toHome() {
    rule.waitUntil(15_000) { data.lists.size == 8 }
    waitForText("Vocabulary")
  }

  private fun openVocabularyList(title: String, button: String) {
    rule.onNodeWithText("Vocabulary").performClick()
    waitForText("Kies één of meer lijsten")
    rule.onNodeWithText(title).performClick()
    waitForText(button)
    rule.onNodeWithText(button).performClick()
  }

  /** Verbindt alle paren juist; `wrong` = aantal bewuste foute koppelingen vooraf. */
  private fun solveMatch(wrong: Int = 0) {
    var mistakes = wrong
    var guard = 0
    while (vm.match.value != null && guard++ < 300) {
      val ui = vm.match.value!!
      val open = ui.en.filter { !it.matched }
      val tile = open.first()
      rule.runOnIdle {
        if (mistakes > 0 && open.size > 1) {
          val other = ui.nl.first { !it.matched && it.id != tile.id }
          vm.selectTile(Side.EN, tile.id)
          vm.selectTile(Side.NL, other.id)
          mistakes--
        } else {
          vm.selectTile(Side.EN, tile.id)
          vm.selectTile(Side.NL, tile.id)
        }
      }
      idle()
      rule.waitForIdle()
    }
  }

  private fun correctAnswerForCurrent(): String {
    val ui = vm.type.value!!
    if (ui.grammar) return app.grammar.first { it.text == ui.prompt }.answers.first()
    val words = data.lists.flatMap { it.words }
    return if (ui.promptLabel.startsWith("Engels")) {
      words.first { it.en == ui.prompt }.nl.first()
    } else {
      words.first { it.nl.joinToString(", ") == ui.prompt }.en
    }
  }

  private fun typeUntilDone(correct: Boolean = true) {
    var guard = 0
    while (vm.type.value != null && guard++ < 400) {
      val answer = if (correct) correctAnswerForCurrent() else "zzz"
      rule.runOnIdle {
        vm.submitAnswer(answer)
        vm.nextQuestion()
      }
      idle()
      rule.waitForIdle()
    }
  }

  private fun makeExamEligible(level: String) {
    app.repository.update { d ->
      var next = d
      repeat(6) {
        for (list in d.lists) for (w in list.words) next = Learning.recordAnswer(next, level, list.id, w.id, true)
      }
      next
    }
    assertTrue(Learning.vocabularyView(data, level).examAvailable())
  }

  @Test
  fun perfectMatchSessionShowsResultAndTrophy() {
    toHome()
    openVocabularyList("Trolley problem", "Oefenen (23 woorden)")
    waitForText("Verbind de woorden die bij elkaar horen")
    solveMatch()
    waitForText("Foutloos!")
    val s = Learning.vocabularyView(data, Levels.VOCAB_MATCH).groupSession("trolley_problem")
    assertEquals(dump(), 1, s.completed)
    assertTrue(s.lastPerfect)
    assertEquals(1, data.streak.current)
    assertNotNull(vm.result.value)
    assertTrue(vm.result.value!!.newTrophies.any { it.id == "perfect" })
  }

  @Test
  fun matchMistakesAreRepeatedAndSessionIsNotPerfect() {
    toHome()
    openVocabularyList("Trolley problem", "Oefenen (23 woorden)")
    waitForText("Verbind de woorden die bij elkaar horen")
    solveMatch(wrong = 1)
    waitForText("Sessie klaar")
    val s = Learning.vocabularyView(data, Levels.VOCAB_MATCH).groupSession("trolley_problem")
    assertFalse(s.lastPerfect)
    assertEquals(2, vm.result.value!!.mistakes.size)
  }

  /** Beantwoordt het meerkeuze-examen; `wrong` = aantal bewust foute antwoorden. */
  private fun answerChoices(wrong: Int = 0) {
    var mistakes = wrong
    var guard = 0
    while (vm.choice.value != null && guard++ < 200) {
      val ui = vm.choice.value!!
      val words = data.lists.flatMap { it.words }
      val correctText = if (ui.label.startsWith("Engels")) words.first { it.en == ui.prompt }.nl.first()
      else words.first { it.nl.joinToString(", ") == ui.prompt }.en
      val correctIdx = ui.options.indexOf(correctText)
      val pick = if (mistakes > 0) { mistakes--; (correctIdx + 1) % ui.options.size } else correctIdx
      rule.runOnIdle { vm.pickOption(pick); vm.nextChoice() }
      idle()
      rule.waitForIdle()
      if (vm.result.value != null) break
    }
  }

  @Test
  fun testListIsSingleSimplePart() {
    toHome()
    val part = com.robpolus.commskills.domain.Parts.all(data).first { it.listId == "testlijst" }
    assertEquals(12, part.words.size)
  }

  @Test
  fun matchExamPassUnlocksChoiceLevelAndGivesBronze() {
    toHome()
    makeExamEligible(Levels.VOCAB_MATCH)
    assertFalse(Learning.levelUnlocked(data, Levels.VOCAB_CHOICE))
    rule.runOnIdle { vm.startPartExam(Levels.VOCAB_MATCH, "testlijst") }
    rule.runOnIdle { assertEquals(3, vm.exam.value!!.lives); assertEquals(12, vm.exam.value!!.total); assertTrue(vm.match.value != null) }
    solveMatch()
    rule.runOnIdle { assertEquals("Examen geslaagd!", vm.result.value!!.title) }
    assertTrue(Learning.levelUnlocked(data, Levels.VOCAB_CHOICE))
    assertEquals(1, data.medals[com.robpolus.commskills.domain.AppData.medalKey(Levels.VOCAB_MATCH, "testlijst")])
  }

  @Test
  fun choicePracticeHasNoLivesAndRepeatsMistakes() {
    toHome()
    rule.runOnIdle { vm.startChoice(Levels.VOCAB_CHOICE, listOf("testlijst")) }
    rule.runOnIdle { assertTrue(vm.exam.value == null); assertFalse(vm.choice.value!!.exam); assertEquals(12, vm.choice.value!!.total) }
    answerChoices(wrong = 2)
    rule.runOnIdle { assertEquals("Sessie klaar", vm.result.value!!.title); assertEquals(2, vm.result.value!!.mistakes.size) }
    assertTrue(data.medals.isEmpty())
    val view = Learning.vocabularyView(data, Levels.VOCAB_CHOICE)
    assertEquals(10.0 / 12, com.robpolus.commskills.domain.Parts.percent(view, com.robpolus.commskills.domain.Parts.all(data).first { it.listId == "testlijst" }), 1e-9)
  }

  @Test
  fun partExamPassUnlocksTypingLevelAndGivesBronze() {
    toHome()
    makeExamEligible(Levels.VOCAB_CHOICE)
    assertFalse(Learning.levelUnlocked(data, Levels.VOCAB_TYPE))
    rule.runOnIdle { vm.startPartExam(Levels.VOCAB_CHOICE, "testlijst") }
    rule.runOnIdle { assertEquals(3, vm.exam.value!!.lives); assertEquals(5, vm.choice.value!!.options.size) }
    answerChoices()
    rule.runOnIdle { assertEquals("Examen geslaagd!", vm.result.value!!.title) }
    assertTrue(dump(), Learning.levelUnlocked(data, Levels.VOCAB_TYPE))
    assertEquals(1, data.medals[com.robpolus.commskills.domain.AppData.medalKey(Levels.VOCAB_CHOICE, "testlijst")])
  }

  @Test
  fun partExamAllowsTwoMistakesForBronze() {
    toHome()
    makeExamEligible(Levels.VOCAB_CHOICE)
    rule.runOnIdle { vm.startPartExam(Levels.VOCAB_CHOICE, "testlijst") }
    answerChoices(wrong = 2)
    rule.runOnIdle { assertEquals("Examen geslaagd!", vm.result.value!!.title) }
  }

  @Test
  fun typingPartExamIsTypedAndGivesMedal() {
    toHome()
    makeExamEligible(Levels.VOCAB_TYPE)
    rule.runOnIdle { vm.startPartExam(Levels.VOCAB_TYPE, "testlijst") }
    rule.runOnIdle { assertEquals(12, vm.exam.value!!.total); assertTrue(vm.type.value != null); assertTrue(vm.choice.value == null) }
    typeUntilDone()
    rule.runOnIdle { assertEquals("Examen geslaagd!", vm.result.value!!.title) }
    assertEquals(1, data.medals[com.robpolus.commskills.domain.AppData.medalKey(Levels.VOCAB_TYPE, "testlijst")])
  }

  @Test
  fun typingPartExamFailsOnTwoMistakes() {
    toHome()
    makeExamEligible(Levels.VOCAB_TYPE)
    rule.runOnIdle { vm.startPartExam(Levels.VOCAB_TYPE, "testlijst") }
    typeUntilDone(correct = false)
    rule.runOnIdle { assertEquals("Examen niet gehaald", vm.result.value!!.title) }
    assertTrue(data.medals.keys.none { it.startsWith(Levels.VOCAB_TYPE) })
  }

  @Test
  fun goldExamFailsOnFirstMistakeAndPassesWhenPerfect() {
    toHome()
    makeExamEligible(Levels.VOCAB_CHOICE)
    val key = com.robpolus.commskills.domain.AppData.medalKey(Levels.VOCAB_CHOICE, "testlijst")
    app.repository.update { it.copy(medals = it.medals + (key to 2)) }
    rule.runOnIdle { vm.startPartExam(Levels.VOCAB_CHOICE, "testlijst") }
    rule.runOnIdle { assertEquals(1, vm.exam.value!!.lives) }
    answerChoices(wrong = 1)
    rule.runOnIdle { assertEquals("Examen niet gehaald", vm.result.value!!.title) }
    assertEquals(2, data.medals[key])
    rule.runOnIdle { vm.clearResult(); vm.startPartExam(Levels.VOCAB_CHOICE, "testlijst") }
    answerChoices()
    rule.runOnIdle { assertEquals("Examen geslaagd!", vm.result.value!!.title) }
    assertEquals(3, data.medals[key])
  }

  @Test
  fun acceptAnywayRemembersAnswerAndCountsAsCorrect() {
    toHome()
    rule.runOnIdle { vm.startType(Levels.VOCAB_TYPE, listOf("testlijst"), com.robpolus.commskills.domain.DirectionChoice.EN_NL) }
    rule.runOnIdle { vm.submitAnswer("zzz") }
    rule.runOnIdle { assertTrue(vm.type.value!!.feedback!!.canAccept) }
    rule.runOnIdle { vm.acceptAnyway() }
    rule.runOnIdle { assertTrue(vm.type.value!!.feedback!!.correct) }
    assertTrue(dump(), data.extraAnswers.values.any { "zzz" in it })
    assertTrue(data.progress.values.any { it.streak == 1 })
  }

  @Test
  fun acceptAnywayInExamGivesLifeBack() {
    toHome()
    makeExamEligible(Levels.VOCAB_TYPE)
    rule.runOnIdle { vm.startPartExam(Levels.VOCAB_TYPE, "testlijst") }
    rule.runOnIdle { vm.submitAnswer("zzz") }
    rule.runOnIdle { assertEquals(2, vm.exam.value!!.lives); assertTrue(vm.type.value!!.feedback!!.canAccept) }
    rule.runOnIdle { vm.acceptAnyway() }
    rule.runOnIdle { assertEquals(3, vm.exam.value!!.lives); assertTrue(vm.type.value!!.feedback!!.correct) }
  }

  @Test
  fun tipRevealsLettersAndHintedAnswerDoesNotCount() {
    toHome()
    rule.runOnIdle { vm.startType(Levels.VOCAB_TYPE, listOf("testlijst"), com.robpolus.commskills.domain.DirectionChoice.EN_NL) }
    rule.runOnIdle { assertTrue(vm.type.value!!.canTip); assertEquals(null, vm.type.value!!.tip) }
    rule.runOnIdle { vm.requestTip() }
    rule.runOnIdle { assertTrue(vm.type.value!!.tip!!.isNotBlank()) }
    val answer = correctAnswerForCurrent()
    rule.runOnIdle { vm.submitAnswer(answer) }
    rule.runOnIdle {
      assertTrue(vm.type.value!!.feedback!!.correct)
      assertTrue(com.robpolus.commskills.domain.Note.HINT in vm.type.value!!.feedback!!.notes)
    }
    assertTrue(dump(), data.progress.values.any { it.streak == 0 && it.wrong == 1 })
  }

  @Test
  fun giveUpShowsAnswerAndCountsWrong() {
    toHome()
    rule.runOnIdle { vm.startType(Levels.VOCAB_TYPE, listOf("testlijst"), com.robpolus.commskills.domain.DirectionChoice.EN_NL) }
    rule.runOnIdle { vm.giveUp() }
    rule.runOnIdle {
      val f = vm.type.value!!.feedback!!
      assertTrue(!f.correct && f.correctAnswer.isNotBlank())
    }
    assertTrue(data.progress.values.any { it.wrong == 1 })
  }

  @Test
  fun noTipsInExam() {
    toHome()
    makeExamEligible(Levels.VOCAB_TYPE)
    rule.runOnIdle { vm.startPartExam(Levels.VOCAB_TYPE, "testlijst") }
    rule.runOnIdle { assertTrue(!vm.type.value!!.canTip); vm.requestTip(); assertEquals(null, vm.type.value!!.tip) }
    rule.runOnIdle { vm.giveUp() }
    rule.runOnIdle { assertEquals(2, vm.exam.value!!.lives) }
  }

  @Test
  fun typingSessionPerfect() {
    toHome()
    rule.runOnIdle { vm.startType(Levels.VOCAB_TYPE, listOf("trolley_problem"), com.robpolus.commskills.domain.DirectionChoice.MIXED) }
    typeUntilDone()
    rule.runOnIdle { assertEquals("Foutloos!", vm.result.value!!.title) }
  }

  @Test
  fun grammarSessionMixedUnlockAndExam() {
    toHome()
    // Elk type één keer spelen; daarna gaat "Gemixt" open.
    for (t in 0..3) {
      rule.runOnIdle { vm.startGrammar(listOf("type$t")) }
      typeUntilDone()
      rule.runOnIdle { assertEquals("Foutloos!", vm.result.value!!.title); vm.clearResult() }
    }
    val view = com.robpolus.commskills.domain.Grammar.view(data, app.grammar)
    assertTrue(dump() + " sessions=" + data.sessions, view.mixedUnlocked())
    assertTrue("niveau moet 70% halen na 4 perfecte sessies is niet gegarandeerd", view.percentage() > 0.05)
    assertEquals(4, data.sessions.values.count { it.perfectCount == 1 })
    // Examen: eerst voldoende voortgang opbouwen.
    app.repository.update { d ->
      var next = d
      repeat(6) { app.grammar.forEach { e -> next = com.robpolus.commskills.domain.Grammar.recordAnswer(next, app.grammar, e, true) } }
      next
    }
    assertTrue(com.robpolus.commskills.domain.Grammar.view(data, app.grammar).examAvailable())
    assertTrue(com.robpolus.commskills.domain.Grammar.examOpen(data, app.grammar, 0))
    rule.runOnIdle { vm.startGrammarExam(0) }
    rule.runOnIdle { assertEquals(15, vm.exam.value!!.total) }
    typeUntilDone()
    rule.runOnIdle { assertEquals("Examen geslaagd!", vm.result.value!!.title) }
    assertEquals(1, com.robpolus.commskills.domain.Grammar.medal(data, 0))
    assertTrue(data.examPassed.contains(Levels.GRAMMAR))
    assertTrue(Levels.GRAMMAR in data.levelReached)
  }

  @Test
  fun dataSurvivesAppRestartViaFile() {
    toHome()
    rule.runOnIdle { vm.startGrammar(listOf("type0")) }
    typeUntilDone()
    rule.waitUntil(5_000) { java.io.File(app.filesDir, "appdata.json").exists() && java.io.File(app.filesDir, "appdata.json").length() > 1000 }
    app.repository.flushNow()
    val reloaded = com.robpolus.commskills.data.AppRepository(java.io.File(app.filesDir, "appdata.json"), app.appScope)
    assertEquals(data.progress, reloaded.data.value.progress)
    assertEquals(data.lists, reloaded.data.value.lists)
    assertEquals(data.streak, reloaded.data.value.streak)
  }
}
