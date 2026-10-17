package com.robpolus.commskills.importer

import com.robpolus.commskills.domain.AppData
import com.robpolus.commskills.domain.Learning
import com.robpolus.commskills.domain.Levels
import com.robpolus.commskills.domain.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListMergeTest {
  private fun word(id: String, nl: String) = Word(id, id, null, "", listOf(nl))
  private fun imported(title: String, vararg words: Word) = ImportedList(title, words.toList(), emptyList())

  @Test
  fun idIsAStableSlug() {
    assertEquals("business_writing", ListMerge.listId("Business writing"))
    assertEquals("cafe_ethics", ListMerge.listId("Café  Ethics!"))
  }

  @Test
  fun addsNewList() {
    val d = ListMerge.apply(AppData(), imported("Een", word("a", "x")))
    assertEquals(listOf("een"), d.lists.map { it.id })
    assertTrue(ListMerge.exists(d, "Een"))
  }

  @Test
  fun sameTitleReplacesWordsAndKeepsProgress() {
    var d = ListMerge.apply(AppData(), imported("Een", word("a", "x"), word("b", "y")))
    d = Learning.recordAnswer(d, Levels.VOCAB_MATCH, "een", "a", true)
    d = ListMerge.apply(d, imported("Een", word("a", "x2"), word("c", "z")))
    assertEquals(1, d.lists.size)
    assertEquals(listOf("a", "c"), d.lists[0].words.map { it.id })
    assertEquals(0.5, Learning.vocabularyView(d, Levels.VOCAB_MATCH).score("een", "a"), 1e-9)
  }
}
