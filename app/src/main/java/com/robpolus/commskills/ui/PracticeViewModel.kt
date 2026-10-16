package com.robpolus.commskills.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.robpolus.commskills.CommSkillsApp
import com.robpolus.commskills.domain.ActiveTimer
import com.robpolus.commskills.domain.AnswerChecker
import com.robpolus.commskills.domain.AnswerResult
import com.robpolus.commskills.domain.AppData
import com.robpolus.commskills.domain.DirectionChoice
import com.robpolus.commskills.domain.Entry
import com.robpolus.commskills.domain.ExamEngine
import com.robpolus.commskills.domain.ExamStatus
import com.robpolus.commskills.domain.Grammar
import com.robpolus.commskills.domain.GrammarQuestion
import com.robpolus.commskills.domain.Learning
import com.robpolus.commskills.domain.Levels
import com.robpolus.commskills.domain.MatchEvent
import com.robpolus.commskills.domain.MatchSession
import com.robpolus.commskills.domain.Note
import com.robpolus.commskills.domain.Question
import com.robpolus.commskills.domain.SessionBuilder
import com.robpolus.commskills.domain.Side
import com.robpolus.commskills.domain.Stats
import com.robpolus.commskills.domain.Trophies
import com.robpolus.commskills.domain.TrophyStatus
import com.robpolus.commskills.domain.TypingSession
import com.robpolus.commskills.domain.VocabQuestion
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
  /** Uitleg van de geselecteerde Engelse tegel (voor de (i)-knop). */
  val selectedDefinition: String?,
  val combo: Int = 0,
)

data class TypeFeedback(
  val correct: Boolean,
  val notes: Set<Note>,
  val given: String,
  val correctAnswer: String,
  val explanation: String,
)

data class TypeUi(
  val serial: Int,
  val completed: Int,
  val total: Int,
  val prompt: String,
  val promptLabel: String,
  val hint: String?,
  val answerLabel: String,
  val feedback: TypeFeedback?,
  val grammar: Boolean,
)

data class MistakeLine(val key: String, val title: String, val detail: String)

data class ResultUi(
  val level: String,
  val title: String,
  val total: Int,
  val mistakes: List<MistakeLine>,
  val perfect: Boolean,
  val newTrophies: List<TrophyStatus>,
  val levelPercentage: Double?,
  val examAvailable: Boolean,
  val message: String? = null,
  val isExam: Boolean = false,
)

/** Toestand van een lopend examen (voor de levens bovenaan). */
data class ExamUi(val lives: Int, val answered: Int, val total: Int)

class PracticeViewModel(application: Application) : AndroidViewModel(application) {

  private val app = application as CommSkillsApp
  private val repository = app.repository
  private val grammar = app.grammar
  val data: StateFlow<AppData> = repository.data

  private val checker = AnswerChecker()
  private val timer = ActiveTimer()
  private fun today(): LocalDate = LocalDate.now()

  private var level = ""
  private var before: AppData = AppData()

  private val _match = MutableStateFlow<MatchUi?>(null)
  val match: StateFlow<MatchUi?> = _match

  private val _type = MutableStateFlow<TypeUi?>(null)
  val type: StateFlow<TypeUi?> = _type

  private val _exam = MutableStateFlow<ExamUi?>(null)
  val exam: StateFlow<ExamUi?> = _exam

  private val _result = MutableStateFlow<ResultUi?>(null)
  val result: StateFlow<ResultUi?> = _result

  private var matchSession: MatchSession? = null
  private var matchExam: ExamEngine? = null
  private var matchEntries: List<Entry> = emptyList()
  private var flash: Pair<String, String>? = null
  private val mistakes = linkedMapOf<String, MistakeLine>()
  private val mistakeGroups = mutableSetOf<String>()

  private var combo = 0
  private var typing: TypingSession<Question>? = null
  private var serial = 0

  private val examEngine: ExamEngine? get() = typing?.exam ?: matchExam

  // --- starten -----------------------------------------------------------------------------------

  fun startMatch(level: String, groupIds: List<String>?) {
    begin(level)
    matchEntries = entriesFor(level, groupIds)
    matchSession = MatchSession(matchEntries)
    publishMatch()
  }

  fun startType(level: String, groupIds: List<String>?, choice: DirectionChoice) {
    begin(level)
    val questions = VocabQuestion.from(entriesFor(level, groupIds), choice, pool = allWords())
    typing = TypingSession(questions)
    publishType(null)
  }

