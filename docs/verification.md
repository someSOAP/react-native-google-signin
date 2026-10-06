# Authentication behavior and verification

Date: **2026-10-05** (Europe/Belgrade). A passed fake-provider test does not mean
real Google OAuth passed. Each layer's evidence is recorded separately.

## Version 1.1.0 and repository hygiene: 2026-10-06

The package version is **1.1.0**. Test-device names, models, serials and UDIDs
were removed from tracked documentation, scripts and CI; command examples use
placeholders. AGENTS.md now requires that these details remain outside the repo.
The iOS test scripts discover an available simulator at runtime, preferring a
booted one, while preserving the `IOS_TEST_DESTINATION` override.

On macOS, with the workspace at RN **0.81.1**, these checks passed:

- CocoaPods **1.16.2** `pod install` from `example/ios`, including codegen;
  the lockfile's library version and podspec checksum were regenerated.
- `yarn prepare` and `yarn pack --out /private/tmp/google-signin-1.1.0.tgz`;
  archive inspection confirmed version, native sources and declarations, with
  local OAuth configuration, build output and native tests excluded.
- `npm publish /private/tmp/google-signin-1.1.0.tgz --dry-run --ignore-scripts
  --access public` with a temporary npm cache; validation passed without
  publishing anything.
- Nine synthetic-fixture checks of simulator discovery and both shell wrappers:
  booted preference, available fallback, exclusion of other platforms and
  unavailable devices, no-simulator failure, and explicit override forwarding.
- Shell/Node syntax, local Markdown links, whitespace and repository/archive
  scans for private application, device and local OAuth identifiers.

These packaging and script checks did not rerun native builds, XCTest, UI E2E
or real OAuth. Their earlier evidence remains in the dated sections below.

## Example bottom-sheet OAuth recheck: 2026-10-06

This recheck supersedes the earlier no-credential/presentation gap for the
googleid 1.2.1 example on the existing development account. Mobile Dev exercised
this repository's normal source-linked example, RN **0.81.1**, on
**Android 15**, using an ignored local OAuth
configuration and registered test identity (`YOUR_REGISTERED_TEST_PACKAGE`).

The example APK's signing certificate was verified, and
`:app:assembleDebug -PreactNativeArchitectures=arm64-v8a` passed. Metro on
**8081** was confirmed to serve this repository's example.

| Current real-provider scenario | Result |
| --- | --- |
| Bottom sheet, `filterByAuthorizedAccounts: false`, `autoSelect: false` | **Passed**: Google bottom sheet appeared with the existing account and Continue button |
| Deliberate dismissal | **Passed**: `Cancelled; ready to retry`; no fallback or automatic second UI |
| Explicit retry and account confirmation | **Passed**: safe `Google credentials received` app status after production token guards accepted the result |
| Provider sign-out after credentials | **Passed**: `Google provider signed out` |
| Sign-in after provider cleanup | **Passed**: bottom sheet reopened and account confirmation returned credentials again |
| `autoSelect: true`, authorized-account filtering omitted/default true | **Passed** for this eligible returning account: credentials returned after tapping Sign in, with no additional confirmation input |

The example source was restored to its documented filtering/auto-selection
disabled settings after the temporary comparison.
No Google account was removed, consent was not revoked, and token values were
not inspected, logged or recorded. The example does not perform a Firebase or
backend token exchange. DNS resolution of Google's hostname now succeeds;
this and the successful retry do not prove the sole cause of the earlier errors.

Remote console configuration was not inspected. Hosted-domain restrictions,
first-time consent, a second account, account-free/reauthentication devices,
iOS OAuth, release builds and backend signature/audience/nonce verification remain unverified in
this recheck. Earlier automated suites/build compatibility evidence is recorded
separately below and was not rerun for these runtime-only checks.

## googleid 1.2.1 and Android flow options: 2026-10-06

