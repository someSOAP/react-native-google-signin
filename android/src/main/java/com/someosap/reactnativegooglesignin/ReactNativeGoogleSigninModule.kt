package com.somesoap.reactnativegooglesignin

import android.os.Handler
import android.os.Looper
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule

@ReactModule(name = ReactNativeGoogleSigninModule.NAME)
class ReactNativeGoogleSigninModule(reactContext: ReactApplicationContext) :
  NativeReactNativeGoogleSigninSpec(reactContext), LifecycleEventListener {
  private val main = Handler(Looper.getMainLooper())
  private val manager = GoogleSignInManager(AndroidGoogleCredentialProvider(reactContext))
  init { reactContext.addLifecycleEventListener(this) }
  override fun getName() = NAME

  private fun callback(promise: Promise) = object : AuthCallback {
    override fun resolve(value: Map<String, String>?) {
      promise.resolve(value?.let { map -> Arguments.createMap().apply {
        map.forEach { (key, item) -> putString(key, item) }
      } })
    }
    override fun reject(code: String) { promise.reject(code, "Google authentication failed ($code).") }
  }
  override fun getGoogleCredentials(configs: ReadableMap, promise: Promise) {
    // Read the map before posting: no borrowed JS arguments escape the invocation.
    val serverClientId: String?
    val nonce: String?
    try {
      serverClientId = configs.getString("serverClientId")
      nonce = if (configs.hasKey("nonce") && !configs.isNull("nonce")) configs.getString("nonce") else null
    } catch (_: Exception) {
      promise.reject("CONFIGURATION_ERROR", "Expected string client ID and nonce."); return
    }
    main.post { manager.getGoogleCredentials(reactApplicationContext.currentActivity, serverClientId, nonce, callback(promise)) }
  }
  override fun signOut(promise: Promise) { main.post { manager.signOut(callback(promise)) } }
  override fun onHostResume() = Unit
  override fun onHostPause() = Unit // The provider's interactive UI can pause our Activity.
  override fun onHostDestroy() { main.post { manager.cancelForDestroyedActivity() } }
  override fun invalidate() {
    reactApplicationContext.removeLifecycleEventListener(this)
    main.post { manager.invalidate() }
    super.invalidate()
  }
  companion object { const val NAME = "ReactNativeGoogleSignin" }
}
