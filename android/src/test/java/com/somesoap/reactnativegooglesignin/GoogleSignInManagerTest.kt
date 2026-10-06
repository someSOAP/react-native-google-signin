package com.somesoap.reactnativegooglesignin

import android.app.Activity
import android.os.CancellationSignal
import android.os.Looper
import androidx.credentials.*
import androidx.credentials.exceptions.*
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.ClearCredentialUnknownException
import com.google.android.libraries.identity.googleid.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class GoogleSignInManagerTest {
  private val client = "123-test.apps.googleusercontent.com"
  private val token = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiIxMjMifQ.c2ln"
  private lateinit var provider: FakeProvider
  private lateinit var manager: GoogleSignInManager
  private lateinit var activity: Activity
  @Before fun setup() {
    provider = FakeProvider(); manager = GoogleSignInManager(provider)
    activity = Robolectric.buildActivity(Activity::class.java).setup().get()
  }
  private class Result : AuthCallback {
    var settles = 0; var code: String? = null; var value: Map<String, String>? = null
    override fun resolve(value: Map<String, String>?) { settles++; this.value = value }
    override fun reject(code: String) { settles++; this.code = code }
  }
  private class FakeProvider : GoogleCredentialProvider {
    var isAvailable = true; var gets = 0; var clears = 0
    var thrown: Exception? = null
    lateinit var request: GetCredentialRequest
    lateinit var signal: CancellationSignal
    lateinit var getCallback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>
    lateinit var clearCallback: CredentialManagerCallback<Void?, ClearCredentialException>
    override fun available() = isAvailable
    override fun get(activity: Activity, request: GetCredentialRequest, signal: CancellationSignal,
                     callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>) {
      thrown?.let { throw it }; gets++; this.request = request; this.signal = signal; getCallback = callback
    }
    override fun clear(signal: CancellationSignal, callback: CredentialManagerCallback<Void?, ClearCredentialException>) {
      thrown?.let { throw it }; clears++; this.signal = signal; clearCallback = callback
    }
  }
  private fun start(nonce: String? = null, options: AndroidSignInOptions = AndroidSignInOptions()): Result = Result().also {
    manager.getGoogleCredentials(activity, client, nonce, it, options)
  }
  private fun success(tokenValue: String = token) {
    // Construct the real provider bundle, then vary the token at the SDK boundary.
    // 1.2.1 rejects malformed tokens in its builder as well as createFrom().
    val data = GoogleIdTokenCredential.Builder().setId("test@example.com").setIdToken(token).build().data
    val tokenKey = data.keySet().single { data.get(it) == token }
    data.putString(tokenKey, tokenValue)
    provider.getCallback.onResult(GetCredentialResponse(CustomCredential(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, data)))
  }
  @Test fun buttonOptionForFirstAndReturningUsersForwardsClientAndNonce() {
    val result = start("caller nonce")
    val option = provider.request.credentialOptions.single() as GetSignInWithGoogleOption
    assertEquals(client, option.serverClientId); assertEquals("caller nonce", option.nonce)
    success(); assertEquals(token, result.value!!["idToken"])
    val returning = start(); success(); assertEquals(1, returning.settles); assertEquals(2, provider.gets)
  }
  @Test fun optionalProfileDoesNotBecomeNullString() {
    val result = start(); success()
    assertFalse(result.value!!.containsKey("profilePictureUri")); assertFalse(result.value!!.containsKey("givenName"))
    assertFalse(result.value!!.containsKey("email")) // No email claim; legacy SDK ID is not an email.
  }
  @Test fun bottomSheetDefaultsAndExplicitSettingsReachGoogleOption() {
    var result = start("caller nonce", AndroidSignInOptions(flow = "bottomSheet"))
    var option = provider.request.credentialOptions.single() as GetGoogleIdOption
    assertEquals(client, option.serverClientId); assertEquals("caller nonce", option.nonce)
    assertTrue(option.filterByAuthorizedAccounts); assertFalse(option.autoSelectEnabled)
    success(); assertEquals(1, result.settles)
    for (filter in listOf(false, true)) for (auto in listOf(false, true)) {
      result = start(options = AndroidSignInOptions("bottomSheet", filter, auto, "example.com"))
      option = provider.request.credentialOptions.single() as GetGoogleIdOption
      assertEquals(filter, option.filterByAuthorizedAccounts); assertEquals(auto, option.autoSelectEnabled)
      assertEquals("example.com", option.hostedDomainFilter)
      success(); assertEquals(1, result.settles)
    }
  }
  @Test fun buttonHostedDomainDoesNotChangeFlow() {
    start(options = AndroidSignInOptions(hostedDomain = "example.com"))
    val option = provider.request.credentialOptions.single() as GetSignInWithGoogleOption
    assertEquals("example.com", option.hostedDomainFilter)
  }
  @Test fun invalidOptionsRejectBeforeProviderWorkAndPermitRetry() {
    for (options in listOf(
      AndroidSignInOptions(flow = "unknown"),
      AndroidSignInOptions(autoSelect = false),
      AndroidSignInOptions(filterByAuthorizedAccounts = true),
      AndroidSignInOptions(hostedDomain = ""),
      AndroidSignInOptions(hostedDomain = "https://example.com"),
      AndroidSignInOptions(hostedDomain = "*.example.com"),
      AndroidSignInOptions(hostedDomain = "example..com")
    )) {
      assertEquals("CONFIGURATION_ERROR", start(options = options).code)
    }
    assertEquals(0, provider.gets)
    val retry = start(); success(); assertEquals(1, retry.settles)
  }
  @Test fun bottomSheetCancellationAndNoCredentialsNeverFallBack() {
    for (error in listOf(GetCredentialCancellationException(), NoCredentialException())) {
      val result = start(options = AndroidSignInOptions(flow = "bottomSheet"))
      val calls = provider.gets; val old = provider.getCallback
      old.onError(error)
      assertEquals(if (error is GetCredentialCancellationException) "CANCELLATION_ERROR" else "NO_CREDENTIALS_ERROR", result.code)
      assertEquals(calls, provider.gets); assertEquals(1, result.settles)
      val retry = start(); old.onError(error); success()
      assertEquals(1, retry.settles); assertEquals(1, result.settles)
    }
  }
  @Test fun bridgeOptionsAreCopiedAndTypesAreChecked() {
    val map = com.facebook.react.bridge.JavaOnlyMap.of("flow", "bottomSheet", "autoSelect", false,
      "filterByAuthorizedAccounts", false, "hostedDomain", "example.com")
    val options = AndroidSignInOptions.fromMap(map)
    map.putString("flow", "button")
    assertEquals(AndroidSignInOptions("bottomSheet", false, false, "example.com"), options)
    for (invalid in listOf(
      com.facebook.react.bridge.JavaOnlyMap.of("flow", null),
      com.facebook.react.bridge.JavaOnlyMap.of("flow", 123),
      com.facebook.react.bridge.JavaOnlyMap.of("flow", "bottomSheet", "autoSelect", "false"),
      com.facebook.react.bridge.JavaOnlyMap.of("flow", "bottomSheet", "autoSelect", null),
      com.facebook.react.bridge.JavaOnlyMap.of("flow", "button", "hostedDomain", false)
    )) {
      assertThrows(Exception::class.java) { AndroidSignInOptions.fromMap(invalid) }
    }
  }
  @Test fun emailComesFromTokenRatherThanLegacyIdentifier() {
    val payload = android.util.Base64.encodeToString(
      "{\"sub\":\"123\",\"email\":\"test@example.com\"}".toByteArray(),
      android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING)
    val result = start()
    success("eyJhbGciOiJSUzI1NiJ9.$payload.c2ln")
    assertEquals("test@example.com", result.value!!["email"])
  }
  @Test fun fullProfileMapped() {
    val result = start()
    val credential = GoogleIdTokenCredential.Builder().setId("test@example.com").setIdToken(token)
      .setGivenName("Test").setFamilyName("User").setProfilePictureUri(android.net.Uri.parse("https://example.com/photo")).build()
    provider.getCallback.onResult(GetCredentialResponse(credential))
    assertEquals("Test", result.value!!["givenName"]); assertEquals("User", result.value!!["familyName"])
    assertEquals("https://example.com/photo", result.value!!["profilePictureUri"])
  }
  @Test fun missingActivityAndConfigNeverLaunchUI() {
    for (id in listOf(null, "", " ", "not-a-client")) {
      val result = Result(); manager.getGoogleCredentials(activity, id, null, result)
      assertEquals("CONFIGURATION_ERROR", result.code)
    }
    val result = Result(); manager.getGoogleCredentials(null, client, null, result)
    assertEquals("ERR_ACTIVITY", result.code); assertEquals(0, provider.gets)
    assertEquals("CONFIGURATION_ERROR", start(" ").code)
  }
  @Test fun unavailablePlayServices() { provider.isAvailable = false; assertEquals("PROVIDER_UNAVAILABLE", start().code) }
  @Test fun providerFailuresSettleAndAllowRetryWithoutFallback() {
    val cases = listOf(
      GetCredentialCancellationException() to "CANCELLATION_ERROR",
      NoCredentialException() to "NO_CREDENTIALS_ERROR",
      GetCredentialProviderConfigurationException() to "PROVIDER_UNAVAILABLE",
      GetCredentialUnsupportedException() to "PROVIDER_UNAVAILABLE",
      GetCredentialUnknownException("network or reauthentication response") to "GET_CREDENTIALS_ERROR")
    for ((exception, code) in cases) {
      val result = start(); val calls = provider.gets; provider.getCallback.onError(exception)
      assertEquals(code, result.code); assertEquals(calls, provider.gets); assertEquals(1, result.settles)
      val retry = start(); success(); assertEquals(1, retry.settles)
    }
  }
  @Test fun unknownCustomAndNonGoogleTypesAlwaysReject() {
    for (credential in listOf(CustomCredential("unexpected", android.os.Bundle()), PasswordCredential("id", "password"))) {
      val result = start(); provider.getCallback.onResult(GetCredentialResponse(credential))
      assertEquals("UNKNOWN_CREDENTIALS_TYPE", result.code); assertEquals(1, result.settles)
    }
  }
  @Test fun parsingFailureAndMalformedToken() {
    val parsing = start()
    provider.getCallback.onResult(GetCredentialResponse(CustomCredential(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, android.os.Bundle())))
    assertEquals("INVALID_TOKEN_ERROR", parsing.code)
    for (value in listOf(" ", "malformed", "a.b.c", "e30.e30.c2ln")) {
      val result = start(); success(value); assertEquals("INVALID_TOKEN_ERROR", result.code)
    }
    assertFalse(GoogleSignInManager.validToken(""))
  }
  @Test fun concurrentCallsIncludingLogoutRejectSecond() {
    val first = start(); assertEquals("IN_PROGRESS", start().code)
    val logout = Result(); manager.signOut(logout); assertEquals("IN_PROGRESS", logout.code)
    success(); assertEquals(1, first.settles)
  }
  @Test fun duplicateAndStaleCallbacksCannotSettleNewRequest() {
    val first = start(); val old = provider.getCallback; success(); old.onError(GetCredentialCancellationException())
    assertEquals(1, first.settles)
    val second = start(); old.onResult(GetCredentialResponse(GoogleIdTokenCredential.Builder().setId("test@example.com").setIdToken(token).build()))
    assertEquals(0, second.settles); success(); assertEquals(1, second.settles)
  }
  @Test fun teardownCancelsAndIgnoresLateCallback() {
    val result = start(); manager.invalidate()
    assertTrue(provider.signal.isCanceled); assertEquals("MODULE_DESTROYED", result.code)
    success(); assertEquals(1, result.settles); assertEquals("MODULE_DESTROYED", start().code)
  }
  @Test fun activityDestroyedAllowsNewHostRetry() {
    val result = start(); manager.cancelForDestroyedActivity()
    assertEquals("ERR_ACTIVITY", result.code); assertTrue(provider.signal.isCanceled)
    val retry = start(); success(); assertEquals(1, retry.settles)
  }
  @Test fun logoutSuccessFailureAndNextSignIn() {
    val logout = Result(); manager.signOut(logout); assertEquals("IN_PROGRESS", start().code)
    provider.clearCallback.onResult(null); provider.clearCallback.onError(ClearCredentialUnknownException())
    assertEquals(1, logout.settles); assertNull(logout.code)
    val failed = Result(); manager.signOut(failed); provider.clearCallback.onError(ClearCredentialUnknownException())
    assertEquals("SIGN_OUT_ERROR", failed.code)
    val retry = start(); success(); assertEquals(1, retry.settles)
  }
  @Test fun synchronousBoundaryExceptionsDoNotHang() {
    provider.thrown = IllegalStateException("sensitive provider text")
    assertEquals("GET_CREDENTIALS_ERROR", start().code)
    val logout = Result(); manager.signOut(logout); assertEquals("SIGN_OUT_ERROR", logout.code)
    provider.thrown = null; val retry = start(); success(); assertEquals(1, retry.settles)
  }
  @Test fun synchronousCancellationIsNotGenericOrRetried() {
    provider.thrown = GetCredentialCancellationException()
    val cancelled = start(); assertEquals("CANCELLATION_ERROR", cancelled.code); assertEquals(1, cancelled.settles)
    provider.thrown = null; val retry = start(); success(); assertEquals(1, retry.settles)
  }
  @Test fun logoutProviderUnavailableIsClassified() {
    for (error in listOf(ClearCredentialProviderConfigurationException(), ClearCredentialUnsupportedException())) {
      val logout = Result(); manager.signOut(logout); provider.clearCallback.onError(error)
      assertEquals("PROVIDER_UNAVAILABLE", logout.code)
    }
    provider.isAvailable = false
    val missing = Result(); manager.signOut(missing); assertEquals("PROVIDER_UNAVAILABLE", missing.code)
  }
  @Test fun providerConfigurationExceptionSettlesAndAllowsRetry() {
    provider.thrown = IllegalArgumentException("SDK configuration error")
    assertEquals("CONFIGURATION_ERROR", start().code)
    provider.thrown = null; val retry = start(); success(); assertEquals(1, retry.settles)
  }
  @Test fun finishingAndDestroyedActivityCannotLaunchUI() {
    activity.finish(); assertEquals("ERR_ACTIVITY", start().code)
    val controller = Robolectric.buildActivity(Activity::class.java).setup()
    activity = controller.get(); controller.pause().stop().destroy()
    assertEquals("ERR_ACTIVITY", start().code); assertEquals(0, provider.gets)
  }
  @Test fun providerNeverCallsBackHasBoundedTimeout() {
    val result = start(); shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(5))
    assertEquals("TIMEOUT_ERROR", result.code); assertTrue(provider.signal.isCanceled)
    success(); assertEquals(1, result.settles)
    val retry = start(); success(); assertEquals(1, retry.settles)
  }
}
