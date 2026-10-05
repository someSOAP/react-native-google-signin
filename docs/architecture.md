# Authentication architecture

This is a credential bridge for React Native's New Architecture. The public API
is `getGoogleSignInToken(config)` and `signOut()`. Google owns account selection,
consent, and reauthentication; the consuming authentication system owns the app
session. Consumer setup and error codes are documented in [the README](../README.md).

## Source map

| Layer | Source | Responsibility |
| --- | --- | --- |
| Public JS wrapper | `src/index.tsx` | Configuration validation, exported types/errors, sanitized rejection mapping, empty-result guard |
| TurboModule contract | `src/NativeReactNativeGoogleSignin.ts` | Native method and result definitions; input to RN codegen |
| Android React bridge | `android/src/main/java/com/someosap/reactnativegooglesignin/ReactNativeGoogleSigninModule.kt` | Copies bridge arguments, dispatches to main, maps results/promises, observes host lifecycle |
| Android coordinator/provider | `android/src/main/java/com/someosap/reactnativegooglesignin/GoogleSignInManager.kt` | Owns requests, Credential Manager boundary, parsing, provider availability, timeout and cancellation |
| iOS coordinator | `ios/GSAuthCoordinator.h` and `ios/GSAuthCoordinator.m` | Configuration/scheme validation, request ownership, token/profile screening, errors and timeout |
| iOS React bridge/provider | `ios/ReactNativeGoogleSignin.mm` | GoogleSignIn adapter, foreground presenter selection, main-queue dispatch and invalidation |
| Source example | `example/src/App.tsx` | Calls the real library and displays safe status; consumer token exchange is a marked integration point |
| Simulated example actions | `example/src/VerificationApp.tsx` and `example/index.e2e.js` | Deterministic UI testing through debug-only entry points |

The Android main-source directory has a historical `someosap` spelling; the
Kotlin package and test directory use `somesoap`. Do not conflate the path with
the package or rename it incidentally during an authentication fix.

## Requests and provider boundaries

Android's `GoogleSignInManager` receives a `GoogleCredentialProvider`. The real
adapter checks Google Play Services and uses Credential Manager async APIs with
a main-thread executor and `CancellationSignal`. The manager always builds
`GetSignInWithGoogleOption` for the button. No authorized-only bottom sheet,
silent restore, fallback, or automatic retry is retained. It accepts only the
Google ID-token custom credential type and parses it with Google's SDK.

iOS's `GSAuthCoordinator` receives a `GSAuthProvider`. The real adapter uses
GoogleSignIn 9.x, configures the iOS and Web/server client IDs, and starts the
interactive nonce-aware sign-in method without extra scopes. iOS client-ID
precedence is the explicit argument, `GIDClientID`, then the bundled Google
plist's `CLIENT_ID`. The coordinator checks the reversed iOS client URL scheme;
the consuming app must still forward callback URLs. The bridge chooses a visible
controller from an active foreground scene's key window.

The native suites replace these SDK boundaries while exercising the real
coordinators. Adapter, React bridge, and presenter integration also require
native builds and device checks; coordinator coverage does not cover them.

## Ownership, lifecycle, and settlement

Calls run on the main thread and share a pending-operation slot per coordinator.
An overlapping sign-in or sign-out rejects with `IN_PROGRESS`. Request identity
prevents an old callback from settling a newer promise. Completion removes
request ownership and cancels its watchdog before resolving or rejecting.

Both platforms use a five-minute watchdog and reject pending work during module
invalidation. Android cancels its SDK signal on timeout, host destruction, and
invalidation. Pausing the host does not cancel provider UI.

iOS has no public SDK cancellation method. A timeout settles the promise but
keeps the coordinator busy until the old callback drains. The production adapter
also shares a busy guard across module instances. Late successful results after
timeout/invalidation are discarded and provider state is cleared. Cleanup
exceptions after settlement are contained. If the SDK never calls back, an app
restart is needed to release that operation.

Android provider cleanup uses `clearCredentialStateAsync`; iOS uses
`GIDSignIn.signOut`. Neither operation ends a Firebase/backend session or revokes
Google consent. A new interactive attempt is initiated by a new caller action.

## Credentials and error boundaries

Native code requires three nonempty base64url JWT segments with JSON-object
header/payload. This is structural screening only. The backend must verify the
signature, issuer, audience, expiry, and expected nonce before creating a session.
Nonce is forwarded unchanged and is neither hashed nor persisted by the library.

Absent optional profile values are omitted. SDK error descriptions are not
exposed; the JS wrapper normalizes known codes into `GoogleSignInError`. iOS
cancellation requires Google's error domain and canceled code, not just a numeric
match. Android maps typed Credential Manager exceptions; network/reauth failures
that the SDK does not distinguish remain `GET_CREDENTIALS_ERROR`.

For validation commands and evidence rules, see [the agent runbook](agents.md)
and [the dated verification matrix](verification.md).
