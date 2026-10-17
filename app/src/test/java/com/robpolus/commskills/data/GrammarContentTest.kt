package com.robpolus.commskills.data

import com.robpolus.commskills.domain.AnswerChecker
import com.robpolus.commskills.domain.Grammar
import com.robpolus.commskills.domain.GrammarExercise
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Bewaakt de inhoud van `assets/grammar/conditionals.json`. */
class GrammarContentTest {

  private val exercises: List<GrammarExercise> = run {
    val file = listOf("src/main/assets/grammar/conditionals.json", "app/src/main/assets/grammar/conditionals.json")
      .map(::File).first { it.exists() }
    GrammarParser.parse(file.readText(Charsets.UTF_8))
  }

  @Test
  fun hasTwentyFivePerType() {
    for (t in Grammar.types) assertEquals("type $t", 25, exercises.count { it.type == t })
    assertEquals(100, exercises.size)
  }

  @Test
  fun idsAreUnique() {
    assertEquals(exercises.size, exercises.map { it.id }.toSet().size)
  }

  @Test
  fun everyExerciseIsWellFormed() {
    for (e in exercises) {
      assertEquals(e.id, 1, Regex(Regex.escape(GrammarExercise.BLANK)).findAll(e.text).count())
      assertTrue(e.id, e.answers.isNotEmpty())
      assertTrue(e.id, e.hint.startsWith("(") && e.hint.endsWith(")"))
      assertTrue(e.id, e.explain.isNotBlank())
    }
  }

  @Test
  fun everyListedAnswerIsAccepted() {
    val checker = AnswerChecker()
    for (e in exercises) for (a in e.answers) {
      assertTrue("${e.id}: $a", checker.check(a, e.answers).isCorrect)
    }
  }

  @Test
  fun wrongFormsAreRejected() {
    val checker = AnswerChecker()
    for (e in exercises) assertFalse(e.id, checker.check("zzz", e.answers).isCorrect)
  }

  @Test
  fun filledSentencesFollowTheirType() {
    val modal = Regex("\\b(would|wouldn't|won't|will|'ll|'d)\\b|'ll|'d", RegexOption.IGNORE_CASE)
    for (e in exercises) {
      val full = e.filled()
      when (e.type) {
        3 -> assertTrue(e.id + ": " + full, Regex("\\b(had|hadn't|would have|wouldn't have|'d have)\\b|'d ", RegexOption.IGNORE_CASE).containsMatchIn(full))
        2 -> assertTrue(e.id + ": " + full, Regex("\\b(would|wouldn't|could)\\b|'d ", RegexOption.IGNORE_CASE).containsMatchIn(full))
        1 -> assertTrue(e.id + ": " + full, Regex("\\b(will|won't)\\b|'ll", RegexOption.IGNORE_CASE).containsMatchIn(full))
        0 -> assertFalse(e.id + ": " + full, modal.containsMatchIn(full))
      }
    }
  }
}