This section records checks of the SDK/options changes. The earlier sections
below describe older source and are historical evidence only.

Android now uses **googleid 1.2.1**, Credential Manager / Play Services adapter
**1.5.0**, and **Kotlin 2.4.20**. The previous Kotlin compiler failed on the new
SDK's 2.4 metadata; the library default and example plugin were upgraded, then
the builds below passed. GoogleSignIn on iOS remains **9.0.0**.

The default request still constructs `GetSignInWithGoogleOption`. The optional
`android.flow: 'bottomSheet'` constructs `GetGoogleIdOption`; native boundary
tests inspect the real SDK options, nonce, hosted domain, filtering and
auto-selection settings. JS/native validation rejects invalid combinations
before provider UI. Cancellation and no-credential results settle once without
fallback. Email mapping uses the new SDK's email value rather than its legacy ID.

Host: Node **22.12.0** for RN **0.81.1**, Node **24.16.0** for the isolated
RN **0.86.3** snapshot; JDK **17**, Kotlin **2.4.20**, Gradle **8.14.3** and
CocoaPods **1.15.2**. The compatibility snapshot excluded local OAuth files.

| Check | RN 0.81.1 | RN 0.86.3 |
| --- | --- | --- |
| `yarn typecheck`, `yarn test --runInBand`, `yarn prepare` | **Passed**, 53 JS cases | **Passed**, 53 JS cases |
| `yarn lint` | **Passed**, zero errors; two existing example debugger warnings | **Not rerun** in the snapshot |
| Android `testDebugUnitTest` and `:app:assembleDebug`, arm64-v8a | **Passed**, 25 native cases and generated bindings | **Passed**, 25 native cases and generated bindings |
| iOS Pod install/codegen and generic simulator `xcodebuild` | **Passed** | **Passed**, with RN prebuilt dependencies enabled |
| Markdown local links and `git diff --check` | **Passed** | Same source documentation |

Android retry ran on **Android 15**,
Google Play Services **26.37.35**, and the normal RN **0.81.1** example with the
existing user-selected test identity (`YOUR_REGISTERED_TEST_PACKAGE`). The
user removed the example installations before this reinstall. Metro served the
source-linked example on port **8081**; a full reload confirmed the final
bottom-sheet configuration with filtering and auto-selection disabled.

| Current real-provider check | Result |
| --- | --- |
| Updated button flow UI and deliberate dismissal | **Passed**: Google chooser appeared; dismissal returned `Cancelled; ready to retry` without a second flow |
| Final bottom-sheet request | **Observed**: `NO_CREDENTIALS_ERROR`, controls available for explicit retry, no automatic fallback UI |
| Bottom-sheet presentation / successful credentials | **Not verified** on this run |

The emulator could not resolve `www.google.com` (`ping: unknown host`) and
provider logs reported DNS failures. These may contribute to the provider's
no-credential result; the returned code alone does not establish its cause.
No account was removed, device data was not erased, and token values were not
inspected or recorded. A complete OAuth round trip, hosted-domain filtering and
auto-selection with real eligible accounts remain unverified for this SDK update.
iOS runtime/XCTest, deterministic UI E2E, RN 0.86.3 runtime and release builds
were not rerun; native builds/codegen and controllable-boundary tests are the
current evidence. Existing successful OAuth evidence below predates the update.

Commands actually run for the native checks:

```sh
cd example/android
./gradlew :somesoap_react-native-google-signin:testDebugUnitTest :app:assembleDebug -PreactNativeArchitectures=arm64-v8a
./gradlew :app:installDebug -PreactNativeArchitectures=arm64-v8a

cd ../ios
pod install
xcodebuild -workspace ReactNativeGoogleSigninExample.xcworkspace -scheme ReactNativeGoogleSigninExample -configuration Debug -destination 'generic/platform=iOS Simulator' -derivedDataPath build CODE_SIGNING_ALLOWED=NO build
```

