package com.robpolus.commskills.ui.navigation

import androidx.compose.runtime.Composable
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
}

@Composable
fun AppNavHost(nav: NavHostController = rememberNavController()) {
  val vm: AppViewModel = viewModel()
  val data by vm.data.collectAsState()
  val importState by vm.importState.collectAsState()
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
    composable(Routes.VOCABULARY) { PlaceholderScreen("Vocabulary") { nav.popBackStack() } }
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
