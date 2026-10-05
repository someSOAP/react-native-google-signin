package somesoap.reactnativegooglesignin.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthUiTest {
  private lateinit var device: UiDevice
  @Before fun launch() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    device = UiDevice.getInstance(instrumentation)
    val context = instrumentation.targetContext
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)!!
    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    assertTrue("Build with -PgoogleSigninE2E=true and run Metro", device.wait(Until.hasObject(By.text("Simulated provider UI verification")), 30000))
  }
  private fun tap(id: String) {
    val view = device.wait(Until.findObject(By.res(id)), 10000)
    assertNotNull("Missing $id", view); view.click()
  }
  private fun status(text: String) { assertTrue("Expected $text", device.wait(Until.hasObject(By.text(text)), 10000)) }
  @Test fun cancellationFailureRetryAndLogout() {
    tap("scenario-cancel"); tap("sign-in"); status("Cancelled; ready to retry")
    tap("scenario-success"); tap("sign-in"); status("Google credentials received")
    tap("scenario-failure"); tap("sign-in"); status("NETWORK_ERROR")
    tap("scenario-success"); tap("sign-in"); status("Google credentials received")
    tap("scenario-logoutFailure"); tap("sign-out"); status("SIGN_OUT_ERROR")
    tap("scenario-success"); tap("sign-out"); status("Google provider signed out")
    tap("sign-in"); status("Google credentials received")
  }
}
