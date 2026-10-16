package com.robpolus.commskills.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.robpolus.commskills.MainActivity
import com.robpolus.commskills.R
import com.robpolus.commskills.domain.AppData
import com.robpolus.commskills.domain.Reminders
import java.time.LocalDateTime
import java.time.ZoneId

object ReminderScheduler {
  private const val CHANNEL = "reminders"
  private const val REQUEST = 4711
  private const val NOTIFICATION_ID = 1

  private fun pending(context: Context): PendingIntent =
    PendingIntent.getBroadcast(
      context,
      REQUEST,
      Intent(context, ReminderReceiver::class.java),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

  /** Plant de volgende herinnering (of annuleert ze als ze uit staan). */
  fun schedule(context: Context, data: AppData) {
    val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarms.cancel(pending(context))
    val s = data.settings
    if (!s.reminderEnabled) return
    val next = Reminders.nextTrigger(LocalDateTime.now(), s.reminderHour, s.reminderMinute)
    val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    // Niet-exact: de melding mag enkele minuten schuiven, daar is geen extra toestemming voor nodig.
    alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending(context))
  }

  fun notify(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(
      NotificationChannel(CHANNEL, "Dagelijkse herinnering", NotificationManager.IMPORTANCE_DEFAULT),
    )
    val open = PendingIntent.getActivity(
      context,
      0,
      Intent(context, MainActivity::class.java),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val notification = NotificationCompat.Builder(context, CHANNEL)
      .setSmallIcon(R.drawable.ic_launcher)
      .setContentTitle("Tijd om te oefenen")
      .setContentText("Houd je streak in leven met een korte oefening.")
      .setContentIntent(open)
      .setAutoCancel(true)
      .build()
    manager.notify(NOTIFICATION_ID, notification)
  }
}
