package com.somesoap.reactnativegooglesignin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GoogleSignInManager(
  private val context: Context
) {

  companion object {
    const val CANCELLATION_ERROR = "CANCELLATION_ERROR"
  }

  private fun getCredentialOption(
    serverClientId: String,
    nonce: String?
  ): CredentialOption {

    val builder = GetGoogleIdOption.Builder()

    return builder.setServerClientId(serverClientId)
//      .setAutoSelectEnabled(autoSelectEnabled)
//      .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
//      .setRequestVerifiedPhoneNumber(requestVerifiedPhoneNumber)
      .setNonce(nonce)
      .build()
  }

  fun getGoogleCredentials(
    activity: Activity,
    serverClientId: String,
    nonce: String?,
    promise: Promise
  ) {

    val manager = CredentialManager.create(activity)

    val creds = getCredentialOption(
      serverClientId,
      nonce,
    )

    val request = GetCredentialRequest.Builder()
      .addCredentialOption(creds)
      .build()

    CoroutineScope(Dispatchers.Default).launch {
      try {
        val result = manager.getCredential(activity, request)

        when (result.credential) {
          is CustomCredential -> {
            if(result.credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
              val googleCreds = GoogleIdTokenCredential.createFrom(result.credential.data)
              val response = Arguments.createMap()

              response.putString("idToken", googleCreds.idToken)
              response.putString("givenName", googleCreds.givenName)
              response.putString("familyName", googleCreds.familyName)
              response.putString("email", googleCreds.id)
              response.putString("profilePictureUri", googleCreds.profilePictureUri.toString())

              promise.resolve(response)
            }
          }
          else -> {
            promise.reject("UNKNOWN_CREDENTIALS_TYPE", "Unknown type of credentials")
          }
        }

      } catch (e: NoCredentialException) {
        promise.reject("NO_CREDENTIALS_ERROR", e)
      } catch (e: GetCredentialCancellationException) {
        promise.reject(CANCELLATION_ERROR, e)
      } catch (e: GetCredentialException) {
        promise.reject("GET_CREDENTIALS_ERROR", e)
      }
    }
  }
}
