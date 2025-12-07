package com.someosap.reactnativegooglesignin

import android.content.Intent
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

  override fun getGoogleSignInToken(configs: ReadableMap, promise: Promise) {
    val activity = reactApplicationContext.currentActivity

    if(activity == null) {
      promise.reject("A", "cant call signIn because currentActivity is null")
      return
    }

    manager.getGoogleSignInToken(activity, configs, promise)
  }

  companion object {
    const val NAME = "ReactNativeGoogleSignin"
  }
}
