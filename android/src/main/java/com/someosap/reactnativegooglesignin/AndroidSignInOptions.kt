package com.somesoap.reactnativegooglesignin

import com.facebook.react.bridge.ReadableMap

internal data class AndroidSignInOptions(
  val flow: String = "button",
  val filterByAuthorizedAccounts: Boolean? = null,
  val autoSelect: Boolean? = null,
  val hostedDomain: String? = null
) {
  fun isValid(): Boolean =
    (flow == "button" || flow == "bottomSheet") &&
      (flow == "bottomSheet" || (filterByAuthorizedAccounts == null && autoSelect == null)) &&
      (hostedDomain == null || (hostedDomain.length <= 253 && hostedDomain.contains('.') &&
        hostedDomain.split('.').all { DOMAIN_LABEL.matches(it) }))

  companion object {
    private val DOMAIN_LABEL = Regex("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?$")

    // Copy bridge values before posting to main; reject null and incorrect types.
    fun fromMap(map: ReadableMap): AndroidSignInOptions {
      require(map.hasKey("flow") && !map.isNull("flow"))
      fun optionalBoolean(key: String): Boolean? {
        if (!map.hasKey(key)) return null
        require(!map.isNull(key))
        return map.getBoolean(key)
      }
      val domain = if (map.hasKey("hostedDomain")) {
        require(!map.isNull("hostedDomain"))
        requireNotNull(map.getString("hostedDomain"))
      } else null
      return AndroidSignInOptions(requireNotNull(map.getString("flow")),
        optionalBoolean("filterByAuthorizedAccounts"), optionalBoolean("autoSelect"), domain)
    }
  }
}
