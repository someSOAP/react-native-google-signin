# Source verification app

Metro and native autolinking resolve the edited library root, not a registry
package or another app's node_modules. Keep this app separate from your
existing apps: its application / bundle ID is
`somesoap.reactnativegooglesignin.example`.

From the repository root:

```sh
yarn install --immutable
node scripts/configure-example.mjs
```

Edit ignored `example/src/config.local.json` with the Web and iOS client IDs.
Register this example's package ID and debug SHA-1 in Google Cloud. Obtain it
with `cd example/android && ./gradlew signingReport`. Existing consumer Google
configuration files may name a different package; copying them does not register
this example. No Google Services plugin or Firebase dependency is used. For a registered test
package that is not already installed on the device, the Android debug build can
use `-PgoogleSigninApplicationId=YOUR_REGISTERED_TEST_PACKAGE`. Never use this to
replace an existing app. The default remains the separate example package.

For iOS, create ignored `example/ios/GoogleSignIn.local.xcconfig`:

```text
GOOGLE_IOS_CLIENT_ID = YOUR_IOS_CLIENT_ID.apps.googleusercontent.com
GOOGLE_REVERSED_CLIENT_ID = com.googleusercontent.apps.YOUR_IOS_CLIENT_ID
```

The app delegate forwards OAuth callback URLs. No token is displayed or logged.
The app shows only status and optional email. The consuming backend exchange
point is marked in `App.tsx`; this example creates no independent app session.

Start Metro in one terminal, then build and run:

```sh
yarn example start --port 8081
# Separate terminal:
yarn example android --device emulator-5554
cd example/ios && pod install
# From repository root:
yarn example ios --udid YOUR_SIMULATOR_UDID
```

## Deterministic UI E2E (simulated app provider)

Keep Metro running on port 8081. These tests check the **real example UI**
with injected app actions: success, cancellation/retry, failure/retry, provider
logout success/failure, and sign-in after logout. They do not call Google OAuth
or replace the native regression suites. Android uses UiAutomator; iOS uses
XCUITest. No Maestro, account credentials, or device reset is required.

```sh
# Only the selected emulator receives the test:
ANDROID_SERIAL=emulator-5554 yarn e2e:android
IOS_TEST_DESTINATION='platform=iOS Simulator,id=YOUR_UDID' yarn e2e:ios
```

Android selects `index.e2e` only in debug builds with
`-PgoogleSigninE2E=true`. iOS selects it only in debug builds launched with
`--google-signin-e2e`. Normal / release entry points use the actual library.
Restore the normal Android build with `yarn example android` after E2E.

## Real OAuth checklist

Run the normal app with matching OAuth registrations. Select a development
account and verify the credential status. Cancel once, verify ready-to-retry,
then explicitly try again. Sign out and sign in again to inspect account
selection. Use a dedicated disposable Google Play emulator for no-account and
reauthentication scenarios. Do not delete personal accounts or reset device data.
An account chooser appearing alone does not establish successful sign-in.

## RN 0.86.3

Use Node supported by that release (the checked build used Node 24.16.0).
Create a clean isolated copy with the supplied script, which pins React 19.2.3,
the RN 0.86.3 tooling, and its separate Jest preset:

```sh
node scripts/prepare-rn-compat.mjs /tmp/google-signin-compat 0.86.3
cd /tmp/google-signin-compat
yarn install
yarn typecheck
yarn test --runInBand
yarn prepare
cd example/android
./gradlew :somesoap_react-native-google-signin:testDebugUnitTest :app:assembleDebug -PreactNativeArchitectures=arm64-v8a
cd ../ios
RCT_USE_PREBUILT_RNCORE=1 RCT_USE_RN_DEP=1 pod install
xcodebuild -workspace ReactNativeGoogleSigninExample.xcworkspace -scheme ReactNativeGoogleSigninExample -configuration Debug -destination 'generic/platform=iOS Simulator' -derivedDataPath build CODE_SIGNING_ALLOWED=NO build
```
