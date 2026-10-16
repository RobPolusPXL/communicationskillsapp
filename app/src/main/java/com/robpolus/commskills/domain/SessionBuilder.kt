package com.robpolus.commskills.domain

import kotlin.math.ceil
import kotlin.random.Random

/** Eén te oefenen woord, met de lijst waartoe het behoort. */
data class Entry(val group: String, val word: Word)

object SessionBuilder {
  /** Aantal woorden/zinnen per oefensessie; de zwakste en nog niet geoefende komen eerst. */
  const val SESSION_SIZE = 10


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