  fun startGrammar(groupIds: List<String>?) {
    begin(Levels.GRAMMAR)
    val questions = Grammar.sessionExercises(data.value, grammar, groupIds).take(SessionBuilder.SESSION_SIZE).map { GrammarQuestion(it) }
    typing = TypingSession(questions)
    publishType(null)
  }

  fun startExam(level: String) {
    begin(level)
    when (level) {
      Levels.VOCAB_MATCH -> {
        val engine = ExamEngine.vocabulary()
        matchExam = engine
        matchEntries = SessionBuilder.examEntries(data.value, level, ExamEngine.VOCABULARY_QUESTIONS)
        matchSession = MatchSession(matchEntries, repeatMistakes = false)
        publishMatch()
      }
      Levels.VOCAB_TYPE -> {
        val entries = SessionBuilder.examEntries(data.value, level, ExamEngine.VOCABULARY_QUESTIONS)
        typing = TypingSession(VocabQuestion.from(entries, DirectionChoice.MIXED, pool = allWords()), ExamEngine.vocabulary())
        publishType(null)
      }
      else -> {
        val questions = Grammar.examExercises(data.value, grammar).map { GrammarQuestion(it) }
        typing = TypingSession(questions, ExamEngine.grammar())
        publishType(null)
      }
    }
    publishExam()
  }

  private fun begin(level: String) {
    this.level = level
    before = data.value
    timer.reset()
    clearSession()
    _result.value = null
  }

  private fun clearSession() {
    _match.value = null
    _type.value = null
    _exam.value = null
    matchSession = null
    matchExam = null
    matchEntries = emptyList()
    typing = null
    flash = null
    combo = 0
    serial = 0
    mistakes.clear()
    mistakeGroups.clear()
  }

  private fun entriesFor(level: String, groupIds: List<String>?): List<Entry> =
    SessionBuilder.vocabularySession(data.value, level, groupIds)

  private fun allWords() = data.value.lists.flatMap { it.words }

  private fun publishExam() {
    val e = examEngine ?: return
    _exam.value = ExamUi(e.livesLeft, e.answered, e.totalQuestions)
  }

  /** Registreert activiteit (actieve oefentijd voor het dagdoel). */
  private fun touch() {
    val seconds = timer.activity(System.currentTimeMillis())
    if (seconds > 0) repository.update { Stats.addActiveSeconds(it, today(), seconds) }
  }

  // --- matchen -----------------------------------------------------------------------------------