The same test/build commands passed in the RN 0.86.3 snapshot, with
`RCT_USE_PREBUILT_RNCORE=1 RCT_USE_RN_DEP=1 pod install` for its iOS integration.
SDK version reference: [Google release notes](https://developers.google.com/identity/android-credential-manager/releases).

## Successful development-package recheck: 2026-10-06

This later run supersedes the unsuccessful same-day package checks below for
the registered development test identity. The example's local Gradle
configuration used that identity (`YOUR_REGISTERED_DEV_PACKAGE` here), and the
installed package was confirmed to be this verification example. Its existing
`serverClientId` matched the Web client in the supplied development JSON.

Platform: **Android 15**, **RN 0.81.1**.
Mobile Dev exercised the normal provider app using the existing development
account. Metro on **8081** was confirmed to serve this repository's example.

| Real Google scenario | Result |
| --- | --- |
| Deliberate chooser cancellation | **Passed**: `Cancelled; ready to retry`, no automatic second flow observed |
| Explicit retry after cancellation | **Passed**: chooser reopened; account selection returned `Google credentials received` |
| Provider sign-out after successful sign-in | **Passed**: `Google provider signed out` |
| Sign-in after provider cleanup | **Passed**: chooser reopened; selecting the same account returned credentials again |

Success was observed through safe app status after the production native and JS
guards accepted the result. Token contents were not inspected, logged, displayed,
or copied into the report. No backend signature/audience/nonce verification or
consumer app session was exercised. A second account, first-time consent,
account-free/reauthentication devices, release builds, and iOS remain unverified
in this run. The earlier reauthentication diagnostics do not prove an account
problem; the corrected development package succeeded without account resets.

The separate simulated UI E2E suite also **passed**:
`AuthUiTest.cancellationFailureRetryAndLogout`, **1 test**, **0 failures/errors**,
with all **7 status assertions** (cancellation/retry, success, failure/retry,
logout failure/success, and sign-in after logout). The XML report is under
`example/android/app/build/outputs/androidTest-results/connected/debug`; the
HTML report is under `example/android/app/build/reports/androidTests`.
These assertions exercise simulated app actions, not Google OAuth.

Commands actually run from `example/android`, with the existing example Metro
server on port 8081 and `ANDROID_SERIAL=YOUR_ANDROID_SERIAL` for the connected test:

```sh
./gradlew :app:connectedDebugAndroidTest -PgoogleSigninE2E=true -PreactNativeArchitectures=arm64-v8a
./gradlew :app:assembleDebug -PreactNativeArchitectures=arm64-v8a
```

The normal real-provider APK was installed and relaunched after the simulated
suite; its ordinary verification heading and `Ready` status were confirmed.
The user's existing Gradle and App.tsx debugging edits and local OAuth JSON were
preserved, checked against pre-test SHA-256 digests. The existing Metro server
was retained. No consumer app was replaced, account removed, or device reset.
Native unit tests and iOS E2E were not rerun. Local Markdown links and
`git diff --check` passed.

## Android real OAuth recheck: 2026-10-06

**Package-selection correction:** the first recheck below selected a different
package listed in the supplied consumer JSON, rather than the consumer's Gradle
application ID. Its result does not verify the intended consumer identity.
The corrected retry is recorded separately below. Reauthentication-related
provider messages do not establish that the account itself is the root cause.

RN **0.81.1**, Android **15**. The supplied
consumer development `google-services.json` contained an Android registration
matching the example debug certificate. Its Web client ID already matched the
ignored local example configuration; local OAuth files were left unchanged.
The registered package held a previous build of this verification example,
identified by its example launcher activity. That APK was backed up before the
test build was installed; no consumer app or device account was replaced.

The normal real-provider build passed with:

```sh
cd example/android
./gradlew :app:assembleDebug -PgoogleSigninApplicationId=YOUR_REGISTERED_TEST_PACKAGE -PreactNativeArchitectures=arm64-v8a -PreactNativeDevServerPort=8082
# Separate terminal, repository root:
yarn example start --port 8082
```

Port 8081 belonged to another project. Mobile Dev exercised the real provider:
the chooser appeared, deliberate Back cancellation returned
`Cancelled; ready to retry`, and explicit retries reopened the chooser.
Three account-selection attempts returned cancellation, including one after
successful provider sign-out (`Google provider signed out`). Sanitized Google
diagnostics contained reauthentication-related errors and status 16. The device
reported a validated Internet connection; this does not prove every Google
endpoint was reachable or establish the underlying cause of those errors.

**Successful credentials were not obtained in this recheck.** Success after
account reauthentication, logout after successful sign-in, and successful
sign-in after logout remain unverified in this run. The earlier successful
Android OAuth evidence below is historical and does not establish success today.
No simulated UI E2E, native regression suite, iOS OAuth, or backend token exchange
was run for this check.

Cleanup passed: the previous verification APK was restored and its bytes checked
against the backup. A fresh `:app:assembleDebug -PreactNativeArchitectures=arm64-v8a`
build restored the default application ID and Metro port. The temporary Metro
server and reverse-port mapping were removed, along with the temporary Google
configuration copy. Source defaults and local OAuth configuration were unchanged.
The report's local Markdown links and `git diff --check` passed.

### Corrected application-ID retry: 2026-10-06

The consumer's `android/app/build.gradle` sets the requested base application ID;
its development flavor adds `.dev`. This retry used the exact requested base ID,
represented here as `YOUR_REQUESTED_BASE_PACKAGE`, without the development suffix.
The supplied development JSON lists the consumer's suffixed package with a Web
OAuth client but no Android OAuth entry; it has no client entry for the requested
base package. The example's existing `serverClientId` matches that Web client.
This describes the supplied file, not the current state of Google Cloud's
registrations. The JSON was consulted as configuration data and was not bundled
or processed by a Google Services Gradle plugin in the verification example.

The base package was absent from the selected emulator before installation. On
the same RN 0.81.1 / Android 15 device, the following normal provider build passed:

```sh
cd example/android
./gradlew :app:assembleDebug -PgoogleSigninApplicationId=YOUR_REQUESTED_BASE_PACKAGE -PreactNativeArchitectures=arm64-v8a -PreactNativeDevServerPort=8082
# Separate terminal, repository root:
yarn example start --port 8082
```

The installed example's foreground package was checked against the requested
base ID. Mobile Dev opened the real Google chooser and selected the existing
account. The app returned `Cancelled; ready to retry`; **no successful credentials
were obtained**. Diagnostics limited to this attempt contained
reauthentication-related errors and status 16. The underlying cause remains
unverified. Provider sign-out, successful sign-in after cleanup, simulated UI
E2E, native regression suites, iOS, and backend token exchange were not repeated
in this corrected retry.

Cleanup passed: only the temporary example installed by this retry was removed,
and its package absence was checked. The Metro server and reverse-port mapping
were removed. A normal `:app:assembleDebug -PreactNativeArchitectures=arm64-v8a`
build restored the default package and Metro port. The local OAuth JSON matched
its pre-run SHA-256 digest. Local Markdown link and whitespace checks passed.

## Scope and pre-implementation audit

Audited target: `@somesoap/react-native-google-signin` **0.2.0**, RN **0.81.1**,
Android Credential Manager **1.5.0** / googleid **1.1.1**, GoogleSignIn iOS **9.0.0**.
Reference: `react-native-google-auth` **1.3.0**, RN **0.81.0** (read-only),
GoogleSignIn iOS 7.x, Credential Manager 1.3.0 / googleid 1.1.0.
Neither repository nor their ancestors contain applicable AGENTS.md instructions.
Existing runner: Jest with a placeholder test in each repository; no native tests.
Example native autolinking and Metro resolve the target source root.
Existing untracked Google configuration files were preserved.

The release version was bumped to **1.1.0** on **2026-10-06**. This metadata-only
bump does not represent a new run of the behavior checks recorded below.

The matrix was created before implementation and extended for missing callbacks,
synchronous provider exceptions, destroyed activities and stale cleanup errors.
Reference flows considered: button vs bottom sheet, provider cleanup,
Play Services checks, restoration, token errors. Its persistent token cache,
extra scopes, retry loops and app-session APIs were excluded. Official SDK docs
are authority. No Firebase consumer is attached. The reference has no changes.

Original regressions and tests of the corrected production implementation:

| Original problem | Regression evidence |
| --- | --- |
| Authorized-only Android bottom sheet used for a button | `buttonOptionForFirstAndReturningUsersForwardsClientAndNonce` inspects the actual `GetSignInWithGoogleOption` request |
| Missing nonce map access could throw | JS omitted-nonce test; native button test; native module checks key existence |
| Unexpected custom credential type could hang | `unknownCustomAndNonGoogleTypesAlwaysReject` checks settlement for custom and password credentials |
| Android null photo became string `null` | `optionalProfileDoesNotBecomeNullString` |
| Unowned coroutine and exception paths could hang | manager uses cancellable SDK callbacks; teardown, timeout, synchronous exception and stale callback tests |
| iOS nil dictionary fields could crash | `testMissingAndOptionalProfileFields`; SDK adapter builds dictionary only from present values |
| Missing / malformed token accepted | native token validation tests on both platforms; JS empty bridge-result checks |
| iOS numeric error alone treated as cancellation | `testCancellationHasNoRetryAndRequiresGoogleDomain` |
| No concurrency / teardown ownership | native concurrent request, duplicate callback, teardown and retry tests |
| Example logged tokens, lacked callback URL integration | tokens removed from UI/logs; iOS AppDelegate forwards Google URL; generated URL scheme validated before UI |

Old implementations had different constructors, so new coordinator tests cannot
be dropped into the old tree unchanged. The table identifies the observable
regressions each test would expose; no claim is made that all tests were executed
against the old revision.

## Automated behavior matrix

**Passed** below means the named production coordinator or wrapper was exercised
with a controllable platform/SDK boundary. See real-device matrix below.

Android evidence: [native tests](../android/src/test/java/com/somesoap/reactnativegooglesignin/GoogleSignInManagerTest.kt)
(**19 passed**). iOS evidence: [XCTest](../tests/ios/GSAuthCoordinatorTests.m)
(**15 passed**). JS evidence: [contract tests](../src/__tests__/index.test.tsx)
(**33 passed**), also run successfully using RN 0.86.3's separate Jest preset.

| Scenario | Android | iOS |
| --- | --- | --- |
| Successful result, missing/full optional profile | **Passed**: button, optional-profile and full-profile tests | **Passed**: first/returning and optional-profile tests |
| First-time vs previously authorized accounts | **Passed**: every request uses explicit button option; 2 successive requests | **Passed**: every request starts interactive provider; 2 successive requests |
| No account / reauth interactive path | **Passed**: button option selected; no credential / provider failure settles without second UI | **Passed**: interactive provider invoked; reauth error settles and caller can retry |
| Cancellation with no fallback | **Passed**: typed cancellation, synchronous cancellation, request-count assertions | **Passed**: Google domain + canceled code; no second provider call |
| Missing provider / unavailable Play Services | **Passed**: availability false, unsupported/configuration callbacks, logout missing provider | **Not verified**: not applicable; GoogleSignIn linked into the app |
| Network/provider failure | **Passed**: SDK unknown errors map to GET_CREDENTIALS_ERROR; no unsupported claim of Android network-specific classification | **Passed**: NSURLErrorDomain maps to NETWORK_ERROR; Google errors remain provider errors |
| Configuration validation before UI | **Passed**: null/blank/invalid ID, blank nonce; provider configuration exception | **Passed**: bad IDs/nonce, missing client ID/scheme, fallback/override ID; no provider start |
| Missing/empty/malformed token and parsing failure | **Passed**: blank/malformed JWT, failed createFrom, empty validator input | **Passed**: absent/null/empty/invalid JWT and invalid payload types |
| Unknown credential type | **Passed**: custom and password reject once | **Not verified**: not applicable to typed GIDSignInResult; missing result tested as INVALID_TOKEN_ERROR |
| Correct server ID and nonce forwarding | **Passed**: actual request option inspected, nonce unchanged | **Passed**: SDK boundary arguments inspected, explicit/fallback iOS ID and optional nonce |
| Concurrent sign-in/sign-out | **Passed**: second request rejects IN_PROGRESS | **Passed**: sign-in and sign-out reject during pending sign-in |
| Repeated/stale callbacks | **Passed**: old callback cannot settle a new request | **Passed**: duplicate callback cannot alter new request's providerBusy state |
| Module teardown | **Passed**: signal canceled, promise rejected once, late callback ignored | **Passed**: reject once, discard late result, cleanup failure cannot crash |
| Destroyed host / foreground presenter | **Passed**: missing, finishing, destroyed activity preflight; active request canceled on host destruction | **Passed**: missing presenter rejects; foreground UI separately checked on simulator |
| Retry after failure/cancellation | **Passed**: caller-created fresh request succeeds | **Passed**: caller-created fresh request succeeds |
| Provider logout success/failure and subsequent sign-in | **Passed**: actual coordinator calls clear boundary, serialized; unavailable provider and retry tested | **Passed**: coordinator calls signOut boundary; failure/exception and retry tested |
| Provider never completes | **Passed**: 5-minute timeout, signal canceled, retry allowed | **Passed**: timeout settles once, SDK quarantined until callback, late provider state cleared |
| JS result/error mapping | **Passed**: real wrapper forwards config, maps all stable codes, sanitizes messages, guards empty results | **Passed**: same cross-platform contract suite |
| Background transitions | **Not verified**: onHostPause deliberately preserves pending provider UI; physical interrupt transitions not comprehensively exercised | **Not verified**: no cancel on background; physical interrupt transitions not comprehensively exercised |

## Native compatibility and deterministic E2E

Host: Node 22.12.0 for baseline, Node 24.16.0 for RN 0.86.3; JDK 17.0.11;
Xcode 26.3; CocoaPods 1.16.2. RN 0.86.3 was installed in an isolated source
snapshot at `/private/tmp/google-signin-rn0863`; the main workspace remains
RN 0.81.1. That copy was refreshed with final library source before final builds.

| Check | Android | iOS |
| --- | --- | --- |
| RN 0.81.1 codegen + example native build | **Passed**: Gradle assembleDebug, arm64-v8a | **Passed**: Pod install/codegen + xcodebuild simulator build |
| RN 0.86.3 codegen + example native build | **Passed**: Gradle assembleDebug, arm64-v8a; 19 native cases | **Passed**: Pod install/codegen + xcodebuild simulator build; GoogleSignIn 9.0.0 retained |
| Runtime launch RN 0.81.1 | **Passed**: Android 15 | **Passed**: iOS 26.1 |
| Runtime launch RN 0.86.3 | **Not verified**: build only | **Not verified**: build only |
| Deterministic example UI E2E | **Passed**: UiAutomator `cancellationFailureRetryAndLogout`, 1 case with 7 status assertions | **Passed**: XCUITest `testCancellationFailureRetryAndLogout`, 1 case with 7 status assertions |
| Physical device / release build | **Not verified** | **Not verified** |

E2E uses a debug-only separate `index.e2e.js` with simulated app actions.
It checks the example's actual UI, not Google OAuth or native provider adapters.
The production coordinators are covered separately by the native suites. Android
real OAuth was additionally verified in the matching test-identity variant.

## Real Google / device evidence

Mobile Dev was used for app UI inspection and control, Android first, then iOS.
Normal real-provider examples were restored after simulated E2E. Device accounts
were not removed, consent was not revoked, and device data was not erased.

| Scenario | Android | iOS |
| --- | --- | --- |
| Provider interactive UI appears | **Passed**: Google account chooser with existing-account and add-account options | **Passed**: system Google sign-in prompt and accounts.google.com login page |
| Deliberate cancellation | **Passed**: Back closes chooser with both default and matching registered test identity; app reports `Cancelled; ready to retry`; no second UI | **Passed**: Cancel in system prompt; same app status; no second UI |
| Retry after cancellation | **Passed**: explicit button reopens Google chooser; matching test identity then obtains real credentials after cancellation | **Passed**: explicit button reopens system auth UI |
| Provider sign-out | **Passed**: real clearCredentialState succeeds after real credentials were obtained | **Passed**: real GIDSignIn.signOut returns success; no prior successful login established |
| Sign-in after cleanup / account selection UI | **Passed**: chooser reopened after cleanup; selecting the same account returns fresh credentials | **Passed**: interactive auth prompt reopened after cleanup |
| Obtain nonempty real ID token | **Passed**: matching registered test-package variant received credentials; native + JS guards require a nonempty ID token | **Not verified**: development-account credentials unavailable in simulator |
| Successful OAuth URL callback | **Not verified**: not applicable | **Not verified**: callback implementation compiled, successful OAuth round trip needs account login |
| Logout after successful real sign-in / switch account | **Passed** for logout after successful sign-in and re-selection of the same account; **not verified** for switching to a second account (none supplied) | **Not verified**: no successful login / account credentials |
| First-time consent vs previously authorized user | **Passed** for repeated/returning sign-in; **not verified** for fresh first-time consent (consent was not revoked) | **Not verified**: credentials needed |
| Device has no Google account | **Not verified**: selected emulator contains an existing account; no disposable account-free device was supplied | **Not verified**: no Google login completed; full first-authorization flow needs credentials |
| Account requires reauthentication | **Not verified**: no dedicated reauth test account/device; existing account security state was not changed | **Not verified**: no dedicated reauth test account/device |
| Real offline / missing Play Services environment | **Not verified**: fake-provider errors tested, no device settings changed | **Not verified**: fake network errors tested |
| Firebase exchange / logout / session persistence | **Not verified**: no Firebase consumer repository attached | **Not verified**: same |

## Coverage and remaining gaps

JS wrapper: 100% statements/branches in the Jest run. This is not a verification
claim for native/provider behavior. JaCoCo reports 56/56 manager lines and 40/42
manager branches; it helped identify destroyed-activity and thrown-configuration
paths, which received tests. Real Android provider/React bridge classes have zero
coverage in JVM tests because those are the replaced boundary. Real cancellation
and cleanup exercised that integration on device outside JVM coverage.
Xcode reports 179/179 executable coordinator lines; real SDK adapter and presenter
selection are outside the coordinator XCTest target. No percentage is used as
an acceptance criterion. Branch coverage, multi-window presentation, keychain
failures, background interruptions, release builds, and complete cross-platform OAuth remain gaps.

**Resolved Android OAuth setup issue:** the local development registration used
a different package from the default example,
`somesoap.reactnativegooglesignin.example`. The default identity returned SDK
cancellation after selecting an account. We checked the registered package was
not installed, compared its supplied certificate hash with the example debug
keystore (matched), built a temporary example variant with
`-PgoogleSigninApplicationId=YOUR_REGISTERED_TEST_PACKAGE`, and obtained real credentials.
No existing app was replaced. The default example still needs its own matching
OAuth registration, or an explicitly selected unused registered test package.
The library correctly does not retry provider cancellations automatically.

Other external blockers: a development Google login on iOS, a second account for
switching, dedicated disposable no-account/reauth devices, and a Firebase consumer
if Firebase integration is desired. No unresolved implementation failure was found in the exercised suites.
Native build/test failures discovered during
implementation were repaired, including regenerating CocoaPods integration after
refreshing the RN 0.86.3 Xcode project (required for its prebuilt Swift module paths). CI configuration is provided but has not been run
on remote GitHub runners; nothing was pushed or published.

## Commands and results

All final checks below passed unless explicitly qualified. Initial compile/test
setup failures (SDK test imports/builders, iOS UI target product name, Watchman
sandbox startup) were corrected before these results.
Local OAuth identifiers and simulator UDIDs are replaced with placeholders in
the command examples; substitute your own values to repeat those checks.

```sh
yarn install --immutable                # passed; existing peer-tooling warnings
yarn lint                              # passed
yarn typecheck                         # passed
yarn test --runInBand --coverage        # 33 passed
yarn prepare                           # module + declaration builds passed
yarn pack --out /private/tmp/somesoap-google-signin-review.tgz
# package inspected: native sources/types included; tests and Google config excluded

cd example/android
./gradlew :somesoap_react-native-google-signin:testDebugUnitTest :app:assembleDebug -PreactNativeArchitectures=arm64-v8a
./gradlew :somesoap_react-native-google-signin:jacocoDebugReport  # 19 passed
ANDROID_SERIAL=YOUR_ANDROID_SERIAL ./gradlew :app:connectedDebugAndroidTest -PgoogleSigninE2E=true -PreactNativeArchitectures=arm64-v8a
# 1 UI E2E passed; normal variant restored afterward
# Real OAuth used an unused matching test identity after checking its absence:
./gradlew :app:assembleDebug -PgoogleSigninApplicationId=YOUR_REGISTERED_TEST_PACKAGE -PreactNativeArchitectures=arm64-v8a
# Debug certificate matches the supplied Android OAuth registration; real token obtained

# Repository root, selected iOS simulator:
IOS_TEST_DESTINATION='platform=iOS Simulator,id=YOUR_SIMULATOR_UDID' yarn test:ios
# 15 XCTest cases passed; code coverage enabled
IOS_TEST_DESTINATION='platform=iOS Simulator,id=YOUR_SIMULATOR_UDID' yarn e2e:ios
# 1 UI E2E passed

cd example/ios
pod install                            # generated native bindings; GoogleSignIn 9.0.0
xcodebuild -workspace ReactNativeGoogleSigninExample.xcworkspace -scheme ReactNativeGoogleSigninExample -configuration Debug -destination 'platform=iOS Simulator,id=YOUR_SIMULATOR_UDID' -derivedDataPath build CODE_SIGNING_ALLOWED=NO build
# passed on baseline; equivalent generic simulator build passed in RN 0.86.3 copy
```

Repeatable RN 0.86.3 preparation and build commands are in
[example/README.md](../example/README.md). Native reports are under ignored
`android/build/reports/tests`, `android/build/reports/jacoco`,
`example/android/app/build/reports/androidTests`, `tests/ios/build/Logs/Test`,
and `example/ios/build/Logs/Test`. JaCoCo command and XCTest scripts are repeatable.
CI runs JS/type/package checks, native regression tests and baseline / RN 0.86.3
builds. Deterministic device E2E is available as an explicit local command.

Official references:
[Android flows](https://developer.android.com/identity/sign-in/credential-manager-siwg),
[Android implementation](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation),
[iOS setup](https://developers.google.com/identity/sign-in/ios/start-integrating),
[iOS SDK](https://developers.google.com/identity/sign-in/ios/reference/Classes/GIDSignIn),
[React Native lifecycle](https://reactnative.dev/docs/the-new-architecture/native-modules-lifecycle).
