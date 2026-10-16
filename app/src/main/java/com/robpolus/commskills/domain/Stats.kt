package com.robpolus.commskills.domain

import java.time.LocalDate

object Stats {
  const val DAILY_GOAL_SECONDS = 10 * 60
  const val INACTIVITY_CUTOFF_MS = 30_000L

  /** Tel één afgeronde oefening: verhoogt de streak (eenmaal per dag) en de dagteller. */
  fun registerExercise(data: AppData, today: LocalDate): AppData {
    val key = today.toString()
    val streak = data.streak
    val updated = when (streak.lastDate) {
      key -> streak
      today.minusDays(1).toString() -> streak.copy(current = streak.current + 1, lastDate = key)
      else -> streak.copy(current = 1, lastDate = key)
    }.let { it.copy(longest = maxOf(it.longest, it.current)) }
    val day = data.daily[key] ?: DailyStat()
    return data.copy(streak = updated, daily = data.daily + (key to day.copy(exercises = day.exercises + 1)))
  }

  fun addActiveSeconds(data: AppData, today: LocalDate, seconds: Int): AppData {
    if (seconds <= 0) return data
    val key = today.toString()
    val day = data.daily[key] ?: DailyStat()
    return data.copy(daily = data.daily + (key to day.copy(activeSeconds = day.activeSeconds + seconds)))
  }

  /** Streak zoals getoond: een gemiste dag zet ze op 0. */
  fun effectiveStreak(streak: StreakState, today: LocalDate): Int = when (streak.lastDate) {
    today.toString(), today.minusDays(1).toString() -> streak.current
    else -> 0
  }

  fun todaySeconds(data: AppData, today: LocalDate): Int = data.daily[today.toString()]?.activeSeconds ?: 0

  fun practicedToday(data: AppData, today: LocalDate): Boolean = (data.daily[today.toString()]?.exercises ?: 0) > 0

  fun dailyGoalReached(data: AppData, today: LocalDate): Boolean = todaySeconds(data, today) >= DAILY_GOAL_SECONDS
}

/**
 * Meet actieve oefentijd: bij elke handeling telt de tijd sinds de vorige handeling mee,
 * met een maximum van 30 seconden (daarna is het pauze).
 */
class ActiveTimer {
  private var lastMs: Long? = null
  private var carryMs = 0L

  /** Geeft de hele seconden terug die bij de totale actieve tijd moeten komen. */
  fun activity(nowMs: Long): Int {
    val last = lastMs
    lastMs = nowMs
    if (last == null) return 0
    carryMs += (nowMs - last).coerceIn(0, Stats.INACTIVITY_CUTOFF_MS)
    val seconds = (carryMs / 1000).toInt()
    carryMs %= 1000
    return seconds
  }

  /** Bij het verlaten van een oefenscherm. */
  fun reset() {
    lastMs = null
    carryMs = 0
  }
}
