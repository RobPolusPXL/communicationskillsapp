package com.robpolus.commskills.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.robpolus.commskills.CommSkillsApp
import com.robpolus.commskills.domain.ActiveTimer
import com.robpolus.commskills.domain.AnswerChecker
import com.robpolus.commskills.domain.AnswerResult
import com.robpolus.commskills.domain.AppData
import com.robpolus.commskills.domain.Direction
import com.robpolus.commskills.domain.Entry
import com.robpolus.commskills.domain.Learning
import com.robpolus.commskills.domain.MatchEvent
import com.robpolus.commskills.domain.MatchSession
import com.robpolus.commskills.domain.Note
import com.robpolus.commskills.domain.SessionBuilder
import com.robpolus.commskills.domain.SessionQueue
import com.robpolus.commskills.domain.Side
import com.robpolus.commskills.domain.Stats
import com.robpolus.commskills.domain.TrophyStatus
import com.robpolus.commskills.domain.Trophies
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class Tile(val id: String, val text: String, val matched: Boolean)

data class MatchUi(
  val round: Int,
  val rounds: Int,
  val repeat: Boolean,
  val en: List<Tile>,
  val nl: List<Tile>,
  val selectedEn: String?,
  val selectedNl: String?,
  /** Tijdelijk rood gekleurd paar na een foute koppeling. */
  val flash: Pair<String, String>?,
  /** Definitie van de geselecteerde Engelse tegel (voor de (i)-knop). */
  val selectedDefinition: String?,
)

data class TypeFeedback(
  val correct: Boolean,
  val notes: Set<Note>,
  val given: String,
  val correctAnswer: String,
  val definition: String,
)

data class TypeUi(
  val serial: Int,
  val completed: Int,
  val total: Int,
  val prompt: String,
  val promptLabel: String,
  val typeHint: String?,
  val answerLabel: String,
  val feedback: TypeFeedback?,
)

data class ResultUi(
  val level: String,
  val title: String,
  val total: Int,
  val mistakes: List<Entry>,
  val perfect: Boolean,
  val newTrophies: List<TrophyStatus>,
  val levelPercentage: Double?,
  val examAvailable: Boolean,
)

enum class DirectionChoice { EN_NL, NL_EN, MIXED }

private data class TypeItem(val entry: Entry, val direction: Direction)

class PracticeViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = (application as CommSkillsApp).repository
  val data: StateFlow<AppData> = repository.data

  private val checker = AnswerChecker()
  private val timer = ActiveTimer()
  private fun today(): LocalDate = LocalDate.now()

  private var level = ""
  private var before: AppData = AppData()
  private var sessionEntries: List<Entry> = emptyList()

  private val _match = MutableStateFlow<MatchUi?>(null)
  val match: StateFlow<MatchUi?> = _match

  private val _type = MutableStateFlow<TypeUi?>(null)
  val type: StateFlow<TypeUi?> = _type

  private val _result = MutableStateFlow<ResultUi?>(null)
  val result: StateFlow<ResultUi?> = _result

  private var matchSession: MatchSession? = null
  private var flash: Pair<String, String>? = null
  private var queue: SessionQueue<TypeItem>? = null
  private val attempted = mutableSetOf<TypeItem>()
  private var serial = 0
  private val mistakeLog = linkedSetOf<Entry>()

  // --- starten -----------------------------------------------------------------------------------

  fun startMatch(level: String, groupIds: List<String>?) {
    begin(level)
    mistakeLog.clear()
    val entries = entriesFor(level, groupIds)
    sessionEntries = entries
    matchSession = MatchSession(entries)
    publishMatch()
  }

  fun startType(level: String, groupIds: List<String>?, choice: DirectionChoice) {
    begin(level)
    mistakeLog.clear()
    val entries = entriesFor(level, groupIds)
    sessionEntries = entries
    val items = entries.map { entry ->
      val dir = when (choice) {
        DirectionChoice.EN_NL -> Direction.EN_NL
        DirectionChoice.NL_EN -> Direction.NL_EN
        DirectionChoice.MIXED -> if (kotlin.random.Random.nextBoolean()) Direction.EN_NL else Direction.NL_EN
      }
      TypeItem(entry, dir)
    }
    attempted.clear()
    queue = SessionQueue(items)
    publishType(null)
  }

  private fun begin(level: String) {
    this.level = level
    mistakeLog.clear()
    before = data.value
    timer.reset()
    _result.value = null
    _match.value = null
    _type.value = null
    matchSession = null
    queue = null
    flash = null
  }

  private fun entriesFor(level: String, groupIds: List<String>?): List<Entry> =
    if (groupIds == null) SessionBuilder.mixedEntries(data.value, level)
    else SessionBuilder.vocabularyEntries(data.value, level, groupIds)

  /** Registreert activiteit (actieve oefentijd voor het dagdoel). */
  private fun touch() {
    val seconds = timer.activity(System.currentTimeMillis())
    if (seconds > 0) repository.update { Stats.addActiveSeconds(it, today(), seconds) }
  }

  fun leave() {
    timer.reset()
  }

  // --- matchen -----------------------------------------------------------------------------------

  fun selectTile(side: Side, id: String) {
    val session = matchSession ?: return
    touch()
    val event = session.select(side, id)
    when (event) {
      is MatchEvent.Mismatch -> {
        flash = event.enId to event.nlId
        viewModelScope.launch {
          delay(650)
          flash = null
          publishMatch()
        }
      }
      is MatchEvent.Matched -> {
        if (session.round?.isComplete == true) {
          publishMatch()
          viewModelScope.launch {
            delay(450)
            advanceRound()
          }
          return
        }
      }
      else -> {}
    }
    publishMatch()
  }

  private fun advanceRound() {
    val session = matchSession ?: return
    val outcome = session.advance()
    outcome.scored.filter { !it.second }.forEach { mistakeLog += it.first }
    repository.update { d ->
      var next = d
      for ((entry, ok) in outcome.scored) next = Learning.recordAnswer(next, level, entry.group, entry.word.id, ok)
      Stats.registerExercise(next, today())
    }
    if (session.finished) {
      finish(session.perfectByGroup(), session.isPerfect, sessionEntries.size, mistakeLog.toList())
    } else {
      publishMatch()
    }
  }

  private fun publishMatch() {
    val session = matchSession ?: return
    val round = session.round
    if (round == null) {
      _match.value = null
      return
    }
    val selectedEn = round.selectedEn
    _match.value = MatchUi(
      round = session.roundNumber + 1,
      rounds = session.totalRounds,
      repeat = session.roundNumber >= SessionBuilder.rounds(sessionEntries).size,
      en = round.enTiles.map { Tile(it.id, it.en, round.isMatched(Side.EN, it.id)) },
      nl = round.nlTiles.map { Tile(it.id, round.nlText(it), round.isMatched(Side.NL, it.id)) },
      selectedEn = selectedEn,
      selectedNl = round.selectedNl,
      flash = flash,
      selectedDefinition = selectedEn?.let { id -> round.words.firstOrNull { it.id == id }?.definition },
    )
  }

  // --- typen -------------------------------------------------------------------------------------

  fun submitAnswer(answer: String) {
    val q = queue ?: return
    val item = q.current() ?: return
    if (_type.value?.feedback != null) return
    touch()
    val word = item.entry.word
    val result = checker.checkWord(answer, word, item.direction)
    val correct = result.isCorrect
    if (attempted.add(item)) {
      repository.update { d ->
        Stats.registerExercise(Learning.recordAnswer(d, level, item.entry.group, word.id, correct), today())
      }
      if (!correct) mistakeLog += item.entry
    }
    q.answer(correct)
    publishType(
      TypeFeedback(
        correct = correct,
        notes = (result as? AnswerResult.Correct)?.notes.orEmpty(),
        given = answer,
        correctAnswer = if (item.direction == Direction.EN_NL) word.nl.joinToString(", ") else word.en,
        definition = word.definition,
      ),
    )
  }

  fun nextQuestion() {
    val q = queue ?: return
    if (q.isFinished) {
      val entries = sessionEntries
      val perfectByGroup = entries.map { it.group }.distinct().associateWith { g -> mistakeLog.none { it.group == g } }
      finish(perfectByGroup, q.isPerfect, entries.size, mistakeLog.toList())
    } else {
      publishType(null)
    }
  }

  private fun publishType(feedback: TypeFeedback?) {
    val q = queue ?: return
    val item = q.current()
    if (item == null) {
      // Laatste vraag beantwoord: feedback blijft zichtbaar tot "Volgende".
      val last = _type.value
      _type.value = last?.copy(feedback = feedback ?: last.feedback, completed = q.completed)
      return
    }
    val word = item.entry.word
    val en = item.direction == Direction.EN_NL
    if (feedback == null) serial++
    _type.value = TypeUi(
      serial = serial,
      completed = q.completed,
      total = q.total,
      prompt = if (en) word.en else word.nl.joinToString(", "),
      promptLabel = if (en) "Engels → Nederlands" else "Nederlands → Engels",
      typeHint = if (en) null else word.type,
      answerLabel = if (en) "Nederlandse vertaling" else "Engelse vertaling",
      feedback = feedback,
    )
  }

  // --- afronden ----------------------------------------------------------------------------------

  private fun finish(perfectByGroup: Map<String, Boolean>, perfect: Boolean, total: Int, mistakes: List<Entry>) {
    repository.update { Learning.finishSession(it, level, perfectByGroup) }
    val after = data.value
    val view = Learning.vocabularyView(after, level)
    _result.value = ResultUi(
      level = level,
      title = if (perfect) "Foutloos!" else "Sessie klaar",
      total = total,
      mistakes = mistakes.distinctBy { it.group to it.word.id },
      perfect = perfect,
      newTrophies = Trophies.newlyEarned(before, after),
      levelPercentage = view.percentage(),
      examAvailable = view.examAvailable(),
    )
    mistakeLog.clear()
    _match.value = null
    _type.value = null
    matchSession = null
    queue = null
  }

  fun clearResult() {
    _result.value = null
  }

  fun abandon() {
    mistakeLog.clear()
    _match.value = null
    _type.value = null
    matchSession = null
    queue = null
    timer.reset()
  }
}