  fun selectTile(side: Side, id: String) {
    val session = matchSession ?: return
    touch()
    val engine = matchExam
    when (val event = session.select(side, id)) {
      is MatchEvent.Mismatch -> {
        combo = 0
        engine?.loseLife()
        publishExam()
        flash = event.enId to event.nlId
        viewModelScope.launch {
          delay(650)
          flash = null
          if (engine?.status == ExamStatus.FAILED) finishExam() else publishMatch()
        }
      }
      is MatchEvent.Matched -> {
        combo++
        engine?.record(true)
        publishExam()
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
    for ((entry, ok) in outcome.scored) if (!ok) noteMistake(entry)
    repository.update { d ->
      var next = d
      for ((entry, ok) in outcome.scored) next = Learning.recordAnswer(next, level, entry.group, entry.word.id, ok)
      Stats.registerExercise(next, today())
    }
    when {
      session.finished && matchExam != null -> finishExam()
      session.finished -> finishPractice(session.perfectByGroup(), session.isPerfect, matchEntries.size)
      else -> publishMatch()
    }
  }

  private fun noteMistake(entry: Entry) {
    mistakeGroups += entry.group
    mistakes.getOrPut(entry.group + "/" + entry.word.id) {
      MistakeLine(entry.group + "/" + entry.word.id, entry.word.en + (entry.word.type?.let { " ($it)" } ?: ""), entry.word.nl.joinToString(", "))
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
      repeat = session.roundNumber >= SessionBuilder.rounds(matchEntries, SessionBuilder.MATCH_ROUND_SIZE).size,
      en = round.enTiles.map { Tile(it.id, it.en, round.isMatched(Side.EN, it.id)) },
      nl = round.nlTiles.map { Tile(it.id, round.nlText(it), round.isMatched(Side.NL, it.id)) },
      selectedEn = selectedEn,
      selectedNl = round.selectedNl,
      flash = flash,
      selectedDefinition = selectedEn?.let { id -> round.words.firstOrNull { it.id == id }?.definition },
      combo = combo,
    )
  }

  // --- typen en grammatica -----------------------------------------------------------------------

  fun submitAnswer(answer: String) {
    val session = typing ?: return
    if (_type.value?.feedback != null) return
    touch()
    val outcome = session.answer(checker, answer) ?: return
    val q = outcome.question
    val correct = outcome.result.isCorrect
    if (outcome.firstTry) {
      repository.update { d ->
        val scored = if (q is GrammarQuestion) {
          Grammar.recordAnswer(d, grammar, q.exercise, correct)
        } else {
          Learning.recordAnswer(d, level, q.group, q.id, correct)
        }
        Stats.registerExercise(scored, today())
      }
      if (!correct) noteMistake(q)
    }
    publishExam()
    publishType(
      TypeFeedback(
        correct = correct,
        notes = (outcome.result as? AnswerResult.Correct)?.notes.orEmpty(),
        given = answer,
        correctAnswer = q.correctText,
        explanation = q.explanation,
      ),
    )
  }

  private fun noteMistake(q: Question) {
    mistakeGroups += q.group
    mistakes.getOrPut(q.group + "/" + q.id) {
      when (q) {
        is VocabQuestion -> MistakeLine(
          q.group + "/" + q.id,
          q.entry.word.en + (q.entry.word.type?.let { " ($it)" } ?: ""),
          q.entry.word.nl.joinToString(", "),
        )
        is GrammarQuestion -> MistakeLine(q.group + "/" + q.id, q.exercise.filled(), q.exercise.explain)
      }
    }
  }

  fun nextQuestion() {
    val session = typing ?: return
    if (session.finished) {
      if (session.exam != null) finishExam() else finishPractice(session.perfectByGroup(), session.isPerfect, session.total)
    } else {
      publishType(null)
    }
  }

  private fun publishType(feedback: TypeFeedback?) {
    val session = typing ?: return
    // Na de laatste vraag blijft de feedback staan tot "Volgende".
    val q = session.current() ?: run {
      _type.value = _type.value?.copy(feedback = feedback ?: _type.value?.feedback, completed = session.completed)
      return
    }
    if (feedback == null) serial++
    _type.value = TypeUi(
      serial = serial,
      completed = session.completed,
      total = session.total,
      prompt = q.prompt,
      promptLabel = q.label,
      hint = q.hint,
      answerLabel = q.answerLabel,
      feedback = feedback,
      grammar = q is GrammarQuestion,
    )
  }

  // --- afronden ----------------------------------------------------------------------------------

  private fun finishPractice(perfectByGroup: Map<String, Boolean>, perfect: Boolean, total: Int) {
    repository.update { Learning.finishSession(it, level, perfectByGroup) }
    val after = data.value
    val view = if (level == Levels.GRAMMAR) Grammar.view(after, grammar) else Learning.vocabularyView(after, level)
    if (level == Levels.GRAMMAR) repository.update { Learning.markReachedIfAvailable(it, Grammar.view(it, grammar)) }
    val finalData = data.value
    _result.value = ResultUi(
      level = level,
      title = if (perfect) "Foutloos!" else "Sessie klaar",
      total = total,
      mistakes = mistakes.values.toList(),
      perfect = perfect,
      newTrophies = Trophies.newlyEarned(before, finalData),
      levelPercentage = view.percentage(),
      examAvailable = view.examAvailable(),
    )
    clearSession()
  }

  private fun finishExam() {
    val engine = examEngine ?: return
    val passed = engine.status == ExamStatus.PASSED
    matchSession?.let { session ->
      val partial = session.partialOutcome()
      partial.filter { !it.second }.forEach { noteMistake(it.first) }
      if (partial.isNotEmpty()) {
        repository.update { d ->
          partial.fold(d) { acc, (entry, ok) -> Learning.recordAnswer(acc, level, entry.group, entry.word.id, ok) }
        }
      }
    }
    if (passed) repository.update { Learning.markExamPassed(it, level) }
    val after = data.value
    _result.value = ResultUi(
      level = level,
      title = if (passed) "Examen geslaagd!" else "Examen niet gehaald",
      total = engine.totalQuestions,
      mistakes = mistakes.values.toList(),
      perfect = false,
      newTrophies = Trophies.newlyEarned(before, after),
      levelPercentage = null,
      examAvailable = false,
      message = when {
        passed && level == Levels.VOCAB_MATCH -> "Het niveau Typen is ontgrendeld."
        passed -> "Gefeliciteerd, dit niveau is afgerond."
        else -> "Geen leven meer over. Oefen nog even en probeer opnieuw."
      },
      isExam = true,
    )
    clearSession()
  }

  fun clearResult() {
    _result.value = null
  }

  fun abandon() {
    clearSession()
    timer.reset()
  }
}
