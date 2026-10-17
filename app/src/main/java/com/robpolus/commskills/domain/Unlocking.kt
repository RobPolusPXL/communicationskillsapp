package com.robpolus.commskills.domain

object Unlocking {
  /** De gemixte oefening gaat open nadat alle lijsten (of if-types) minstens één keer gespeeld zijn. */
  fun mixedUnlocked(played: Collection<Boolean>): Boolean =
    played.isNotEmpty() && played.all { it }

  /** Het volgende niveau ontgrendelt enkel door het examen van het vorige te halen. */
  fun nextLevelUnlocked(previousExamPassed: Boolean): Boolean = previousExamPassed
}
