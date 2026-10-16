package com.robpolus.commskills.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.robpolus.commskills.domain.Levels
import com.robpolus.commskills.ui.PracticeViewModel
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

  fun overview(level: String, listId: String) = "overview/$level/$listId"
}

@Composable
fun AppNavHost(nav: NavHostController = rememberNavController()) {
  val vm: AppViewModel = viewModel()
  val data by vm.data.collectAsState()
  val importState by vm.importState.collectAsState()
  val practice: PracticeViewModel = viewModel()
  val matchUi by practice.match.collectAsState()
  val typeUi by practice.type.collectAsState()
  val result by practice.result.collectAsState()

  // Zodra een sessie klaar is, naar het resultaat.
  LaunchedEffect(result) {
    if (result != null && nav.currentDestination?.route != Routes.RESULT) {
      nav.navigate(Routes.RESULT) { popUpTo(Routes.VOCABULARY) { inclusive = false } }
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
      )
    }
    composable(Routes.TYPE) {
      TypeScreen(
        ui = typeUi,
        onSubmit = practice::submitAnswer,
        onNext = practice::nextQuestion,
        onQuit = { practice.abandon(); nav.popBackStack() },
      )
    }
    composable(Routes.RESULT) {
      ResultScreen(result = result, onDone = { practice.clearResult(); nav.popBackStack() })
    }
    composable(Routes.GRAMMAR) { PlaceholderScreen("Grammar") { nav.popBackStack() } }
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
