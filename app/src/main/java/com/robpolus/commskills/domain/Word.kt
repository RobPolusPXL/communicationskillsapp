package com.robpolus.commskills.domain

/**
 * Eén woord uit een woordenlijst.
 *
 * @param en Engels woord zoals in de lijst, mét lidwoord of "to" (bv. "to pursue", "a trolley"), zonder type.
 * @param type Woordtype zoals "v", "n", "adj" (optioneel).
 * @param definition Engelse uitleg (Explanation/Definition).
 * @param nl Nederlandse vertalingen; één juiste variant volstaat.
 */
data class Word(
  val id: String,
  val en: String,
  val type: String?,
  val definition: String,
  val nl: List<String>,
)

enum class Direction { EN_NL, NL_EN }
