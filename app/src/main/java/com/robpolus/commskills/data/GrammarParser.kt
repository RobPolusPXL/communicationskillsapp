package com.robpolus.commskills.data

import com.robpolus.commskills.domain.GrammarExercise
import org.json.JSONObject

object GrammarParser {
  fun parse(json: String): List<GrammarExercise> {
    val array = JSONObject(json).getJSONArray("exercises")
    return (0 until array.length()).map { i ->
      val o = array.getJSONObject(i)
      val answers = o.getJSONArray("answers")
      GrammarExercise(
        id = o.getString("id"),
        type = o.getInt("type"),
        text = o.getString("text"),
        hint = o.optString("hint"),
        answers = (0 until answers.length()).map { answers.getString(it) },
        explain = o.optString("explain"),
      )
    }
  }
}
