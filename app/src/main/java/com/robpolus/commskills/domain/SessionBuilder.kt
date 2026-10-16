package com.robpolus.commskills.domain

import kotlin.math.ceil
import kotlin.random.Random

/** Eén te oefenen woord, met de lijst waartoe het behoort. */
data class Entry(val group: String, val word: Word)

object SessionBuilder {
  /** Aantal woorden/zinnen per oefensessie; de zwakste en nog niet geoefende komen eerst. */
  const val SESSION_SIZE = 10

  /** Aantal woorden per vocabulary-sessie: de helft zwakste, de helft willekeurig. */
  const val VOCAB_SESSION_SIZE = 40

  /**
   * Vocabulary-sessie: heeft de keuze meer dan [size] woorden, dan is de helft de zwakste
   * (laagste score, ongespeeld telt als 0) en de helft willekeurig uit de rest. Daarna gemengd.
   */
  fun vocabularySession(
    data: AppData,
    level: String,
    groupIds: List<String>?,
    size: Int = VOCAB_SESSION_SIZE,
    random: Random = Random.Default,
  ): List<Entry> {
    val view = Learning.vocabularyView(data, level)
    val ids = groupIds ?: data.lists.map { it.id }
    val entries = data.lists.filter { it.id in ids }.flatMap { list -> list.words.map { Entry(list.id, it) } }
    if (entries.size <= size) return entries.shuffled(random)
    val weakCount = size / 2
    val weakest = entries.shuffled(random).sortedBy { view.score(it.group, it.word.id) }.take(weakCount)
    val rest = (entries - weakest.toSet()).shuffled(random).take(size - weakCount)
    return (weakest + rest).shuffled(random)
  }


  /**
   * Sessie over de gekozen groepen: alle woorden, gewogen volgorde
   * (zwakke woorden vroeger), zodat foute woorden vaker en eerder terugkomen.
   */
  fun vocabularyEntries(
    data: AppData,
    level: String,
    groupIds: List<String>,
    random: Random = Random.Default,
  ): List<Entry> {
    val view = Learning.vocabularyView(data, level)
    val entries = data.lists.filter { it.id in groupIds }.flatMap { list -> list.words.map { Entry(list.id, it) } }
    return Sessions.weightedOrder(entries, { view.weight(it.group, it.word.id) }, random)
  }

  /** Alle woorden van alle lijsten, voor "Gemixt". */
  fun mixedEntries(data: AppData, level: String, random: Random = Random.Default): List<Entry> =
    vocabularyEntries(data, level, data.lists.map { it.id }, random)

  /** Examenvragen: gewogen trekking uit alle woorden van het niveau, zonder dubbels. */
  fun examEntries(data: AppData, level: String, count: Int, random: Random = Random.Default): List<Entry> {
    val view = Learning.vocabularyView(data, level)
    val all = data.lists.flatMap { list -> list.words.map { Entry(list.id, it) } }
    return Sessions.examQuestions(all, count, { view.weight(it.group, it.word.id) }, random)
  }

  /** Verdeelt items gelijkmatig in rondes van hoogstens [maxSize] (17 → 6, 6, 5). */
  fun <T> rounds(items: List<T>, maxSize: Int = 6): List<List<T>> {
    if (items.isEmpty()) return emptyList()
    val count = ceil(items.size / maxSize.toDouble()).toInt()
    val base = items.size / count
    val extra = items.size % count
    val result = mutableListOf<List<T>>()
    var index = 0
    for (i in 0 until count) {
      val size = base + if (i < extra) 1 else 0
      result += items.subList(index, index + size)
      index += size
    }
    return result
  }
}
