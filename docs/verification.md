# Authentication behavior and verification

Date: **2026-10-05** (Europe/Belgrade). A passed fake-provider test does not mean
real Google OAuth passed. Each layer's evidence is recorded separately.

## Scope and pre-implementation audit

Audited target: `@somesoap/react-native-google-signin` **0.2.0**, RN **0.81.1**,
Android Credential Manager **1.5.0** / googleid **1.1.1**, GoogleSignIn iOS **9.0.0**.
Reference: `react-native-google-auth` **1.3.0**, RN **0.81.0** (read-only),
GoogleSignIn iOS 7.x, Credential Manager 1.3.0 / googleid 1.1.0.
Neither repository nor their ancestors contain applicable AGENTS.md instructions.
Existing runner: Jest with a placeholder test in each repository; no native tests.
Example native autolinking and Metro resolve the target source root.
Existing untracked Google configuration files were preserved.

The release version was bumped to **1.0.0** on **2026-10-06**. This metadata-only
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
| Runtime launch RN 0.81.1 | **Passed**: Pixel_8_API_35 / emulator-5554, Android 15 | **Passed**: iPhone 16e, iOS 26.1 |
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
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest -PgoogleSigninE2E=true -PreactNativeArchitectures=arm64-v8a
# 1 UI E2E passed; normal variant restored afterward
# Real OAuth used an unused matching test identity after checking its absence:
./gradlew :app:assembleDebug -PgoogleSigninApplicationId=YOUR_REGISTERED_TEST_PACKAGE -PreactNativeArchitectures=arm64-v8a
# Debug certificate matches the supplied Android OAuth registration; real token obtained

# Repository root, iPhone 16e selected in Mobile Dev:
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
