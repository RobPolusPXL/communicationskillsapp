package com.robpolus.commskills.domain

import java.time.LocalDate
import java.time.LocalDateTime

object Reminders {

  /** Eerstvolgende tijdstip van de dagelijkse herinnering: vandaag als het nog niet voorbij is, anders morgen. */
  fun nextTrigger(now: LocalDateTime, hour: Int, minute: Int): LocalDateTime {
    val today = now.toLocalDate().atTime(hour, minute)
    return if (today.isAfter(now)) today else today.plusDays(1)
  }

  /** Alleen een melding als herinneringen aan staan en er vandaag nog niet geoefend is. */
  fun shouldNotify(data: AppData, today: LocalDate): Boolean =
    data.settings.reminderEnabled && !Stats.practicedToday(data, today)
}
