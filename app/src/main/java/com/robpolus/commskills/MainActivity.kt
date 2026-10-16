package com.robpolus.commskills

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.robpolus.commskills.ui.navigation.AppNavHost
import com.robpolus.commskills.ui.theme.CommSkillsTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      CommSkillsTheme {
        AppNavHost()
      }
    }
  }

  override fun onStop() {
    super.onStop()
    val app = application as CommSkillsApp
    app.repository.flushNow()
    if (app.cloud.signedIn) app.cloud.flush()
  }
}
