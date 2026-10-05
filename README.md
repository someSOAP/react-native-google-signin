# @somesoap/react-native-google-signin

A small Google credential bridge for Android and iOS. Call it from an explicit
Google sign-in button, exchange the ID token with your backend, and let that
backend own the app session. No Firebase dependency, custom token store,
refresh-token API, analytics, extra scopes, or automatic sign-in at startup.
Google's iOS SDK itself can store its provider state in Keychain.

```sh
yarn add @somesoap/react-native-google-signin
cd ios && pod install
```

Requires React Native's New Architecture / TurboModules. Native builds and
codegen were checked on **RN 0.81.1 and 0.86.3**, Android API 24+ and iOS at the
minimum required by your React Native version (15.1 for these versions).
This is evidence for those versions, not a guarantee for every intervening RN
release. GoogleSignIn iOS remains **9.x**; Android uses Credential Manager 1.5.0
and googleid 1.1.1 with its Play Services provider.

```tsx
import {
  getGoogleSignInToken,
  signOut,
  isGoogleSignInError,
  ErrorCodes,
} from '@somesoap/react-native-google-signin';

try {
  const credentials = await getGoogleSignInToken({
    serverClientId: 'YOUR_WEB_CLIENT_ID.apps.googleusercontent.com',
    iosClientId: 'YOUR_IOS_CLIENT_ID.apps.googleusercontent.com', // optional fallback below
    nonce: nonceFromYourBackend, // optional, forwarded unchanged
  });
  await exchangeWithYourBackend(credentials.idToken);
} catch (error) {
  if (isGoogleSignInError(error) && error.code === ErrorCodes.CANCELLATION_ERROR) {
    // Leave the button available. Retry only after another user action.
  }
}

// The consuming auth system owns its own logout; also clear Google provider state.
await signOut();
```

`getGoogleSignInToken(config): Promise<GetGoogleCredentialsResponse>` retains
its name and success shape: `idToken: string`, with optional `givenName`,
`familyName`, `email`, `profilePictureUri`. Missing optional values are omitted.
`signOut(): Promise<void>` is additive. Types, `GoogleSignInError`,
`GoogleSignInErrorCode`, and `isGoogleSignInError` are exported.

Each call requests interactive credentials, including for returning accounts.
Android uses `GetSignInWithGoogleOption`, which supports adding an account and
provider reauthentication. There is no bottom-sheet or silent-restoration flow,
so there is no fallback after cancellation. iOS uses the SDK's interactive flow
without extra scopes or an account hint. Google controls consent, account
selection, saved browser cookies, and reauthentication. To switch accounts,
clear provider state with `signOut()` before another explicit sign-in; iOS may
still offer accounts remembered by Google's browser.

Only one sign-in or sign-out may run per module. Overlapping requests reject
with `IN_PROGRESS`. Every accepted request settles once; duplicates and stale
callbacks are ignored. Activity destruction on Android cancels pending work;
module invalidation rejects pending work on both platforms. Backgrounding alone
does not cancel an external provider UI. A five-minute watchdog rejects missing
callbacks with `TIMEOUT_ERROR`. Android cancels the SDK operation and permits
retry. iOS has no public SDK cancellation API, so it remains `IN_PROGRESS` until
the old callback returns; restart the app if it never returns. Late results after
module teardown are discarded and iOS provider state is cleared.

The library screens missing and structurally malformed JWTs. **Your backend
must verify Google's signature, issuer, audience, expiry, and the expected
nonce** before creating an app session. Generate a fresh unpredictable nonce
per attempt, bind it to the backend attempt, and verify it there. This library
does not hash, persist, or verify nonce claims and does not log tokens or SDK
error descriptions. Never print credentials in your app.

## Android setup

1. Create a Web OAuth client for `serverClientId` in your Google Cloud project.
2. Register an Android OAuth client with your app's **actual application ID and
   signing SHA-1**, including debug and release / Play App Signing certificates.
