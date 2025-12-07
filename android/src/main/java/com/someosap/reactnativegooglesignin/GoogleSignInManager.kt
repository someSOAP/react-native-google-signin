package com.someosap.reactnativegooglesignin

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
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GoogleSignInManager(val context: Context) {
  private fun getCredentialOption(
    serverClientId: String,
    autoSelectEnabled: Boolean,
    filterByAuthorizedAccounts: Boolean,
    requestVerifiedPhoneNumber: Boolean,
    nonce: String?
  ): CredentialOption {

    val builder = GetGoogleIdOption.Builder()


    return builder.setServerClientId(serverClientId)
      .setAutoSelectEnabled(autoSelectEnabled)
      .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
      .setRequestVerifiedPhoneNumber(requestVerifiedPhoneNumber)
      .setNonce(nonce)
      .build()
  }


  fun getGoogleSignInToken(activity: Activity, configs: ReadableMap, promise: Promise) {
    val manager = CredentialManager.create(activity)

    val serverClientId = configs.getString("serverClientId")
    val autoSelectEnabled = if (configs.hasKey("autoSelectEnabled")) configs.getBoolean("autoSelectEnabled") else false
    val filterByAuthorizedAccounts = if (configs.hasKey("filterByAuthorizedAccounts")) configs.getBoolean("filterByAuthorizedAccounts") else false
    val requestVerifiedPhoneNumber = if (configs.hasKey("requestVerifiedPhoneNumber")) configs.getBoolean("requestVerifiedPhoneNumber") else false
    val nonce = configs.getString("nonce")

    if(serverClientId == null) {
      promise.reject("A", "serverClientId must be string")
      return
    }

    val creds = getCredentialOption(
      serverClientId,
      autoSelectEnabled,
      filterByAuthorizedAccounts,
      requestVerifiedPhoneNumber,
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
            if(result.credential.type === GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
              val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
              val googleTokenId = googleIdTokenCredential.idToken
              promise.resolve(googleTokenId)
            }
          }
          else -> {
            promise.reject("B", "FAIL SOME")
          }
        }

      } catch (e: NoCredentialException) {
        val intent = Intent(Settings.ACTION_ADD_ACCOUNT)
        intent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
        activity.startActivity(intent)
        promise.reject("B", e)

      } catch (e: GetCredentialCancellationException) {
        promise.reject("C", e)
      } catch (e: GetCredentialException) {
        promise.reject("D", e)
      } catch (e: Error) {
        promise.reject("E", e)
      }
    }

  }

}
