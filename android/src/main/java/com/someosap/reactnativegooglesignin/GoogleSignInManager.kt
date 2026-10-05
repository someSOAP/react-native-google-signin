package com.somesoap.reactnativegooglesignin

import android.app.Activity
import android.content.Context
import android.content.MutableContextWrapper
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.credentials.*
import androidx.credentials.exceptions.*
import androidx.credentials.exceptions.ClearCredentialException
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import org.json.JSONObject
import java.util.concurrent.Executor

internal interface AuthCallback {
  fun resolve(value: Map<String, String>?)
  fun reject(code: String)
}

/** Only the Google/platform boundary is replaceable in tests. */
internal interface GoogleCredentialProvider {
  fun available(): Boolean
  fun get(activity: Activity, request: GetCredentialRequest, signal: CancellationSignal,
          callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>)
  fun clear(signal: CancellationSignal,
            callback: CredentialManagerCallback<Void?, ClearCredentialException>)
}

internal class AndroidGoogleCredentialProvider(private val context: Context) : GoogleCredentialProvider {
  private val executor = Executor { Handler(Looper.getMainLooper()).post(it) }
  override fun available() = GoogleApiAvailability.getInstance()
    .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
  override fun get(activity: Activity, request: GetCredentialRequest, signal: CancellationSignal,
                   callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>) {
    CredentialManager.create(context).getCredentialAsync(
      MutableContextWrapper(activity), request, signal, executor, callback)
  }
  override fun clear(signal: CancellationSignal,
                     callback: CredentialManagerCallback<Void?, ClearCredentialException>) {
    CredentialManager.create(context).clearCredentialStateAsync(
      ClearCredentialStateRequest(), signal, executor, callback)
  }
}

internal class GoogleSignInManager(
  private val provider: GoogleCredentialProvider,
  private val handler: Handler = Handler(Looper.getMainLooper()),
  private val timeoutMs: Long = 300_000L
) {
  private class Pending(val callback: AuthCallback) {
    val signal = CancellationSignal()
    var timeout: Runnable? = null
  }
  private var pending: Pending? = null
  private var destroyed = false

  private fun begin(callback: AuthCallback): Pending? {
    if (destroyed) { callback.reject("MODULE_DESTROYED"); return null }
    if (pending != null) { callback.reject("IN_PROGRESS"); return null }
    val operation = Pending(callback)
    pending = operation
    operation.timeout = Runnable {
      finish(operation, code = "TIMEOUT_ERROR")
      operation.signal.cancel()
    }.also { handler.postDelayed(it, timeoutMs) }
    return operation
  }

  private fun finish(operation: Pending, result: Map<String, String>? = null, code: String? = null) {
    if (pending !== operation) return
    pending = null
    operation.timeout?.let { handler.removeCallbacks(it) }
    if (code != null) operation.callback.reject(code) else operation.callback.resolve(result)
  }

  fun getGoogleCredentials(activity: Activity?, serverClientId: String?, nonce: String?, callback: AuthCallback) {
    val operation = begin(callback) ?: return
    if (serverClientId == null || !CLIENT_ID.matches(serverClientId) ||
        (nonce != null && nonce.isBlank())) {
      finish(operation, code = "CONFIGURATION_ERROR"); return
    }
    if (activity == null || activity.isFinishing || activity.isDestroyed) {
      finish(operation, code = "ERR_ACTIVITY"); return
    }
    try {
      if (!provider.available()) { finish(operation, code = "PROVIDER_UNAVAILABLE"); return }
      val option = GetSignInWithGoogleOption.Builder(serverClientId).setNonce(nonce).build()
      val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
      provider.get(activity, request, operation.signal,
        object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
          override fun onResult(result: GetCredentialResponse) {
            if (pending !== operation) return
            val credential = result.credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
              finish(operation, code = "UNKNOWN_CREDENTIALS_TYPE"); return
            }
            try {
              val google = GoogleIdTokenCredential.createFrom(credential.data)
              if (!validToken(google.idToken)) {
                finish(operation, code = "INVALID_TOKEN_ERROR"); return
              }
              val response = mutableMapOf("idToken" to google.idToken, "email" to google.id)
              google.givenName?.let { response["givenName"] = it }
              google.familyName?.let { response["familyName"] = it }
              google.profilePictureUri?.let { response["profilePictureUri"] = it.toString() }
              finish(operation, response)
            } catch (_: Exception) { finish(operation, code = "INVALID_TOKEN_ERROR") }
          }
          override fun onError(e: GetCredentialException) {
            // A cancellation never launches a fallback or automatic retry.
            val code = credentialError(e)
            finish(operation, code = code)
          }
        })
    } catch (e: GetCredentialException) { finish(operation, code = credentialError(e))
    } catch (_: IllegalArgumentException) { finish(operation, code = "CONFIGURATION_ERROR")
    } catch (_: Exception) { finish(operation, code = "GET_CREDENTIALS_ERROR") }
  }

  fun signOut(callback: AuthCallback) {
    val operation = begin(callback) ?: return
    try {
      if (!provider.available()) { finish(operation, code = "PROVIDER_UNAVAILABLE"); return }
      provider.clear(operation.signal,
        object : CredentialManagerCallback<Void?, ClearCredentialException> {
          override fun onResult(result: Void?) { finish(operation) }
          override fun onError(e: ClearCredentialException) {
            finish(operation, code = if (e is ClearCredentialProviderConfigurationException ||
              e is ClearCredentialUnsupportedException) "PROVIDER_UNAVAILABLE" else "SIGN_OUT_ERROR")
          }
        })
    } catch (_: Exception) { finish(operation, code = "SIGN_OUT_ERROR") }
  }

  fun cancelForDestroyedActivity() { cancel("ERR_ACTIVITY") }
  fun invalidate() { destroyed = true; cancel("MODULE_DESTROYED") }
  private fun cancel(code: String) {
    val operation = pending ?: return
    finish(operation, code = code)
    operation.signal.cancel()
  }

  private fun credentialError(error: GetCredentialException) = when (error) {
    is GetCredentialCancellationException -> "CANCELLATION_ERROR"
    is NoCredentialException -> "NO_CREDENTIALS_ERROR"
    is GetCredentialProviderConfigurationException, is GetCredentialUnsupportedException -> "PROVIDER_UNAVAILABLE"
    else -> "GET_CREDENTIALS_ERROR"
  }

  companion object {
    private val CLIENT_ID = Regex("^[A-Za-z0-9_-]+\\.apps\\.googleusercontent\\.com$")
    // Structural screening only. The backend verifies signature, audience, expiry and nonce.
    internal fun validToken(token: String): Boolean = try {
      val parts = token.split('.')
      parts.size == 3 && parts.all { it.isNotEmpty() && it.matches(Regex("[A-Za-z0-9_-]+")) } &&
        JSONObject(String(Base64.decode(parts[0], Base64.URL_SAFE), Charsets.UTF_8)).length() > 0 &&
        JSONObject(String(Base64.decode(parts[1], Base64.URL_SAFE), Charsets.UTF_8)).length() > 0
    } catch (_: Exception) { false }
  }
}
