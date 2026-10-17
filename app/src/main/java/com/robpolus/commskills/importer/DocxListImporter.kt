package com.robpolus.commskills.importer

import com.robpolus.commskills.domain.Word
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.Normalizer
import java.util.zip.ZipInputStream
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler

/** Resultaat van een import: de woorden plus meldingen over wat overgeslagen of onvolledig was. */
data class ImportedList(
  val title: String,
  val words: List<Word>,
  val warnings: List<String>,
)

class ImportException(message: String) : Exception(message)

/**
 * Leest een Word-bestand (.docx) met woordtabellen in en maakt er een woordenlijst van.
 *
 * Verwacht per tabel de kolommen `Word and Type | Explanation/Definition | Dutch Translation`.
 * Tabellen met een andere kop worden overgeslagen (met een melding).
 * Een .docx is een zip; de tekst staat in `word/document.xml`, die met SAX gelezen wordt
 * (werkt op JVM en Android, dus eenvoudig te testen).
 */
object DocxListImporter {

  private const val MAX_XML_BYTES = 20 * 1024 * 1024
  private const val HEADER = "word and type"

  private val knownTypes = setOf(
    "n", "v", "adj", "adv", "phrase", "verb phrase", "phrasal v", "phrasal verb",
    "prep", "conj", "pron", "interj",
  )
  private val trailingGroup = Regex("\\(([^()]*)\\)\\s*$")
  private val noiseWords = setOf("wordlist", "voclist", "vocabulary", "list")
  private val spaces = Regex("[\\s\\u00A0]+")

  fun parse(input: InputStream, title: String): ImportedList {
    val xml = readDocumentXml(input)
    val tables = readTables(xml)
    val warnings = mutableListOf<String>()
    val words = LinkedHashMap<String, Word>()

    var wordTables = 0
    tables.forEachIndexed { index, rows ->
      val first = rows.firstOrNull()?.firstOrNull()?.lowercase()
      if (first != HEADER) {
        warnings += "Tabel ${index + 1} overgeslagen: geen woordtabel (kop \"${first.orEmpty()}\")."
        return@forEachIndexed
      }
      wordTables++
      for (row in rows) readRow(row, words, warnings)
    }
    if (wordTables == 0) throw ImportException("Geen woordtabel gevonden in dit bestand.")
    if (words.isEmpty()) throw ImportException("De woordtabel bevat geen woorden.")
    return ImportedList(title, words.values.toList(), warnings)
  }

  /** "wordlist_business_writing.docx" → "Business writing"; "applyingforajob voclist.docx" → "Applyingforajob". */
  fun titleFromFileName(fileName: String): String {
    var name = fileName.substringAfterLast('/').substringAfterLast('\\')
    name = name.substringBeforeLast('.', name)
    name = name.replace('_', ' ').replace('-', ' ')
    name = name.replace(Regex("^[0-9a-fA-F]{8}\\s+"), "")
    name = name.split(' ').filter { it.isNotBlank() && it.lowercase() !in noiseWords }.joinToString(" ")
    return name.replaceFirstChar { it.uppercase() }.ifEmpty { "Nieuwe lijst" }
  }

  // --- rijen -----------------------------------------------------------------------------------

  private fun readRow(row: List<String>, words: MutableMap<String, Word>, warnings: MutableList<String>) {
    if (row.all { it.isEmpty() }) return
    val rawEn = row.getOrElse(0) { "" }
    if (rawEn.equals(HEADER, ignoreCase = true)) return
    val definition = row.getOrElse(1) { "" }
    val rawNl = row.getOrElse(2) { "" }

    val (en, type) = splitType(rawEn)
    if (en.isEmpty()) return
    if (type == null) warnings += "\"$en\": geen woordsoort herkend in \"$rawEn\"."

    val nl = splitVariants(rawNl)
    if (nl.isEmpty()) {
      warnings += "\"$en\" overgeslagen: geen Nederlandse vertaling."
      return
    }
    if (definition.isEmpty()) warnings += "\"$en\": geen uitleg."

    val id = makeId(en, type)
    if (words.containsKey(id)) {
      warnings += "\"$en\" staat dubbel in de lijst; eerste vermelding behouden."
      return
    }
    words[id] = Word(id = id, en = en, type = type, definition = definition, nl = nl)
  }

