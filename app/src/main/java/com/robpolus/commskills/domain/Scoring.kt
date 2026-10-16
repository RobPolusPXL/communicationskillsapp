package com.robpolus.commskills.domain

enum class WordColor { GREY, RED, ORANGE, GREEN }

enum class ListColor { GREY, RED, ORANGE, GREEN, GOLD }

object Scoring {
  /** Gewicht van het nieuwste antwoord in het voortschrijdend gemiddelde. */
  const val ALPHA = 0.3

  /** Vanaf dit niveaupercentage mag het examen gedaan worden. */
  const val EXAM_THRESHOLD = 0.7

  /** Voortschrijdend gemiddelde per woord (0..1); een nieuw woord start op 0. */
  fun update(old: Double, correct: Boolean): Double =
    (1 - ALPHA) * old + ALPHA * (if (correct) 1.0 else 0.0)

  /** Gewicht voor het trekken van woorden: zwakke woorden komen vaker aan bod (1..4). */
  fun weight(score: Double): Double = 1.0 + 3.0 * (1.0 - score.coerceIn(0.0, 1.0))

  /**
   * Niveaupercentage: gemiddelde woordscore over alle woorden van het niveau.
   * Woorden die nog nooit gespeeld zijn moeten als 0 meegegeven worden.
   */
  fun levelPercentage(scores: Collection<Double>): Double =
    if (scores.isEmpty()) 0.0 else scores.average()

  fun examAvailable(levelPercentage: Double): Boolean =
    levelPercentage >= EXAM_THRESHOLD - 1e-9

  fun wordColor(score: Double, played: Boolean): WordColor = when {
    !played -> WordColor.GREY
    score < 0.5 -> WordColor.RED
    score < 0.8 -> WordColor.ORANGE
    else -> WordColor.GREEN
  }

  /**
   * Kleur van een lijst. Goud = de laatste volledige sessie van de lijst was foutloos (100%).
   * @param averageScore gemiddelde woordscore van de lijst (nooit gespeeld = 0).
   */
  fun listColor(averageScore: Double, anyPlayed: Boolean, lastSessionPerfect: Boolean): ListColor = when {
    !anyPlayed -> ListColor.GREY
    lastSessionPerfect -> ListColor.GOLD
    averageScore < 0.5 -> ListColor.RED
    averageScore < EXAM_THRESHOLD -> ListColor.ORANGE
    else -> ListColor.GREEN
  }
}
