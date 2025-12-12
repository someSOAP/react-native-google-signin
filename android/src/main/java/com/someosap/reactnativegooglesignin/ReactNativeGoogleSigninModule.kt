package com.somesoap.reactnativegooglesignin

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap


@ReactModule(name = ReactNativeGoogleSigninModule.NAME)
class ReactNativeGoogleSigninModule(reactContext: ReactApplicationContext) :
  NativeReactNativeGoogleSigninSpec(reactContext) {

  val manager = GoogleSignInManager(reactContext)

  override fun getName(): String {
    return NAME
  }

  override fun getGoogleCredentials(configs: ReadableMap, promise: Promise) {
    val activity = reactApplicationContext.currentActivity

    if(activity == null) {
      promise.reject("ERR_ACTIVITY", "Can't call getGoogleCredentials because currentActivity is null")
      return
    }


    val serverClientId = configs.getString("serverClientId")
    val nonce = configs.getString("nonce")

    if(serverClientId == null) {
      promise.reject("ERROR", "serverClientId must be string")
      return
    }

    manager.getGoogleCredentials(
      activity = activity,
      serverClientId = serverClientId,
      nonce = nonce,
      promise = promise,
    )
  }

  companion object {
    const val NAME = "ReactNativeGoogleSignin"
  }
}
