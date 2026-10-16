package com.robpolus.commskills.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.robpolus.commskills.CommSkillsApp
import com.robpolus.commskills.ui.screens.GrammarOverviewScreen
import com.robpolus.commskills.ui.screens.GrammarScreen
import com.robpolus.commskills.domain.Levels
import com.robpolus.commskills.ui.PracticeViewModel
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
  const val RESULT = "practice/result"
  const val OVERVIEW = "overview/{level}/{listId}"
  const val GRAMMAR_OVERVIEW = "grammar/overview/{type}"

  fun overview(level: String, listId: String) = "overview/$level/$listId"
}

@Composable
fun AppNavHost(nav: NavHostController = rememberNavController()) {
  val vm: AppViewModel = viewModel()
  val data by vm.data.collectAsState()
  val importState by vm.importState.collectAsState()
  val practice: PracticeViewModel = viewModel()
  val grammar = (LocalContext.current.applicationContext as CommSkillsApp).grammar
  val matchUi by practice.match.collectAsState()
  val typeUi by practice.type.collectAsState()
  val result by practice.result.collectAsState()
  val examUi by practice.exam.collectAsState()

  fun startExam(level: String) {
    practice.startExam(level)
    nav.navigate(if (level == Levels.VOCAB_MATCH) Routes.MATCH else Routes.TYPE)
  }

  // Zodra een sessie klaar is, naar het resultaat.
  LaunchedEffect(result) {
    if (result != null && nav.currentDestination?.route != Routes.RESULT) {
      val base = if (result?.level == Levels.GRAMMAR) Routes.GRAMMAR else Routes.VOCABULARY
      nav.navigate(Routes.RESULT) { popUpTo(base) { inclusive = false } }
    }
  }
  NavHost(navController = nav, startDestination = Routes.LOGIN) {
    composable(Routes.LOGIN) {
      LoginScreen(
        onContinue = {
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
          } else {
            practice.startType(level, groups, direction)
            nav.navigate(Routes.TYPE)
          }
        },
        onOverview = { level, listId -> nav.navigate(Routes.overview(level, listId)) },
        onExam = { level -> startExam(level) },
      )
    }
    composable(Routes.OVERVIEW) { entry ->
      WordOverviewScreen(
        data = data,
        level = entry.arguments?.getString("level").orEmpty(),
        listId = entry.arguments?.getString("listId").orEmpty(),
        onBack = { nav.popBackStack() },
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
        onQuit = { practice.abandon(); nav.popBackStack() },
        topExtra = { ExamHearts(examUi) },
      )
    }
    composable(Routes.RESULT) {
      ResultScreen(
        result = result,
        onDone = { practice.clearResult(); nav.popBackStack() },
        onExam = { level ->
          practice.clearResult()
          nav.popBackStack()
          startExam(level)
        },
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
        onExam = { level -> startExam(level) },
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
      )
    }
  }
}
