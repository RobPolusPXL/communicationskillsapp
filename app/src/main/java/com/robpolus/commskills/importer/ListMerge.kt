package com.robpolus.commskills.importer

import com.robpolus.commskills.domain.AppData
import com.robpolus.commskills.domain.WordList
import java.text.Normalizer

/** Voegt een ingelezen lijst toe aan de gegevens, zonder voortgang te verliezen. */
object ListMerge {

  fun listId(title: String): String {
    val slug = Normalizer.normalize(title, Normalizer.Form.NFD)
      .replace(Regex("\\p{Mn}+"), "")
      .lowercase()
      .replace(Regex("[^a-z0-9]+"), "_")
      .trim('_')
    return slug.ifEmpty { "lijst" }
  }

  /** `true` als er al een lijst met deze titel is (de woorden worden dan vervangen, voortgang blijft). */
  fun exists(data: AppData, title: String) = data.lists.any { it.id == listId(title) }

  fun apply(data: AppData, imported: ImportedList): AppData {
    val id = listId(imported.title)
    val list = WordList(id, imported.title, imported.words)
    val lists = if (data.lists.any { it.id == id }) {
      data.lists.map { if (it.id == id) list else it }
    } else {
      data.lists + list
    }
    return data.copy(lists = lists)
  }
}
