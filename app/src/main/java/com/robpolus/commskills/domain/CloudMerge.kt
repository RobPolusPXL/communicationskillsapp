package com.robpolus.commskills.domain

/** Beslist wat er bij het synchroniseren met de cloud gebeurt. Laatste wijziging wint. */
object CloudMerge {

  enum class Decision { USE_CLOUD, PUSH_LOCAL, NOTHING }

  /** Een toestel zonder eigen voortgang (net geïnstalleerd) neemt altijd de cloud over. */
  fun isFresh(d: AppData): Boolean =
    d.progress.isEmpty() && d.sessions.isEmpty() && d.daily.isEmpty() && d.examPassed.isEmpty()

  fun decide(local: AppData, cloud: AppData?): Decision = when {
    cloud == null -> Decision.PUSH_LOCAL
    isFresh(local) && !isFresh(cloud) -> Decision.USE_CLOUD
    cloud.updatedAt > local.updatedAt -> Decision.USE_CLOUD
    cloud.updatedAt < local.updatedAt -> Decision.PUSH_LOCAL
    else -> Decision.NOTHING
  }
}
