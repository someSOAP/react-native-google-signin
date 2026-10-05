import XCTest

final class AuthUITests: XCTestCase {
  func testCancellationFailureRetryAndLogout() {
    let app = XCUIApplication()
    app.launchArguments = ["--google-signin-e2e"]
    app.launch()
    XCTAssertTrue(app.staticTexts["Simulated provider UI verification"].waitForExistence(timeout: 30))
    func tap(_ id: String) { app.buttons[id].tap() }
    func status(_ text: String) { XCTAssertTrue(app.staticTexts[text].waitForExistence(timeout: 10)) }
    tap("scenario-cancel"); tap("sign-in"); status("Cancelled; ready to retry")
    tap("scenario-success"); tap("sign-in"); status("Google credentials received")
    tap("scenario-failure"); tap("sign-in"); status("NETWORK_ERROR")
    tap("scenario-success"); tap("sign-in"); status("Google credentials received")
    tap("scenario-logoutFailure"); tap("sign-out"); status("SIGN_OUT_ERROR")
    tap("scenario-success"); tap("sign-out"); status("Google provider signed out")
    tap("sign-in"); status("Google credentials received")
  }
}
