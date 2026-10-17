package com.robpolus.commskills.data

import android.content.res.AssetManager
import com.robpolus.commskills.domain.AppData
import com.robpolus.commskills.importer.DocxListImporter
import com.robpolus.commskills.importer.ListMerge

/** De lijsten uit `assets/lists/` worden bij de eerste start (en bij nieuwe bestanden) ingelezen. */
object BundledLists {

  private val titles = mapOf(
    "0_testlijst.docx" to "Testlijst",
    "academic_writing.docx" to "Academic writing",
    "applyingforajob.docx" to "Applying for a job",
    "business_writing.docx" to "Business writing",
    "ethicaldilemmas.docx" to "Ethical dilemmas",
    "moralforeignlanguageeffect.docx" to "Moral foreign language effect",
    "stanfordclass.docx" to "Stanford class",
    "trolleyproblem.docx" to "Trolley problem",
  )

  fun seed(assets: AssetManager, data: AppData): AppData {
    var result = data
    val files = assets.list("lists").orEmpty().filter { it.endsWith(".docx") }.sorted()
    for (file in files) {
      if (file in result.seededAssets) continue
      try {
        val title = titles[file] ?: DocxListImporter.titleFromFileName(file)
        val imported = assets.open("lists/$file").use { DocxListImporter.parse(it, title) }
        result = ListMerge.apply(result, imported)
      } catch (_: Exception) {
        // Een kapot bestand mag de app niet blokkeren; het wordt niet als ingelezen gemarkeerd.
        continue
      }
      result = result.copy(seededAssets = result.seededAssets + file)
    }
    return result
  }
}
