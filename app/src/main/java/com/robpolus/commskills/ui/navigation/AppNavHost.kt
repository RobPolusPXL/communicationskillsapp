package com.robpolus.commskills.ui.navigation

import androidx.compose.runtime.Composable
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
        onVocabulary = { nav.navigate(Routes.VOCABULARY) },
        onGrammar = { nav.navigate(Routes.GRAMMAR) },
        onTrophies = { nav.navigate(Routes.TROPHIES) },
        onSettings = { nav.navigate(Routes.SETTINGS) },
      )
    }
    composable(Routes.VOCABULARY) { PlaceholderScreen("Vocabulary") { nav.popBackStack() } }
    composable(Routes.GRAMMAR) { PlaceholderScreen("Grammar") { nav.popBackStack() } }
    composable(Routes.TROPHIES) { PlaceholderScreen("Trofeeën") { nav.popBackStack() } }
    composable(Routes.SETTINGS) { PlaceholderScreen("Instellingen") { nav.popBackStack() } }
  }
}
