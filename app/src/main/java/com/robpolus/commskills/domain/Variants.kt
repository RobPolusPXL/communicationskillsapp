package com.robpolus.commskills.domain

object Variants {
  private val group = Regex("\\(([^()]*)\\)")
  private val spaces = Regex("\\s+")

  /**
   * Tekst tussen haakjes is optioneel: `touch (upon)` geeft `touch upon` en `touch`,
   * `(na)streven` geeft `nastreven` en `streven`. Maximaal de eerste 3 groepen worden uitgewerkt.
   */
  fun expandOptional(s: String): List<String> {
    val matches = group.findAll(s).toList().take(MAX_GROUPS)
    if (matches.isEmpty()) return listOf(s)
    val results = LinkedHashSet<String>()
    for (mask in 0 until (1 shl matches.size)) {
      val sb = StringBuilder()
      var last = 0
      matches.forEachIndexed { i, m ->
        sb.append(s, last, m.range.first)
        if (mask and (1 shl i) != 0) sb.append(m.groupValues[1])
        last = m.range.last + 1
      }
      sb.append(s, last, s.length)
      val variant = sb.toString().replace(spaces, " ").trim()
      if (variant.isNotEmpty()) results += variant
    }
    return results.toList()
  }

  private const val MAX_GROUPS = 3
}
