package com.robpolus.commskills.notify

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.robpolus.commskills.CommSkillsApp
import com.robpolus.commskills.domain.Reminders
import java.time.LocalDate

/** Toont de dagelijkse herinnering (tenzij er al geoefend is) en plant de volgende. Plant ook opnieuw na een herstart. */
class ReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val repository = (context.applicationContext as CommSkillsApp).repository
    val data = repository.data.value
    if (intent.action != Intent.ACTION_BOOT_COMPLETED && Reminders.shouldNotify(data, LocalDate.now())) {
      val allowed = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
      if (allowed) ReminderScheduler.notify(context)
    }
    ReminderScheduler.schedule(context, data)
  }
}
