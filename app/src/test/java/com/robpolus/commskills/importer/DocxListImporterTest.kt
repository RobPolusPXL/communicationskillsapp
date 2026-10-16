package com.robpolus.commskills.importer

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DocxListImporterTest {

  // --- hulpfuncties om een minimaal .docx te bouwen ----------------------------------------------

  private fun cell(text: String) = "<w:tc><w:p><w:r><w:t>${esc(text)}</w:t></w:r></w:p></w:tc>"
  private fun cellRaw(inner: String) = "<w:tc>$inner</w:tc>"
  private fun row(vararg cells: String) = "<w:tr>${cells.joinToString("")}</w:tr>"
  private fun table(vararg rows: String) = "<w:tbl>${rows.joinToString("")}</w:tbl>"
  private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;")

  private fun wordRow(en: String, def: String, nl: String) = row(cell(en), cell(def), cell(nl))

  private val header = row(cell("Word and Type"), cell("Explanation/Definition"), cell("Dutch Translation"))

  private fun docx(body: String, entryName: String = "word/document.xml"): ByteArrayInputStream {
    val xml = """<?xml version="1.0" encoding="UTF-8"?>
      <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>$body</w:body></w:document>"""
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zip ->
      zip.putNextEntry(ZipEntry(entryName))
      zip.write(xml.toByteArray(Charsets.UTF_8))
      zip.closeEntry()
    }
    return ByteArrayInputStream(out.toByteArray())
  }

  private fun import(body: String) = DocxListImporter.parse(docx(body), "Test")

  // --- tests -------------------------------------------------------------------------------------

  @Test
  fun readsBasicWordTable() {
    val result = import(
      table(header, wordRow("pursue (v)", "to follow", "nastreven"), wordRow("road (n)", "a way", "weg")),
    )
    assertEquals(2, result.words.size)
    val w = result.words[0]
    assertEquals("pursue", w.en)
    assertEquals("v", w.type)
    assertEquals("to follow", w.definition)
    assertEquals(listOf("nastreven"), w.nl)
    assertTrue(result.warnings.isEmpty())
  }

  @Test
  fun splitsDutchVariants() {
    val result = import(
      table(
        header,
        wordRow("grant (v)", "give", "eer toekennen; crediteren, toekennen"),
        wordRow("train (v)", "teach", "opleiden/een opleiding volgen"),
      ),
    )
    assertEquals(listOf("eer toekennen", "crediteren", "toekennen"), result.words[0].nl)
    assertEquals(listOf("opleiden", "een opleiding volgen"), result.words[1].nl)
  }

  @Test
  fun doesNotSplitInsideParentheses() {
    assertEquals(
      listOf("(na)streven", "streven(d) naar"),
      DocxListImporter.splitVariants("(na)streven, streven(d) naar"),
    )
    assertEquals(listOf("a (b, c) d"), DocxListImporter.splitVariants("a (b, c) d"))
  }

  @Test
  fun handlesOddTypePlacement() {
    assertEquals("to aspire (to)" to "v", DocxListImporter.splitType("to aspire (to)(v)"))
    assertEquals("touch (upon)" to "v", DocxListImporter.splitType("touch (upon) (v)"))
    assertEquals("on behalf of" to "phrase", DocxListImporter.splitType("on behalf of (phrase)"))
    assertEquals("take off" to "phrasal v", DocxListImporter.splitType("take off (phrasal v)"))
    assertEquals("touch (upon)" to null, DocxListImporter.splitType("touch (upon)"))
  }

  @Test
  fun skipsEmptyRowsAndRepeatedHeaders() {
    val empty = row(cell(""), cell(""), cell(""))
    val result = import(table(header, empty, wordRow("road (n)", "a way", "weg"), header))
    assertEquals(1, result.words.size)
  }

  @Test
  fun flattensNestedTablesInCells() {
    val nested = cellRaw(
      "<w:p/>" + table(row(cell("opleiden"), cell("onderwijzen"))) + "<w:p><w:r><w:t>extra</w:t></w:r></w:p>",
    )
    val result = import(table(header, row(cell("educate (v)"), cell("teach"), nested)))
    assertEquals(1, result.words.size)
    assertEquals(listOf("opleiden onderwijzen extra"), result.words[0].nl)
  }

  @Test
  fun skipsNonWordTablesWithWarning() {
    val phrases = table(row(cell("English"), cell("Nederlands")), row(cell("I am attaching"), cell("Ik voeg bij")))
    val result = import(table(header, wordRow("road (n)", "a way", "weg")) + phrases)
    assertEquals(1, result.words.size)
    assertEquals(1, result.warnings.size)
    assertTrue(result.warnings[0].contains("Tabel 2"))
  }

  @Test
  fun skipsWordWithoutDutchAndWarnsAboutMissingDefinition() {
    val result = import(
      table(header, wordRow("road (n)", "a way", ""), wordRow("path (n)", "", "pad")),
    )
    assertEquals(listOf("path"), result.words.map { it.en })
    assertTrue(result.warnings.any { it.contains("road") && it.contains("vertaling") })
    assertTrue(result.warnings.any { it.contains("path") && it.contains("uitleg") })
  }

  @Test
  fun dedupesWithinList() {
    val result = import(
      table(header, wordRow("road (n)", "a way", "weg"), wordRow("road (n)", "a way", "straat")),
    )
    assertEquals(1, result.words.size)
    assertEquals(listOf("weg"), result.words[0].nl)
    assertTrue(result.warnings.any { it.contains("dubbel") })
  }

  @Test
  fun sameSpellingWithDifferentTypeIsNotADuplicate() {
    val result = import(
      table(header, wordRow("train (n)", "vehicle", "trein"), wordRow("train (v)", "teach", "opleiden")),
    )
    assertEquals(2, result.words.size)
    assertEquals(2, result.words.map { it.id }.toSet().size)
  }

  @Test
  fun idsAreStable() {
    val a = import(table(header, wordRow("Café au lait (n)", "x", "koffie")))
    val b = import(table(header, wordRow("Café au lait (n)", "x", "koffie")))
    assertEquals(a.words[0].id, b.words[0].id)
    assertEquals("cafe_au_lait_n", a.words[0].id)
  }

  @Test
  fun typeMissingGivesNullTypeAndWarning() {
    val result = import(table(header, wordRow("road", "a way", "weg")))
    assertNull(result.words[0].type)
    assertTrue(result.warnings.isNotEmpty())
  }

  @Test
  fun fileWithoutWordTableFails() {
    try {
      import(table(row(cell("English"), cell("Nederlands"))))
      fail("verwacht ImportException")
    } catch (e: ImportException) {
      assertTrue(e.message!!.contains("woordtabel"))
    }
  }

  @Test
  fun nonDocxFails() {
    try {
      DocxListImporter.parse(docx("", entryName = "something/else.xml"), "x")
      fail("verwacht ImportException")
    } catch (e: ImportException) {
      assertTrue(e.message!!.contains("geldig"))
    }
    try {
      DocxListImporter.parse(ByteArrayInputStream("geen zip".toByteArray()), "x")
      fail("verwacht ImportException")
    } catch (e: ImportException) {
      // ok
    }
  }

  @Test
  fun titleFromFileName() {
    assertEquals("Business writing", DocxListImporter.titleFromFileName("wordlist_business_writing.docx"))
    assertEquals("Trolleyproblem", DocxListImporter.titleFromFileName("/x/y/trolleyproblem.docx"))
    assertEquals("Applyingforajob", DocxListImporter.titleFromFileName("5d378412 applyingforajob voclist.docx"))
    assertEquals("Academic writing", DocxListImporter.titleFromFileName("60346729 wordlist academic writing.docx"))
    assertEquals("Nieuwe lijst", DocxListImporter.titleFromFileName(".docx"))
  }
}
