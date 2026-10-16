package com.robpolus.commskills.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.robpolus.commskills.CommSkillsApp
import com.robpolus.commskills.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Start de echte app (Robolectric) en klikt door de belangrijkste schermen. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h2600dp-xxhdpi")
class SmokeTest {

  @get:Rule
  val rule = createAndroidComposeRule<MainActivity>()

  private val app get() = rule.activity.application as CommSkillsApp

  private fun back() = rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

  private fun waitForText(text: String) = rule.waitUntil(10_000) {
    rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
  }

  private fun toHome() {
    rule.waitUntil(15_000) { app.repository.data.value.lists.size == 7 }
    waitForText("Vocabulary")
  }

  @Test
  fun bundledListsAreImportedOnFirstStart() {
    rule.waitUntil(15_000) { app.repository.data.value.lists.size == 7 }
    val lists = app.repository.data.value.lists
    assertEquals(
      setOf("Academic writing", "Applying for a job", "Business writing", "Ethical dilemmas", "Moral foreign language effect", "Stanford class", "Trolley problem"),
      lists.map { it.title }.toSet(),
    )
    assertEquals(559, lists.sumOf { it.words.size })
    assertEquals(100, app.grammar.size)
  }

  @Test
  fun homeShowsRoutes() {
    toHome()
    rule.onNodeWithText("Vocabulary").assertIsDisplayed()
    rule.onNodeWithText("Grammar").assertIsDisplayed()
  }

  @Test
  fun vocabularyMatchSessionStarts() {
    toHome()
    rule.onNodeWithText("Vocabulary").performClick()
    waitForText("Kies één of meer lijsten")
    rule.onNodeWithText("Trolley problem").performClick()
    waitForText("Oefenen (23 woorden)")
    rule.onNodeWithText("Oefenen (23 woorden)").performClick()
    waitForText("Verbind de woorden die bij elkaar horen")
  }

  @Test
  fun lockedTypeLevelAsksForConfirmation() {
    toHome()
    rule.onNodeWithText("Vocabulary").performClick()
    waitForText("Kies één of meer lijsten")
    rule.onNodeWithText("Typen").performClick()
    waitForText("Ben je zeker?")
    rule.onNodeWithText("Ja, toch openen").performClick()
    waitForText("Niveau Typen")
  }

  @Test
  fun grammarTypingGivesFeedbackWithCorrectAnswer() {
    toHome()
    rule.onNodeWithText("Grammar").performClick()
    waitForText("Kies één of meer types")
    rule.onNodeWithText("Type 0").performClick()
    waitForText("Oefenen (10 van 25 zinnen)")
    rule.onNodeWithText("Oefenen (10 van 25 zinnen)").performClick()
    waitForText("Vul de zin aan")
    rule.onNodeWithText("Ontbrekend deel").performTextInput("zzz")
    rule.onNodeWithText("Controleren").performClick()
    waitForText("Fout")
    waitForText("Volgende")
    val d = app.repository.data.value
    assertTrue("lists=${d.lists.size} progress=${d.progress.size} streak=${d.streak} seeded=${d.seededAssets.size} t=${d.updatedAt}", d.progress.isNotEmpty())
  }

  @Test
  fun trophiesAndSettingsScreensRender() {
    toHome()
    rule.onNodeWithText("Vocabulary").assertIsDisplayed()
    rule.onNodeWithContentDescription("Trofeeën").performClick()
    waitForText("Dagdoel")
    back()
    waitForText("Vocabulary")
    rule.onNodeWithContentDescription("Instellingen").performClick()
    waitForText("Nieuwe lijst importeren (Word)")
    waitForText("Business writing")
    waitForText("Dagelijkse herinnering")
  }
}
