package com.robpolus.commskills.domain

import java.text.Normalizer
import kotlin.math.abs

/** Meldingen bij een juist antwoord. */
enum class Note {
  /** Eén letter vervangen door een naburige toets (AZERTY). */
  TYPO,

  /** Accent(en) ontbreken. */
  MISSING_ACCENT,

  /** Bij een werkwoord ontbreekt "to": voor 100% correct moet het "to …" zijn. */
  MISSING_TO,
}

sealed class AnswerResult {
  /** Juist, eventueel met meldingen. */
  data class Correct(val notes: Set<Note> = emptySet()) : AnswerResult()

  /** Fout; [correctAnswer] is de oplossing die getoond wordt. */
  data class Wrong(val correctAnswer: String) : AnswerResult()

  val isCorrect: Boolean get() = this is Correct
}

/**
 * Controleert getypte antwoorden volgens de regels uit ANALYSE.md / ONTWERP.md § 4:
 * - één juiste vertaling volstaat; tekst tussen haakjes in een woord is optioneel (zie [Variants]);
 * - hoofdletters tellen enkel waar de referentie zelf een hoofdletter heeft;
 * - accenten zijn niet vereist (enkel een melding);
 * - bij NL→EN zijn "to", "a" en "an" optioneel (zonder "to": melding);
 * - één vervangen letter naar een naburige AZERTY-toets is een typo (juist, met melding);
 *   omgewisselde, ontbrekende of extra letters zijn fout.
 */
class AnswerChecker {

  fun checkWord(given: String, word: Word, direction: Direction): AnswerResult =
    when (direction) {
      Direction.EN_NL ->
        check(
          given,
          word.nl.flatMap { Variants.expandOptional(it) },
          lenientPrefixes = false,
          display = word.nl.joinToString(", "),
        )
      Direction.NL_EN ->
        check(given, Variants.expandOptional(word.en), lenientPrefixes = true, display = word.en)
    }

  /**
   * @param accepted alle aanvaarde antwoorden; één match volstaat.
   * @param lenientPrefixes `true` voor Engelse woorden (optionele "to"/"a"/"an"), `false` voor zinnen en Nederlands.
   * @param display wat getoond wordt als het antwoord fout is.
   */
  fun check(
    given: String,
    accepted: List<String>,
    lenientPrefixes: Boolean = false,
    display: String = accepted.joinToString(", "),
  ): AnswerResult {
    val best = accepted
      .mapNotNull { matchOne(given, it, lenientPrefixes) }
      .minByOrNull { it.size }
    return if (best != null) AnswerResult.Correct(best) else AnswerResult.Wrong(display)
  }

  // ---------------------------------------------------------------------------------------------

  private fun matchOne(givenRaw: String, referenceRaw: String, lenient: Boolean): Set<Note>? {
    var ref = clean(referenceRaw)
    var given = clean(givenRaw)
    if (ref.isEmpty() || given.isEmpty()) return null

    val notes = mutableSetOf<Note>()
    if (lenient) {
      val refHasTo = ref.startsWith("to ", ignoreCase = true)
      val givenHasTo = given.startsWith("to ", ignoreCase = true)
      ref = stripLeading(ref)
      given = stripLeading(given)
      if (refHasTo && !givenHasTo) notes += Note.MISSING_TO
    }

    val refTokens = ref.split(' ')
    val givenTokens = given.split(' ')
    if (refTokens.size != givenTokens.size) return null

    // 1. Exacte vergelijking (met accent- en hoofdlettertolerantie).
    var accentMissing = false
    var exact = true
    for (i in refTokens.indices) {
      when (compareToken(givenTokens[i], refTokens[i])) {
        TokenMatch.NONE -> { exact = false; break }
        TokenMatch.ACCENT -> accentMissing = true
        TokenMatch.EXACT -> {}
      }
    }
    if (exact) {
      if (accentMissing) notes += Note.MISSING_ACCENT
      return notes
    }

    // 2. Typo: precies één vervangen letter naar een naburige toets.
    if (isNeighbourTypo(given, ref)) {
      notes += Note.TYPO
      return notes
    }
    return null
  }

  private enum class TokenMatch { EXACT, ACCENT, NONE }

  private fun compareToken(given: String, ref: String): TokenMatch {
    val requireCase = ref.any { it.isUpperCase() }
    val g = stripAccents(given)
    val r = stripAccents(ref)
    val same = if (requireCase) g == r else g.equals(r, ignoreCase = true)
    if (!same) return TokenMatch.NONE
    val refHasAccents = r != ref
    val identical = if (requireCase) given == ref else given.equals(ref, ignoreCase = true)
    return if (refHasAccents && !identical) TokenMatch.ACCENT else TokenMatch.EXACT
  }

  private fun isNeighbourTypo(given: String, ref: String): Boolean {
    val g = stripAccents(given)
    val r = stripAccents(ref)
    if (g.length != r.length) return false
    var diffAt = -1
    for (i in r.indices) {
      if (g[i].lowercaseChar() != r[i].lowercaseChar()) {
        if (diffAt != -1) return false // meer dan één verschil
        diffAt = i
      }
    }
    if (diffAt == -1) return false // geen verschil (hoofdlettergeval), geen typo
    if (!adjacent(g[diffAt].lowercaseChar(), r[diffAt].lowercaseChar())) return false
    // Verplichte hoofdletters moeten kloppen.
    for (i in r.indices) {
      if (r[i].isUpperCase() && !g[i].isUpperCase()) return false
    }
    return true
  }

  // ---------------------------------------------------------------------------------------------

  private fun clean(s: String): String =
    s.replace('-', ' ')
      .filter { it.isLetterOrDigit() || it.isWhitespace() }
      .trim()
      .replace(Regex("\\s+"), " ")

  private fun stripLeading(s: String): String {
    val stripped = s.replace(Regex("^(to|an|a) ", RegexOption.IGNORE_CASE), "")
    return if (stripped.isEmpty()) s else stripped
  }

  private fun stripAccents(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

  // --- AZERTY-toetsenbord -----------------------------------------------------------------------

  private val keyboardRows = listOf("azertyuiop" to 0.0, "qsdfghjklm" to 0.25, "wxcvbn" to 0.75)

  private val coords: Map<Char, Pair<Double, Int>> by lazy {
    buildMap {
      keyboardRows.forEachIndexed { row, (keys, offset) ->
        keys.forEachIndexed { idx, ch -> put(ch, (idx + offset) to row) }
      }
    }
  }

  private fun adjacent(a: Char, b: Char): Boolean {
    val (ax, ay) = coords[a] ?: return false
    val (bx, by) = coords[b] ?: return false
    val dx = abs(ax - bx)
    val dy = abs(ay - by)
    return (dy == 0 && dx <= 1.0) || (dy == 1 && dx < 1.0)
  }
}
