package com.robpolus.commskills.domain

/** Tips bij typen: een patroon met de eerste letters van het antwoord, de rest als streepjes. */
object Tips {
  /** Het aantal letters (spaties tellen niet mee) dat een tip hoogstens mag onthullen: nooit het hele antwoord. */
  fun maxReveal(answer: String): Int = (answer.count { it.isLetterOrDigit() } - 1).coerceAtLeast(0)

  /** "afname", 2 → "a f _ _ _ _"; woorden gescheiden door een brede spatie. */
  fun pattern(answer: String, revealed: Int): String {
    val show = revealed.coerceIn(0, maxReveal(answer))
    var seen = 0
    return answer.trim().split(' ').filter { it.isNotEmpty() }.joinToString("   ") { word ->
      word.map { c ->
        if (!c.isLetterOrDigit()) c.toString()
        else if (seen++ < show) c.toString() else "_"
      }.joinToString(" ")
    }
  }
}
