package com.robpolus.commskills.domain

fun w(en: String, vararg nl: String) = Word(id = en, en = en, type = null, definition = "", nl = nl.toList())

fun sampleData(): AppData = AppData(
  lists = listOf(
    WordList("a", "A", listOf(w("one", "een"), w("two", "twee"), w("three", "drie"))),
    WordList("b", "B", listOf(w("four", "vier"), w("five", "vijf"))),
  ),
)
