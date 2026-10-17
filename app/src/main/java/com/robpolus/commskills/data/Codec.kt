package com.robpolus.commskills.data

import com.robpolus.commskills.domain.AppData
import com.robpolus.commskills.domain.DailyStat
import com.robpolus.commskills.domain.SessionStat
import com.robpolus.commskills.domain.Settings
import com.robpolus.commskills.domain.StreakState
import com.robpolus.commskills.domain.Word
import com.robpolus.commskills.domain.WordList
import com.robpolus.commskills.domain.WordProgress
import org.json.JSONArray
import org.json.JSONObject

/** Zet [AppData] om van en naar JSON (zowel voor het bestand op het toestel als voor de cloud). */
object Codec {

  private const val VERSION = 1

  fun encode(d: AppData): String = JSONObject().apply {
    put("version", VERSION)
    put("updatedAt", d.updatedAt)
    put("lists", JSONArray().apply { d.lists.forEach { put(encodeList(it)) } })
    put("progress", JSONObject().apply {
      d.progress.forEach { (k, p) ->
        put(k, JSONArray().put(p.score).put(p.correct).put(p.wrong).put(p.played).put(p.streak))
      }
    })
    put("sessions", JSONObject().apply {
      d.sessions.forEach { (k, s) -> put(k, JSONArray().put(s.completed).put(s.lastPerfect).put(s.perfectCount)) }
    })
    put("examPassed", JSONArray(d.examPassed.toList()))
    put("medals", JSONObject().apply { d.medals.forEach { (k, v) -> put(k, v) } })
    put("levelReached", JSONArray(d.levelReached.toList()))
    put("streak", JSONObject().put("current", d.streak.current).put("longest", d.streak.longest)
      .put("lastDate", d.streak.lastDate ?: JSONObject.NULL))
    put("daily", JSONObject().apply {
      d.daily.forEach { (k, s) -> put(k, JSONArray().put(s.activeSeconds).put(s.exercises)) }
    })
    put("settings", JSONObject().put("enabled", d.settings.reminderEnabled)
      .put("hour", d.settings.reminderHour).put("minute", d.settings.reminderMinute))
    put("seededAssets", JSONArray(d.seededAssets.toList()))
  }.toString()

  fun decode(json: String): AppData {
    val o = JSONObject(json)
    return AppData(
      lists = o.optJSONArray("lists").objects().map(::decodeList),
      progress = o.optJSONObject("progress").entries { a ->
        WordProgress(a.getDouble(0), a.getInt(1), a.getInt(2), a.getBoolean(3), a.optInt(4, 0))
      },
      sessions = o.optJSONObject("sessions").entries { a ->
        SessionStat(a.getInt(0), a.getBoolean(1), a.getInt(2))
      },
      examPassed = o.optJSONArray("examPassed").strings().toSet(),
      medals = o.optJSONObject("medals")?.let { m -> m.keys().asSequence().associateWith { m.getInt(it) } } ?: emptyMap(),
      levelReached = o.optJSONArray("levelReached").strings().toSet(),
      streak = o.optJSONObject("streak")?.let {
        StreakState(it.optInt("current"), it.optInt("longest"), if (it.isNull("lastDate")) null else it.getString("lastDate"))
      } ?: StreakState(),
      daily = o.optJSONObject("daily").entries { a -> DailyStat(a.getInt(0), a.getInt(1)) },
      settings = o.optJSONObject("settings")?.let {
        Settings(it.optBoolean("enabled"), it.optInt("hour", 19), it.optInt("minute", 0))
      } ?: Settings(),
      seededAssets = o.optJSONArray("seededAssets").strings().toSet(),
      updatedAt = o.optLong("updatedAt"),
    )
  }

  private fun encodeList(l: WordList) = JSONObject().apply {
    put("id", l.id)
    put("title", l.title)
    put("words", JSONArray().apply {
      l.words.forEach { w ->
        put(JSONObject().put("id", w.id).put("en", w.en).put("type", w.type ?: JSONObject.NULL)
          .put("def", w.definition).put("nl", JSONArray(w.nl)))
      }
    })
  }

  private fun decodeList(o: JSONObject) = WordList(
    id = o.getString("id"),
    title = o.getString("title"),
    words = o.getJSONArray("words").objects().map { w ->
      Word(
        id = w.getString("id"),
        en = w.getString("en"),
        type = if (w.isNull("type")) null else w.getString("type"),
        definition = w.optString("def"),
        nl = w.getJSONArray("nl").strings(),
      )
    },
  )

  private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }

  private fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else (0 until length()).map { getString(it) }

  private fun <T> JSONObject?.entries(read: (JSONArray) -> T): Map<String, T> {
    if (this == null) return emptyMap()
    val out = LinkedHashMap<String, T>()
    for (key in keys()) out[key] = read(getJSONArray(key))
    return out
  }
}