  /** "to aspire (to)(v)" → ("to aspire (to)", "v"). */
  internal fun splitType(raw: String): Pair<String, String?> {
    val text = raw.trim()
    val match = trailingGroup.find(text) ?: return text to null
    val type = match.groupValues[1].trim().lowercase()
    if (type !in knownTypes) return text to null
    return text.substring(0, match.range.first).trim() to type
  }

  /** Splitst op komma, puntkomma en schuine streep, maar niet binnen haakjes. */
  internal fun splitVariants(raw: String): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var depth = 0
    for (ch in raw) {
      when {
        ch == '(' -> { depth++; current.append(ch) }
        ch == ')' -> { depth = maxOf(0, depth - 1); current.append(ch) }
        depth == 0 && (ch == ',' || ch == ';' || ch == '/') -> {
          result += current.toString(); current.clear()
        }
        else -> current.append(ch)
      }
    }
    result += current.toString()
    return result.map { it.replace(spaces, " ").trim() }.filter { it.isNotEmpty() }.distinct()
  }

  internal fun makeId(en: String, type: String?): String {
    val base = Normalizer.normalize("$en ${type.orEmpty()}", Normalizer.Form.NFD)
      .replace(Regex("\\p{Mn}+"), "")
      .lowercase()
      .replace(Regex("[^a-z0-9]+"), "_")
      .trim('_')
    return base.ifEmpty { "woord" }
  }

  // --- docx / xml ------------------------------------------------------------------------------

  private fun readDocumentXml(input: InputStream): ByteArray {
    ZipInputStream(input).use { zip ->
      var entry = zip.nextEntry
      while (entry != null) {
        if (entry.name == "word/document.xml") {
          val out = ByteArrayOutputStream()
          val buffer = ByteArray(8192)
          while (true) {
            val n = zip.read(buffer)
            if (n < 0) break
            out.write(buffer, 0, n)
            if (out.size() > MAX_XML_BYTES) throw ImportException("Bestand is te groot.")
          }
          return out.toByteArray()
        }
        entry = zip.nextEntry
      }
    }
    throw ImportException("Dit is geen geldig Word-bestand (.docx).")
  }

  /** Alle tabellen op het hoogste niveau: lijst van rijen, elke rij een lijst celteksten. */
  private fun readTables(xml: ByteArray): List<List<List<String>>> {
    val handler = TableHandler()
    try {
      val factory = SAXParserFactory.newInstance()
      factory.isNamespaceAware = false
      try {
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
      } catch (_: Exception) {
        // niet op elk platform ondersteund
      }
      factory.newSAXParser().parse(InputSource(ByteArrayInputStream(xml)), handler)
    } catch (e: ImportException) {
      throw e
    } catch (e: Exception) {
      throw ImportException("Het Word-bestand kon niet gelezen worden.")
    }
    return handler.tables
  }

  private class TableHandler : DefaultHandler() {
    val tables = mutableListOf<List<List<String>>>()

    private var tableDepth = 0
    private var rows: MutableList<List<String>>? = null
    private var cells: MutableList<String>? = null
    private var cell: StringBuilder? = null
    private var inText = false

    override fun startElement(uri: String?, localName: String?, qName: String, attrs: Attributes?) {
      when (qName) {
        "w:tbl" -> {
          tableDepth++
          if (tableDepth == 1) rows = mutableListOf()
        }
        "w:tr" -> if (tableDepth == 1) cells = mutableListOf()
        "w:tc" -> if (tableDepth == 1) cell = StringBuilder()
        "w:t" -> inText = true
        "w:tab", "w:br" -> cell?.append(' ')
      }
    }

    override fun endElement(uri: String?, localName: String?, qName: String) {
      when (qName) {
        "w:t" -> inText = false
        "w:p" -> cell?.append(' ')
        "w:tc" -> if (tableDepth == 1) {
          cells?.add(clean(cell.toString()))
          cell = null
        }
        "w:tr" -> if (tableDepth == 1) {
          cells?.let { rows?.add(it) }
          cells = null
        }
        "w:tbl" -> {
          if (tableDepth == 1) {
            rows?.let { tables += it }
            rows = null
          }
          tableDepth--
        }
      }
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
      if (inText) cell?.append(ch, start, length)
    }

    private fun clean(s: String) = s.replace(spaces, " ").trim()
  }
}
