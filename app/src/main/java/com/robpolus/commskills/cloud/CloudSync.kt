package com.robpolus.commskills.cloud

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.robpolus.commskills.data.AppRepository
import com.robpolus.commskills.data.BundledLists
import com.robpolus.commskills.data.Codec
import com.robpolus.commskills.domain.AppData
import com.robpolus.commskills.domain.CloudMerge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Google-login en online opslag via Firebase. Alles is optioneel: zonder `google-services.json`
 * is [configured] onwaar en werkt de app volledig lokaal.
 */
class CloudSync(
  private val context: Context,
  private val repository: AppRepository,
  private val scope: CoroutineScope,
) {
  private val prefs = context.getSharedPreferences("cloud", Context.MODE_PRIVATE)

  val configured: Boolean get() = FirebaseApp.getApps(context).isNotEmpty()

  val email: String? get() = if (configured) FirebaseAuth.getInstance().currentUser?.email else null

  val signedIn: Boolean get() = configured && FirebaseAuth.getInstance().currentUser != null

  /** Of het inlogscherm getoond moet worden: ingesteld, niet ingelogd en niet bewust lokaal gekozen. */
  fun needsLogin(): Boolean = configured && !signedIn && !prefs.getBoolean(KEY_LOCAL_ONLY, false)

  fun chooseLocalOnly() {
    prefs.edit().putBoolean(KEY_LOCAL_ONLY, true).apply()
  }

  /** Inloggen met Google; geeft `true` bij succes. */
  suspend fun signIn(activity: Activity): Boolean {
    if (!configured) return false
    val clientId = activity.resources.let {
      val id = it.getIdentifier("default_web_client_id", "string", activity.packageName)
      if (id == 0) return false else it.getString(id)
    }
    val option = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(false)
      .setServerClientId(clientId)
      .build()
    val result = CredentialManager.create(activity)
      .getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(option).build())
    val token = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
    FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
    prefs.edit().putBoolean(KEY_LOCAL_ONLY, false).apply()
    start()
    return true
  }

  suspend fun signOut(context: Context) {
    if (!configured) return
    onSavedJob?.cancel()
    repository.onSaved = {}
    FirebaseAuth.getInstance().signOut()
    runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
  }

  /** Haalt de cloud-gegevens op, beslist wie wint en zet daarna het automatisch wegschrijven aan. */
  suspend fun start() {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    try {
      val doc = FirebaseFirestore.getInstance().document("users/$uid/data/state").get().await()
      val cloud = doc.getString("json")?.let { runCatching { Codec.decode(it) }.getOrNull() }
      when (CloudMerge.decide(repository.data.value, cloud)) {
        CloudMerge.Decision.USE_CLOUD -> {
          repository.replace(BundledLists.seed(context.assets, cloud!!))
        }
        CloudMerge.Decision.PUSH_LOCAL -> push(uid, repository.data.value)
        CloudMerge.Decision.NOTHING -> {}
      }
    } catch (e: Exception) {
      // Offline: later opnieuw proberen bij de volgende wijziging.
    }
    repository.onSaved = { scheduleUpload(uid) }
  }

  private var onSavedJob: Job? = null

  /** Bundelt wijzigingen: pas 5 seconden na de laatste wijziging wordt geüpload. */
  private fun scheduleUpload(uid: String) {
    onSavedJob?.cancel()
    onSavedJob = scope.launch {
      delay(UPLOAD_DELAY_MS)
      push(uid, repository.data.value)
    }
  }

  /** Meteen uploaden (bij het verlaten van de app). */
  fun flush() {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    onSavedJob?.cancel()
    scope.launch { push(uid, repository.data.value) }
  }

  private suspend fun push(uid: String, data: AppData) {
    val json = Codec.encode(data)
    if (json.length > MAX_DOC_CHARS) return
    try {
      FirebaseFirestore.getInstance().document("users/$uid/data/state")
        .set(mapOf("json" to json, "updatedAt" to data.updatedAt)).await()
    } catch (e: Exception) {
      // Offline: Firestore bewaart de schrijfactie niet zonder verbinding; volgende wijziging probeert opnieuw.
    }
  }

  private companion object {
    const val KEY_LOCAL_ONLY = "localOnly"
    const val UPLOAD_DELAY_MS = 5_000L
    const val MAX_DOC_CHARS = 900_000
  }
}
