package com.robpolus.commskills.domain

import kotlin.math.pow
import kotlin.random.Random

/**
 * Wachtrij van een oefensessie. Een fout beantwoord item komt 3 à 5 posities verder in dezelfde
 * sessie opnieuw, tot het één keer juist is. Alleen het eerste antwoord telt als fout voor de score.
 */
class SessionQueue<T>(items: List<T>, private val random: Random = Random.Default) {

  private val queue = ArrayDeque(items)
  private val seen = mutableSetOf<T>()
  private val wrongFirstTry = LinkedHashSet<T>()

  val total: Int = items.size

  /** Aantal items dat afgewerkt (juist beantwoord) is. */
  var completed: Int = 0
    private set

  val isFinished: Boolean get() = queue.isEmpty()

  /** Aantal items dat bij de eerste poging fout was. */
  val mistakes: Int get() = wrongFirstTry.size

  /** Foutloze sessie (100%): afgewerkt en geen enkel item fout bij de eerste poging. */
  val isPerfect: Boolean get() = isFinished && wrongFirstTry.isEmpty()

  fun current(): T? = queue.firstOrNull()

  private var lastWrong: T? = null
  private var lastWrongWasFirst = false

  /** Het laatste foute antwoord toch juist rekenen: de herhaling vervalt en het item is afgewerkt. */
  fun forgiveLast(): Boolean {
    val item = lastWrong ?: return false
    if (!queue.remove(item)) return false
    completed++
    if (lastWrongWasFirst) wrongFirstTry.remove(item)
    lastWrong = null
    return true
  }

  /** Registreert het antwoord op het huidige item. */
  fun answer(correct: Boolean) {
    val item = queue.removeFirstOrNull() ?: return
    val firstTry = seen.add(item)
    if (correct) {
      completed++
    } else {
      if (firstTry) wrongFirstTry.add(item)
      lastWrong = item
      lastWrongWasFirst = firstTry
      val position = random.nextInt(3, 6).coerceAtMost(queue.size)
      queue.add(position, item)
    }
  }
}

object Sessions {
  /**
   * Willekeurige volgorde waarin items met een hoger gewicht gemiddeld eerder komen
   * (gewogen trekking zonder teruglegging, Efraimidis–Spirakis).
   */
  fun <T> weightedOrder(items: List<T>, weightOf: (T) -> Double, random: Random = Random.Default): List<T> =
    items
      .map { it to random.nextDouble().coerceAtLeast(1e-12).pow(1.0 / weightOf(it).coerceAtLeast(1e-6)) }
      .sortedByDescending { it.second }
      .map { it.first }

  /** Examenvragen: gewogen trekking (zwakkere woorden vaker), zonder dubbels. */
  fun <T> examQuestions(items: List<T>, count: Int, weightOf: (T) -> Double, random: Random = Random.Default): List<T> =
    weightedOrder(items, weightOf, random).take(count)
}
