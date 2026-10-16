package com.robpolus.commskills.domain

import kotlin.random.Random

enum class Side { EN, NL }

sealed class MatchEvent {
  /** Een tegel is geselecteerd; wacht op de tegel aan de andere kant. */
  data object Selected : MatchEvent()

  /** Juist gekoppeld. */
  data class Matched(val enId: String, val nlId: String) : MatchEvent()

  /** Fout gekoppeld; selectie is gewist. */
  data class Mismatch(val enId: String, val nlId: String) : MatchEvent()

  /** Tegel was al gekoppeld. */
  data object Ignored : MatchEvent()
}

/**
 * Eén matchronde: Engelse en Nederlandse woorden door elkaar, aan beide kanten te beginnen.
 * Een koppeling is juist als de getoonde Nederlandse tekst een vertaling van het Engelse woord is.
 */
class MatchRound(val words: List<Word>, random: Random = Random.Default) {

  val enTiles: List<Word> = words.shuffled(random)
  val nlTiles: List<Word> = words.shuffled(random)

  fun nlText(word: Word): String = word.nl.first()

  private val matchedEn = mutableSetOf<String>()
  private val matchedNl = mutableSetOf<String>()
  private val mistaken = mutableSetOf<String>()

  var selectedEn: String? = null
    private set
  var selectedNl: String? = null
    private set

  val isComplete: Boolean get() = matchedEn.size == words.size && matchedNl.size == words.size

  /** Ids van woorden die minstens één foute koppeling hadden. */
  val mistakenIds: Set<String> get() = mistaken

  fun isMatched(side: Side, id: String) = if (side == Side.EN) id in matchedEn else id in matchedNl

  fun select(side: Side, id: String): MatchEvent {
    if (isMatched(side, id)) return MatchEvent.Ignored
    if (side == Side.EN) selectedEn = id else selectedNl = id
    val en = selectedEn ?: return MatchEvent.Selected
    val nl = selectedNl ?: return MatchEvent.Selected
    selectedEn = null
    selectedNl = null
    val enWord = words.first { it.id == en }
    val nlWord = words.first { it.id == nl }
    return if (nlText(nlWord) in enWord.nl) {
      matchedEn += en
      matchedNl += nl
      MatchEvent.Matched(en, nl)
    } else {
      mistaken += en
      mistaken += nl
      MatchEvent.Mismatch(en, nl)
    }
  }
}

/** Resultaat van een afgeronde ronde: per woord of het zonder fout ging (alleen eerste poging telt). */
data class RoundOutcome(val scored: List<Pair<Entry, Boolean>>)

/**
 * Een matchsessie over meerdere rondes. Na de rondes volgt (indien [repeatMistakes]) één herhalingsronde
 * met de woorden die fout waren; die telt niet opnieuw voor de score.
 */
class MatchSession(
  entries: List<Entry>,
  private val repeatMistakes: Boolean = true,
  private val random: Random = Random.Default,
) {
  private val allEntries = entries
  private val plannedRounds: MutableList<List<Entry>> = SessionBuilder.rounds(entries, SessionBuilder.MATCH_ROUND_SIZE).toMutableList()
  private var repeatPlanned = false
  private val firstTryMistakes = linkedSetOf<Entry>()
  private var unscored: Set<Entry> = emptySet()

  var roundNumber = 0
    private set
  var round: MatchRound? = plannedRounds.firstOrNull()?.let { MatchRound(it.map(Entry::word), random) }
    private set
  private var currentEntries: List<Entry> = plannedRounds.firstOrNull().orEmpty()

  var finished: Boolean = round == null
    private set

  val totalRounds: Int get() = plannedRounds.size

  val isPerfect: Boolean get() = finished && firstTryMistakes.isEmpty()

  /** Foutloos per groep (lijst): voor de goud-status. */
  fun perfectByGroup(): Map<String, Boolean> =
    allEntries.map { it.group }.distinct().associateWith { g -> firstTryMistakes.none { it.group == g } }

  fun select(side: Side, id: String): MatchEvent = round?.select(side, id) ?: MatchEvent.Ignored

  /**
   * Uitkomst van een onvoltooide ronde (bij een mislukt examen): enkel woorden die al gekoppeld
   * of fout gekoppeld zijn, de rest is niet aan bod gekomen.
   */
  fun partialOutcome(): List<Pair<Entry, Boolean>> {
    val current = round ?: return emptyList()
    return currentEntries.mapNotNull { entry ->
      val id = entry.word.id
      val touched = current.isMatched(Side.EN, id) || id in current.mistakenIds
      if (touched && entry !in unscored) entry to (id !in current.mistakenIds) else null
    }
  }

  /** Sluit de huidige (volledig gekoppelde) ronde af en start de volgende. */
  fun advance(): RoundOutcome {
    val current = round ?: return RoundOutcome(emptyList())
    check(current.isComplete) { "Ronde is nog niet af" }
    val scored = currentEntries
      .filter { it !in unscored }
      .map { entry -> entry to (entry.word.id !in current.mistakenIds) }
    scored.filter { !it.second }.forEach { firstTryMistakes += it.first }

    roundNumber++
    if (roundNumber < plannedRounds.size) {
      currentEntries = plannedRounds[roundNumber]
      round = MatchRound(currentEntries.map(Entry::word), random)
    } else if (repeatMistakes && !repeatPlanned && firstTryMistakes.isNotEmpty()) {
      repeatPlanned = true
      val repeat = firstTryMistakes.toMutableList()
      val padding = allEntries.filter { it !in firstTryMistakes }.shuffled(random)
      var extra = 0
      while (repeat.size < MIN_ROUND && extra < padding.size) repeat += padding[extra++]
      unscored = allEntries.toSet() // herhaling telt niet mee
      plannedRounds += SessionBuilder.rounds(repeat, SessionBuilder.MATCH_ROUND_SIZE)
      currentEntries = plannedRounds[roundNumber]
      round = MatchRound(currentEntries.map(Entry::word), random)
    } else {
      round = null
      finished = true
    }
    return RoundOutcome(scored)
  }

  companion object {
    const val MIN_ROUND = 3
  }
}
