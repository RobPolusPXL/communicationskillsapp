package com.robpolus.commskills.ui.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import android.content.Context
import android.content.ContextWrapper
import kotlinx.coroutines.launch
import com.robpolus.commskills.ui.screens.AccountSettings
import androidx.compose.ui.platform.LocalContext
import com.robpolus.commskills.CommSkillsApp
import com.robpolus.commskills.ui.screens.GrammarOverviewScreen
import com.robpolus.commskills.ui.screens.GrammarScreen
import com.robpolus.commskills.domain.Levels
import com.robpolus.commskills.ui.PracticeViewModel
import com.robpolus.commskills.ui.screens.ChoiceScreen
import com.robpolus.commskills.ui.screens.ExamHearts
import com.robpolus.commskills.ui.screens.MatchScreen
import com.robpolus.commskills.ui.screens.ResultScreen
import com.robpolus.commskills.ui.screens.TypeScreen
import com.robpolus.commskills.ui.screens.VocabularyScreen
import com.robpolus.commskills.ui.screens.WordOverviewScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.robpolus.commskills.ui.AppViewModel
import com.robpolus.commskills.ui.screens.ReminderSettings
import com.robpolus.commskills.ui.screens.SettingsScreen
import com.robpolus.commskills.ui.screens.TrophiesScreen
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.robpolus.commskills.ui.screens.HomeScreen
import com.robpolus.commskills.ui.screens.LoginScreen
import com.robpolus.commskills.ui.screens.PlaceholderScreen

object Routes {
  const val LOGIN = "login"
  const val HOME = "home"
  const val VOCABULARY = "vocabulary"
  const val GRAMMAR = "grammar"
  const val TROPHIES = "trophies"
  const val SETTINGS = "settings"
  const val MATCH = "practice/match"
  const val TYPE = "practice/type"
  const val CHOICE = "practice/choice"
  const val RESULT = "practice/result"
  const val OVERVIEW = "overview/{level}/{listId}"
  const val GRAMMAR_OVERVIEW = "grammar/overview/{type}"

  fun overview(level: String, listId: String) = "overview/$level/$listId"
}

