# Repository guidance for agents

Applies to this repository, including the source-linked example and test projects.

## Start here

- Read [README.md](README.md) for the public API and consumer setup.
- Read [docs/architecture.md](docs/architecture.md) before changing native behavior.
- Use [docs/agents.md](docs/agents.md) for commands, device checks, and troubleshooting.
- Consult [docs/verification.md](docs/verification.md) for dated evidence and gaps;
  its results are historical, not proof that your new changes passed.
- Inspect `git status` and applicable nested `AGENTS.md` files before editing.
  Preserve existing changes and local OAuth configuration.

## Scope and contract

- This library obtains Google credentials and clears Google provider state.
  The consuming backend or Firebase app owns its session and token verification.
- Keep Firebase dependencies, custom persistent token storage, analytics, broad
  OAuth scopes, and automatic startup authentication outside the library.
- Preserve `getGoogleSignInToken`, the documented response shape, and stable
  error codes. Explain unavoidable contract changes and migration in the README.
- Use current official Google and React Native documentation for SDK changes.
  An attached `react-native-google-auth` checkout is a read-only reference;
  do not copy its session/cache design or downgrade SDKs to match it.
- Edit source in this repository. Do not patch `node_modules`, Pods, generated
  codegen bindings, or `lib/` as an implementation fix.

## Authentication invariants

- Android's explicit button uses `GetSignInWithGoogleOption`. Cancellation must
  settle the request without launching another flow or retrying automatically.
- Validate configuration before native UI; forward caller nonce unchanged.
  Structural JWT screening does not verify authenticity or nonce claims.
- Require a nonempty validly structured ID token; omit absent optional profile
  fields. Do not return null strings or crash on missing profile values.
- Keep native request ownership on the main thread. Serialize sign-in/sign-out,
  settle accepted requests once, and ignore duplicate or stale callbacks.
- Preserve teardown, destroyed-host, and timeout handling. Backgrounding alone
  must not cancel the external provider UI. iOS SDK work remains quarantined
  after timeout until its callback returns because it has no public cancel API.
- Provider sign-out does not revoke consent, delete a device account, or end
  the consumer's app session.
- Never log, display, or put credentials/tokens in test reports or documentation.
  Keep provider error messages sanitized.
- Use placeholders for identifiers copied from local OAuth configuration in
  committed docs, examples, and reports, including project IDs, client IDs,
  registered package overrides, API keys, and certificate fingerprints.

## Validation and delivery

- Add meaningful regression tests for behavior changes using controllable SDK
  boundaries around the production implementation, not a mocked whole API.
- Run checks appropriate to the changed layer using the agent runbook. Native
  adapter/spec changes need codegen and an example native build; JS tests alone
  do not verify them. Documentation-only edits need link and whitespace checks.
- Keep simulated UI E2E, native boundary tests, and real OAuth evidence separate.
  Record platform, RN version, commands, results, and exact unverified scenarios.
- Use dedicated disposable devices for account/reset scenarios. Do not remove
  existing accounts, erase user data, or replace an installed consumer app.
- Do not overwrite local OAuth files or include them in packages or commits.
  Do not publish, push, or perform release actions unless explicitly requested.
- Summarize compatibility changes, checks actually run, and remaining gaps.
  Coverage percentages or a provider chooser appearing do not prove full OAuth.