3. Use a device with supported, enabled, up-to-date Google Play Services. The
   library checks availability before invoking Credential Manager.

`google-services.json` and the Google Services Gradle plugin are not required
by this library. An OAuth registration mismatch can still be rejected by Google
at runtime; local validation only verifies configuration shape.

See [Google's implementation guide](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation).

## iOS setup

Create an **iOS OAuth client matching the app bundle ID** and a Web OAuth client
for the server ID. Provide `iosClientId` per call, or add `GIDClientID` to the
app's `Info.plist`, or bundle `GoogleService-Info.plist` with `CLIENT_ID`.
The precedence is explicit argument, `GIDClientID`, then bundled Google plist.
`serverClientId` is always the per-call Web client ID.

Register the dot-reversed **iOS** client ID as a URL scheme in `Info.plist`:

```xml
<key>GIDClientID</key>
<string>YOUR_IOS_CLIENT_ID.apps.googleusercontent.com</string>
<key>CFBundleURLTypes</key>
<array><dict><key>CFBundleURLSchemes</key><array>
  <string>com.googleusercontent.apps.YOUR_IOS_CLIENT_ID</string>
</array></dict></array>
```

Forward callback URLs from the app delegate, preserving other URL handlers:

```swift
import GoogleSignIn

func application(_ app: UIApplication, open url: URL,
  options: [UIApplication.OpenURLOptionsKey: Any] = [:]) -> Bool {
  if GIDSignIn.sharedInstance.handle(url) { return true }
  return RCTLinkingManager.application(app, open: url, options: options)
}
```

Apps using scenes must also forward URLs received in
`scene(_:openURLContexts:)` to `GIDSignIn.sharedInstance.handle(context.url)`.
The library validates the URL scheme before presentation; it cannot inspect
whether your delegate actually forwards callbacks. It presents from an active
foreground scene's key window on the main thread. Use a foreground app with a
visible view controller. Sign physical devices with Keychain access enabled.
See [Google's setup](https://developers.google.com/identity/sign-in/ios/start-integrating)
and [SDK reference](https://developers.google.com/identity/sign-in/ios/reference/Classes/GIDSignIn).

## Errors and migration from 0.2.0 to 1.0.0

Failures reject with `GoogleSignInError` and a stable `.code`. Messages are
sanitized, so do not rely on raw provider descriptions.

| Code | Meaning |
| --- | --- |
| `CANCELLATION_ERROR` | Provider explicitly reports user cancellation; never automatically retried |
| `GET_CREDENTIALS_ERROR` | Other Google / Credential Manager failure, including Android network or reauthentication failures not distinguished by the SDK |
| `CONFIGURATION_ERROR` | Invalid client ID / nonce, or missing iOS callback scheme |
| `PROVIDER_UNAVAILABLE` | Missing / disabled / incompatible Play Services or credential provider (Android) |
| `NO_CREDENTIALS_ERROR` | Provider returned no usable credential; no second automatic UI flow |
| `UNKNOWN_CREDENTIALS_TYPE` | Unexpected Android credential type |
| `INVALID_TOKEN_ERROR` | Missing, malformed ID token, or parsing failure |
| `ERR_ACTIVITY` | No usable Android activity / iOS foreground presenter, or Android host destroyed |
| `NETWORK_ERROR` | iOS SDK returned `NSURLErrorDomain` |
| `IN_PROGRESS` | Another provider operation is active |
| `MODULE_DESTROYED` | Module invalidated; pending request discarded |
| `SIGN_OUT_ERROR` | Provider cleanup failed |
| `TIMEOUT_ERROR` | Provider did not complete within five minutes |

Existing `GET_CREDENTIALS_ERROR` and `CANCELLATION_ERROR` values are preserved;
previously unexported Android `NO_CREDENTIALS_ERROR`, `UNKNOWN_CREDENTIALS_TYPE`
and `ERR_ACTIVITY` are now exported. Previous generic `ERROR` configuration
failures become `CONFIGURATION_ERROR`. Android now shows the explicit button
flow instead of the authorized-account-only bottom sheet. Concurrent calls now
reject; callers should disable the sign-in button while awaiting a result.
Optional fields no longer contain null or the literal string `"null"`.
Rebuild your native app after upgrading: the new `signOut` TurboModule method
requires regenerated bindings. Clearing Google state **does not revoke consent**,
remove a device account, or end a Firebase/backend session. Revocation is a
separate user/account operation; it is outside this credential library's API.

## Local development

The `example/` app resolves this repository's edited source through Metro and
native autolinking. JavaScript edits can reload through Metro; native or
TurboModule-spec edits require rebuilding the app. The main workspace uses
RN 0.81.1. Use the [isolated compatibility workflow](example/README.md#rn-0863)
to check RN 0.86.3 without changing the main workspace's dependencies.

### Prerequisites

- Use Node from [`.nvmrc`](.nvmrc) and Yarn **4.11.0**. With nvm, run
  `nvm install` and `nvm use` from this directory. If `yarn` is unavailable,
  enable Corepack with `corepack enable`, or invoke the checked-in Yarn release
  directly with `node .yarn/releases/yarn-4.11.0.cjs <command>`.
- Android: JDK **17**, Android SDK **36**, Build Tools **36.0.0**, NDK
  **27.1.12297006**, and SDK platform/command-line tools. Configure `JAVA_HOME`
  and the SDK location (`ANDROID_HOME` or ignored `example/android/local.properties`)
  for your machine. Make `adb` available on your PATH and accept SDK licenses.
  Boot an emulator or connect an authorized device. A Google Play image is
  required for real Google sign-in; `Pixel_8_API_35` was used for verification.
- iOS: macOS, Xcode with its command-line tools selected, an installed iOS
  simulator runtime, and Ruby/Bundler. Install CocoaPods through the example's
  Gemfile as shown below. CI is configured for Xcode **26.3**; verification used
  an iPhone 16e simulator. Physical-device signing is separate from these
  simulator instructions.
- Dependency installation and initial native builds need access to the package,
  Maven, and CocoaPods sources. Simulated E2E needs no Google account or OAuth
  registration; real Google login needs both.

### Install and configure the example

Run from the repository root:

```sh
yarn install --immutable
node scripts/configure-example.mjs
```

The script creates ignored `example/src/config.local.json` from the placeholder
template only when the file is missing. Keep placeholders for simulated E2E.
For real Google login, set `serverClientId` to your **Web OAuth client ID** and
`iosClientId` to your **iOS OAuth client ID**. The default example's application
and bundle ID is `somesoap.reactnativegooglesignin.example`.

Register that Android package with the example's signing SHA-1 in the same Google
project. To obtain the debug fingerprint, run from the repository root:

```sh
(cd example/android && ./gradlew signingReport)
```

Copying another app's `google-services.json` does not register this example.
See [Android setup](#android-setup) and the
[optional registered test-package override](example/README.md) for details.

For real iOS login, register the matching bundle ID and create ignored
`example/ios/GoogleSignIn.local.xcconfig` with the same iOS client ID as the JSON:

```text
GOOGLE_IOS_CLIENT_ID = YOUR_IOS_CLIENT_ID.apps.googleusercontent.com
GOOGLE_REVERSED_CLIENT_ID = com.googleusercontent.apps.YOUR_IOS_CLIENT_ID
```

The example already forwards OAuth callback URLs. The local xcconfig overrides
the shared placeholder configuration. Local JSON, local xcconfig, and Google
service configuration files are ignored; keep them out of commits.

Install iOS dependencies before building or running iOS UI E2E:

```sh
cd example
bundle install
bundle exec pod install --project-directory=ios
cd ..
```

### Run the normal app

Start Metro from the repository root and leave it running in its own terminal:

```sh
yarn example start --port 8081
```

Use another terminal at the repository root. Discover your devices first, then
substitute the Android serial or iOS simulator UDID in the launch commands:

```sh
# Android: boot the emulator first; replace emulator-5554 if necessary.
adb devices
adb -s emulator-5554 reverse tcp:8081 tcp:8081
yarn example android --device emulator-5554

# iOS: choose an available simulator and replace YOUR_SIMULATOR_UDID.
xcrun simctl list devices available
yarn example ios --udid YOUR_SIMULATOR_UDID
```

The normal app calls the real provider. With matching registrations, use a
development account to verify sign-in, cancellation without fallback, explicit
retry, provider sign-out, and subsequent account selection. The app displays
safe status and optional email, never the token. Use disposable devices for
no-account/reauthentication scenarios; do not remove existing device accounts
or reset user data. A chooser appearing alone does not prove successful login.

## Deterministic UI E2E

These suites exercise the actual example UI with **simulated app actions**:
success, cancellation/retry, failure/retry, logout failure/success, and sign-in
after logout. They do not invoke Google OAuth or replace native regression tests.
No Google login, Firebase project, Maestro installation, or device reset is
required. Placeholder local configuration is sufficient.

Before running, complete dependency installation above, keep the example's Metro
server running on **8081**, boot the selected device, and install Pods for iOS.
Run one platform at a time from the repository root:

```sh
# Android: select a connected, unlocked emulator/device and allow Metro access.
adb devices
adb -s emulator-5554 reverse tcp:8081 tcp:8081
ANDROID_SERIAL=emulator-5554 yarn e2e:android

# iOS: select an installed simulator runtime; replace the UDID.
xcrun simctl list devices available
IOS_TEST_DESTINATION='platform=iOS Simulator,id=YOUR_SIMULATOR_UDID' yarn e2e:ios
```

The commands build, install, and run the UI tests. Android uses UiAutomator and
automatically sets `-PgoogleSigninE2E=true`. iOS uses the shared `AuthUI` scheme;
XCUITest launches the app with `--google-signin-e2e`. Both select the debug-only
`index.e2e.js` entry point. The test app must display
`Simulated provider UI verification`; ordinary Google UI means the wrong entry
point is running. Each platform currently has one scenario with seven status
assertions. The scripts return a failing exit code if an assertion fails.

Android reports are under `example/android/app/build/reports/androidTests`;
iOS test results are under `example/ios/build/Logs/Test`. After Android E2E,
restore the normal build with `yarn example android --device emulator-5554`
without the E2E property. For iOS, launch with
`yarn example ios --udid YOUR_SIMULATOR_UDID` without the E2E launch argument.

If the tests cannot load the app, check that port 8081 serves this repository's
example, the selected device is online, and Android's reverse-port mapping is
present. Reuse an existing Metro server only if it serves this example. If iOS
cannot find a destination, install the runtime or select an available UDID; if
Pods configuration is missing, rerun the Pod install step. Native changes need
a fresh build. Do not solve a real Google registration failure by changing
the simulated test setup or retrying provider cancellation automatically.

## Regression checks and evidence

```sh
# From the repository root after local setup:
yarn typecheck
yarn lint
yarn test --runInBand --coverage
yarn prepare
yarn test:android
# Optional native coverage:
# cd example/android && ./gradlew :somesoap_react-native-google-signin:jacocoDebugReport
IOS_TEST_DESTINATION='platform=iOS Simulator,name=iPhone 16e' yarn test:ios
```

The native suites exercise the production request coordinators with SDK
boundaries replaced by controllable fakes. JS tests exercise the real wrapper
with the native bridge replaced. Tests do not establish real OAuth behavior.
See [example commands](example/README.md), [behavior matrix and results](docs/verification.md),
and [repeatable RN compatibility setup](scripts/prepare-rn-compat.mjs).

For agent-assisted maintenance, start with [AGENTS.md](AGENTS.md), then use the
[architecture guide](docs/architecture.md) and [agent runbook](docs/agents.md).
