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

  private fun dump() = "lists=${data.lists.size} progress=${data.progress.size} sessions=${data.sessions.size} exam=${data.examPassed} streak=${data.streak} t=${data.updatedAt}"

  private fun idle() = ShadowLooper.idleMainLooper(3, TimeUnit.SECONDS)

  private fun toHome() {
    rule.waitUntil(15_000) { data.lists.size == 7 }
    rule.onNodeWithText("Doorgaan").performClick()
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

  @Test
  fun matchExamPassUnlocksTypingLevel() {
    toHome()
    makeExamEligible(Levels.VOCAB_MATCH)
    assertFalse(Learning.levelUnlocked(data, Levels.VOCAB_TYPE))
    rule.runOnIdle { vm.startExam(Levels.VOCAB_MATCH) }
    rule.runOnIdle { assertEquals(3, vm.exam.value!!.lives) }
    solveMatch()
    rule.runOnIdle { assertEquals("Examen geslaagd!", vm.result.value!!.title) }
    assertTrue(dump(), Learning.levelUnlocked(data, Levels.VOCAB_TYPE))
    assertTrue(data.examPassed.contains(Levels.VOCAB_MATCH))
  }

  @Test
  fun matchExamFailsAfterThreeMistakes() {
    toHome()
    makeExamEligible(Levels.VOCAB_MATCH)
    rule.runOnIdle { vm.startExam(Levels.VOCAB_MATCH) }
    solveMatch(wrong = 3)
    rule.runOnIdle { assertEquals("Examen niet gehaald", vm.result.value!!.title) }
    assertFalse(Learning.levelUnlocked(data, Levels.VOCAB_TYPE))
  }

  @Test
  fun typingSessionAndExam() {
    toHome()
    makeExamEligible(Levels.VOCAB_TYPE)
    rule.runOnIdle { vm.startType(Levels.VOCAB_TYPE, listOf("trolley_problem"), com.robpolus.commskills.domain.DirectionChoice.MIXED) }
    typeUntilDone()
    rule.runOnIdle { assertEquals("Foutloos!", vm.result.value!!.title) }
    rule.runOnIdle { vm.clearResult(); vm.startExam(Levels.VOCAB_TYPE) }
    rule.runOnIdle { assertEquals(12, vm.exam.value!!.total) }
    typeUntilDone()
    rule.runOnIdle { assertEquals("Examen geslaagd!", vm.result.value!!.title) }
    assertTrue(data.examPassed.contains(Levels.VOCAB_TYPE))
  }

  @Test
  fun typingExamFailsWithWrongAnswers() {
    toHome()
    makeExamEligible(Levels.VOCAB_TYPE)
    rule.runOnIdle { vm.startExam(Levels.VOCAB_TYPE) }
    typeUntilDone(correct = false)
    rule.runOnIdle { assertEquals("Examen niet gehaald", vm.result.value!!.title) }
    assertFalse(data.examPassed.contains(Levels.VOCAB_TYPE))
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
    assertTrue("niveau moet 70% halen na 4 perfecte sessies is niet gegarandeerd", view.percentage() > 0.25)
    assertEquals(4, data.sessions.values.count { it.perfectCount == 1 })
    // Examen: eerst voldoende voortgang opbouwen.
    app.repository.update { d ->
      var next = d
      repeat(6) { app.grammar.forEach { e -> next = com.robpolus.commskills.domain.Grammar.recordAnswer(next, app.grammar, e, true) } }
      next
    }
    assertTrue(com.robpolus.commskills.domain.Grammar.view(data, app.grammar).examAvailable())
    rule.runOnIdle { vm.startExam(Levels.GRAMMAR) }
    rule.runOnIdle { assertEquals(21, vm.exam.value!!.total) }
    typeUntilDone()
    rule.runOnIdle { assertEquals("Examen geslaagd!", vm.result.value!!.title) }
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