@Composable
fun AppNavHost(nav: NavHostController = rememberNavController()) {
  val application = LocalContext.current.applicationContext as Application
  // Eigen factories: de standaard factory onthoudt statisch de eerste Application (stoort tests).
  val vm: AppViewModel = viewModel(factory = viewModelFactory { initializer { AppViewModel(application) } })
  val data by vm.data.collectAsState()
  val importState by vm.importState.collectAsState()
  val practice: PracticeViewModel = viewModel(factory = viewModelFactory { initializer { PracticeViewModel(application) } })
  val grammar = (application as CommSkillsApp).grammar
  val matchUi by practice.match.collectAsState()
  val typeUi by practice.type.collectAsState()
  val result by practice.result.collectAsState()
  val examUi by practice.exam.collectAsState()
  val choiceUi by practice.choice.collectAsState()

  fun startGrammarExam(type: Int) {
    practice.startGrammarExam(type)
    nav.navigate(Routes.TYPE)
  }

  fun startPartExam(level: String, partId: String) {
    practice.startPartExam(level, partId)
    nav.navigate(
      when (level) {
        Levels.VOCAB_TYPE -> Routes.TYPE
        Levels.VOCAB_MATCH -> Routes.MATCH
        else -> Routes.CHOICE
      },
    )
  }

  // Zodra een sessie klaar is, naar het resultaat.
  LaunchedEffect(result) {
    if (result != null && nav.currentDestination?.route != Routes.RESULT) {
      val base = if (result?.level == Levels.GRAMMAR) Routes.GRAMMAR else Routes.VOCABULARY
      nav.navigate(Routes.RESULT) { popUpTo(base) { inclusive = false } }
    }
  }
  val app = application as CommSkillsApp
  val activity = LocalContext.current.findActivity()
  val scope = rememberCoroutineScope()
  var loginError by remember { mutableStateOf<String?>(null) }
  var loginBusy by remember { mutableStateOf(false) }
  var signedInEmail by remember { mutableStateOf(app.cloud.email) }

  fun signIn(onDone: () -> Unit = {}) {
    val a = activity ?: return
    loginBusy = true
    loginError = null
    scope.launch {
      try {
        if (app.cloud.signIn(a)) {
          signedInEmail = app.cloud.email
          onDone()
        } else {
          loginError = "Inloggen is niet beschikbaar: Firebase is niet volledig ingesteld."
        }
      } catch (e: Exception) {
        loginError = "Inloggen is mislukt. Probeer opnieuw."
      } finally {
        loginBusy = false
      }
    }
  }

  val start = if (app.cloud.needsLogin()) Routes.LOGIN else Routes.HOME
  NavHost(navController = nav, startDestination = start) {
    composable(Routes.LOGIN) {
      LoginScreen(
        error = loginError,
        busy = loginBusy,
        onGoogle = { signIn { nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } } },
        onLocal = {
          app.cloud.chooseLocalOnly()
          nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
        },
      )
    }
    composable(Routes.HOME) {
      HomeScreen(
        data = data,
        today = vm.today(),
        onVocabulary = { nav.navigate(Routes.VOCABULARY) },
        onGrammar = { nav.navigate(Routes.GRAMMAR) },
        onTrophies = { nav.navigate(Routes.TROPHIES) },
        onSettings = { nav.navigate(Routes.SETTINGS) },
        grammarPercentage = com.robpolus.commskills.domain.Grammar.view(data, grammar).percentage(),
      )
    }
    composable(Routes.VOCABULARY) {
      VocabularyScreen(
        data = data,
        onBack = { nav.popBackStack() },
        onPlay = { level, groups, direction ->
          if (level == Levels.VOCAB_MATCH) {
            practice.startMatch(level, groups)
            nav.navigate(Routes.MATCH)
          } else if (level == Levels.VOCAB_CHOICE) {
            practice.startChoice(level, groups)
            nav.navigate(Routes.CHOICE)
          } else {
            practice.startType(level, groups, direction)
            nav.navigate(Routes.TYPE)
          }
        },
        onOverview = { level, listId -> nav.navigate(Routes.overview(level, listId)) },
        onExam = { level, partId -> startPartExam(level, partId) },
      )
    }
    composable(Routes.OVERVIEW) { entry ->
      WordOverviewScreen(
        data = data,
        level = entry.arguments?.getString("level").orEmpty(),
        listId = entry.arguments?.getString("listId").orEmpty(),
        onBack = { nav.popBackStack() },
        onRemoveExtra = { key, text -> vm.update { com.robpolus.commskills.domain.Learning.removeExtraAnswer(it, key, text) } },
      )
    }
    composable(Routes.MATCH) {
      MatchScreen(
        ui = matchUi,
        onSelect = practice::selectTile,
        onQuit = { practice.abandon(); nav.popBackStack() },
        topExtra = { ExamHearts(examUi) },
      )
    }
    composable(Routes.TYPE) {
      TypeScreen(
        ui = typeUi,
        onSubmit = practice::submitAnswer,
        onNext = practice::nextQuestion,
        onAccept = practice::acceptAnyway,
        onTip = practice::requestTip,
        onGiveUp = practice::giveUp,
        onQuit = { practice.abandon(); nav.popBackStack() },
        topExtra = { ExamHearts(examUi) },
      )
    }
    composable(Routes.CHOICE) {
      ChoiceScreen(
        ui = choiceUi,
        onPick = practice::pickOption,
        onNext = practice::nextChoice,
        onQuit = { practice.abandon(); nav.popBackStack() },
        topExtra = { ExamHearts(examUi) },
      )
    }
    composable(Routes.RESULT) {
      ResultScreen(
        result = result,
        onDone = { practice.clearResult(); nav.popBackStack() },
      )
    }
    composable(Routes.GRAMMAR) {
      GrammarScreen(
        data = data,
        exercises = grammar,
        onBack = { nav.popBackStack() },
        onPlay = { groups ->
          practice.startGrammar(groups)
          nav.navigate(Routes.TYPE)
        },
        onOverview = { type -> nav.navigate("grammar/overview/$type") },
        onExam = { type -> startGrammarExam(type) },
      )
    }
    composable(Routes.GRAMMAR_OVERVIEW) { entry ->
      GrammarOverviewScreen(
        data = data,
        exercises = grammar,
        type = entry.arguments?.getString("type")?.toIntOrNull() ?: 0,
        onBack = { nav.popBackStack() },
      )
    }
    composable(Routes.TROPHIES) { TrophiesScreen(data) { nav.popBackStack() } }
    composable(Routes.SETTINGS) {
      SettingsScreen(
        data = data,
        importState = importState,
        onPickFile = vm::importDocx,
        onRename = vm::renameImport,
        onConfirmImport = vm::confirmImport,
        onCancelImport = vm::cancelImport,
        onBack = { nav.popBackStack() },
        onResetList = { id -> vm.update { com.robpolus.commskills.domain.Learning.resetList(it, id) } },
        onResetProgress = { vm.update { com.robpolus.commskills.domain.Learning.resetProgress(it) } },
        extra = {
          AccountSettings(
            configured = app.cloud.configured,
            email = signedInEmail,
            onSignIn = { signIn() },
            onSignOut = {
              scope.launch {
                app.cloud.signOut(application)
                signedInEmail = null
              }
            },
          )
          ReminderSettings(data.settings, vm::setReminder)
        },
      )
    }
  }
}

private fun Context.findActivity(): android.app.Activity? {
  var c: Context? = this
  while (c is ContextWrapper) {
    if (c is android.app.Activity) return c
    c = c.baseContext
  }
  return null
}
