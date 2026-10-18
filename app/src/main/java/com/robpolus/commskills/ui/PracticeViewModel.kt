package com.robpolus.commskills.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.robpolus.commskills.CommSkillsApp
import com.robpolus.commskills.domain.ActiveTimer
import com.robpolus.commskills.domain.AnswerChecker
import com.robpolus.commskills.domain.AnswerResult
import com.robpolus.commskills.domain.AppData
import com.robpolus.commskills.domain.ChoiceBuilder
import com.robpolus.commskills.domain.ChoiceExamSession
import com.robpolus.commskills.domain.ChoiceQuestion
import com.robpolus.commskills.domain.Part
import com.robpolus.commskills.domain.Parts
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
import com.robpolus.commskills.domain.WordProgress
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
  /** Foute vocabulary-vraag in een oefensessie: de gebruiker mag "Toch juist" kiezen. */
  val canAccept: Boolean = false,
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
  /** Patroon met onthulde letters; `null` zolang er geen tip gevraagd is. */
  val tip: String? = null,
  /** Tips zijn er enkel bij oefenen, niet in een examen. */
  val canTip: Boolean = false,
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
data class ExamUi(val lives: Int, val answered: Int, val total: Int, val maxLives: Int = 3)

/** Eén meerkeuzevraag van een deel-examen. */
data class ChoiceUi(
  val serial: Int,
  val number: Int,
  val total: Int,
  val prompt: String,
  val label: String,
  val options: List<String>,
  /** Gekozen optie en juiste optie; pas ingevuld nadat er gekozen is. */
  val picked: Int?,
  val correctIndex: Int?,
  val targetMedal: Int,
)

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

  private val _choice = MutableStateFlow<ChoiceUi?>(null)
  val choice: StateFlow<ChoiceUi?> = _choice

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

  private var choiceSession: ChoiceExamSession? = null
  private var choicePart: Part? = null
  private var choiceMedal = 0

  private val examEngine: ExamEngine? get() = choiceSession?.engine ?: typing?.exam ?: matchExam

  // --- starten -----------------------------------------------------------------------------------

  fun startMatch(level: String, groupIds: List<String>?) {
    begin(level)
    matchEntries = entriesFor(level, groupIds)
    matchSession = MatchSession(matchEntries)
    publishMatch()
  }

  fun startType(level: String, groupIds: List<String>?, choice: DirectionChoice) {
    begin(level)
    val entries = SessionBuilder.vocabularySession(data.value, level, groupIds, size = SessionBuilder.TYPE_SESSION_SIZE)
    val questions = VocabQuestion.from(entries, choice, pool = allWords(), extras = data.value.extraAnswers)
    typing = TypingSession(questions)
    publishType(null)
  }

  fun startGrammar(groupIds: List<String>?) {
    begin(Levels.GRAMMAR)
    val questions = Grammar.sessionExercises(data.value, grammar, groupIds).take(SessionBuilder.SESSION_SIZE).map { GrammarQuestion(it) }
    typing = TypingSession(questions)
    publishType(null)
  }

  /** Grammar-examen over één if-type: getypt, met medaille en levens zoals bij Vocabulary. */
  fun startGrammarExam(type: Int) {
    begin(Levels.GRAMMAR)
    choicePart = Part(Grammar.groupId(type), Grammar.groupId(type), Grammar.title(type), 1, 1, emptyList())
    choiceMedal = Grammar.medal(data.value, type)
    val questions = Grammar.examExercises(grammar, type).map { GrammarQuestion(it) }
    typing = TypingSession(questions, ExamEngine(questions.size, ChoiceBuilder.lives(choiceMedal)))
    publishType(null)
    publishExam()
  }

  /** Meerkeuze-examen over één deel van een woordenlijst. */
  fun startPartExam(level: String, partId: String) {
    val part = Parts.all(data.value).firstOrNull { it.id == partId } ?: return
    begin(level)
    choicePart = part
    choiceMedal = Parts.medal(data.value, level, part)
    val pool = data.value.lists.flatMap { l -> l.words.map { Entry(l.id, it) } }
    val total = if (level == Levels.VOCAB_TYPE) minOf(part.words.size, SessionBuilder.TYPE_SESSION_SIZE) else part.words.size
    val engine = ExamEngine(total, ChoiceBuilder.lives(choiceMedal))
    if (level == Levels.VOCAB_TYPE) {
      // Typen: getypt examen over een willekeurige selectie van het deel.
      val entries = part.words.map { Entry(part.listId, it) }.shuffled().take(total)
      typing = TypingSession(VocabQuestion.from(entries, DirectionChoice.MIXED, pool = allWords(), extras = data.value.extraAnswers), engine)
      publishType(null)
      publishExam()
      return
    }
    choiceSession = ChoiceExamSession(ChoiceBuilder.build(part, pool), engine)
    publishChoice()
    publishExam()
  }

  fun pickOption(option: Int) {
    val session = choiceSession ?: return
    val q = session.question ?: return
    touch()
    val ok = session.pick(option) ?: return
    repository.update { d ->
      Stats.registerExercise(Learning.recordAnswer(d, level, q.group, q.id, ok), today())
    }
    if (!ok) noteChoiceMistake(q)
    publishExam()
    publishChoice()
  }

  fun nextChoice() {
    val session = choiceSession ?: return
    if (session.picked == null) return
    if (session.finished) {
      finishChoiceExam()
    } else {
      session.next()
      serial++
      publishChoice()
    }
  }

  private fun noteChoiceMistake(q: ChoiceQuestion) {
    mistakes.getOrPut(q.group + "/" + q.id) {
      MistakeLine(q.group + "/" + q.id, q.entry.word.en + (q.entry.word.type?.let { " ($it)" } ?: ""), q.entry.word.nl.joinToString(", "))
    }
  }

  private fun publishChoice() {
    val session = choiceSession ?: return
    val q = session.question ?: return
    val picked = session.picked
    _choice.value = ChoiceUi(
      serial = serial,
      number = session.index + 1,
      total = session.total,
      prompt = q.prompt,
      label = q.label,
      options = q.options,
      picked = picked,
      correctIndex = if (picked != null) q.correctIndex else null,
      targetMedal = minOf(3, choiceMedal + 1),
    )
  }

  private fun finishChoiceExam() {
    val session = choiceSession ?: return
    val part = choicePart ?: return
    val passed = session.engine.status == ExamStatus.PASSED
    val replay = choiceMedal >= 3
    if (passed && !replay) repository.update { Learning.awardMedal(it, level, part.id) }
    val after = data.value
    val medalName = when (minOf(3, choiceMedal + 1)) { 1 -> "brons"; 2 -> "zilver"; else -> "goud" }
    _result.value = ResultUi(
      level = level,
      title = if (passed) "Examen geslaagd!" else "Examen niet gehaald",
      total = session.total,
      mistakes = mistakes.values.toList(),
      perfect = false,
      newTrophies = Trophies.newlyEarned(before, after),
      levelPercentage = null,
      examAvailable = false,
      message = when {
        passed && replay -> "Je hebt dit deel al met goud afgerond."
        passed && level == Levels.VOCAB_MATCH && choiceMedal == 0 -> "Medaille $medalName behaald. Het niveau Typen is ontgrendeld."
        passed -> "Medaille $medalName behaald voor ${part.title}."
        session.engine.lives == 1 -> "Goud: één fout is meteen game over. Probeer opnieuw."
        else -> "Te veel fouten voor $medalName. Oefen nog even en probeer opnieuw."
      },
      isExam = true,
    )
    clearSession()
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
    _choice.value = null
    choiceSession = null
    choicePart = null
    choiceMedal = 0
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
    _exam.value = ExamUi(e.livesLeft, e.answered, e.totalQuestions, e.lives)
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
    val hinted = hintLevel > 0
    val outcome = session.answer(checker, answer, hinted) ?: return
    val q = outcome.question
    val correct = outcome.counts
    lastPrior = null
    if (outcome.firstTry) {
      if (q is VocabQuestion) lastPrior = AppData.progressKey(level, q.group, q.id) to data.value.progress[AppData.progressKey(level, q.group, q.id)]
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
        correct = outcome.result.isCorrect,
        notes = (outcome.result as? AnswerResult.Correct)?.notes.orEmpty(),
        given = answer,
        correctAnswer = q.correctText,
        explanation = q.explanation,
        canAccept = !outcome.result.isCorrect && q is VocabQuestion,
      ),
      q,
    )
  }

  private var lastPrior: Pair<String, WordProgress?>? = null

  /** Aantal gevraagde tips bij de huidige vraag. */
  private var hintLevel = 0

  /** Tip: onthult één letter meer van het antwoord (enkel bij oefenen). */
  fun requestTip() {
    val session = typing ?: return
    val ui = _type.value ?: return
    if (ui.feedback != null || session.exam != null) return
    val q = session.current() ?: return
    touch()
    hintLevel = minOf(hintLevel + 1, Tips.maxReveal(q.correctText).coerceAtLeast(1))
    _type.value = ui.copy(tip = Tips.pattern(q.correctText, hintLevel))
  }

  /** "Ik weet het niet": telt als fout, de oplossing wordt getoond. */
  fun giveUp() {
    submitAnswer("")
  }

  /** "Toch juist": het antwoord telt als juist (ook in een examen) en wordt bij dit woord onthouden. */
  fun acceptAnyway() {
    val session = typing ?: return
    val ui = _type.value ?: return
    val feedback = ui.feedback ?: return
    val q = session.lastQuestionAnswered as? VocabQuestion ?: return
    if (!feedback.canAccept || !session.forgiveLast()) return
    val prior = lastPrior
    repository.update { d ->
      var next = d
      if (prior != null) {
        next = next.copy(progress = if (prior.second == null) next.progress - prior.first else next.progress + (prior.first to prior.second!!))
        next = Learning.recordAnswer(next, level, q.group, q.id, true)
      }
      Learning.addExtraAnswer(next, q.extraKey, feedback.given)
    }
    mistakes.remove(q.group + "/" + q.id)
    lastPrior = null
    publishExam()
    _type.value = ui.copy(feedback = feedback.copy(correct = true, canAccept = false, notes = emptySet()))
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

  private fun publishType(feedback: TypeFeedback?, answered: Question? = null) {
    val session = typing ?: return
    // Na de laatste vraag blijft de feedback staan tot "Volgende".
    val q = answered ?: session.current() ?: run {
      _type.value = _type.value?.copy(feedback = feedback ?: _type.value?.feedback, completed = session.completed)
      return
    }
    if (feedback == null) { serial++; hintLevel = 0 }
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
      canTip = feedback == null && session.exam == null,
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
      examAvailable = false,
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
    val part = choicePart
    val medalName = when (minOf(3, choiceMedal + 1)) { 1 -> "brons"; 2 -> "zilver"; else -> "goud" }
    if (passed && part != null) {
      if (choiceMedal < 3) repository.update { Learning.awardMedal(it, level, part.id) }
    } else if (passed) repository.update { Learning.markExamPassed(it, level) }
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
        passed && part != null && choiceMedal >= 3 -> "Je hebt dit deel al met goud afgerond."
        passed && part != null -> "Medaille $medalName behaald voor ${part.title}."
        passed -> "Gefeliciteerd, dit niveau is afgerond."
        part != null && engine.lives == 1 -> "Goud: één fout is meteen game over. Probeer opnieuw."
        part != null -> "Te veel fouten voor $medalName. Oefen nog even en probeer opnieuw."
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
