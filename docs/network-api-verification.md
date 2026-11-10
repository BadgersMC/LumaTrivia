# Network companion verification

Canonical base: a293dafb3427567ed40b72f20ba706b98b463202.
Network main 559bfabc2187ab796a3be889f032383a8041f819 pins RoseChat
cf8a7b040f7194a0bf26d98096493bbb7e53efa9 (BadgersMC/Enthusia-RoseChat).

## Spec and diagnostic

REQ-020 keeps the historical build default, but lets the network compile against
its real companion without overwriting tracked binaries. An explicit missing
input must fail, not be ignored. No gameplay engine change or historical unit
TDD red/green claim is made. Existing Kotlin domain/application boundaries remain
unchanged; only compile infrastructure and verification change.

## Gates

- Build the exact immutable RoseChat source with its Java 21 Gradle shadowJar.
- Verify Trivia clean test/shadowJar with -ProseChatJar pointing to that artifact.
- Reject an explicit nonexistent path; do not fall back to libs/RoseChat-RC-2.jar.
- Preserve the no-argument Java 21 build and optional runtime loading semantics.
- Hosted exact-head CI/review, merge and monorepo pin precede release artifacts.
- Local companion compilation is not actual Paper chat/mute or client acceptance.

No EARS/state helpers exist locally; requirements/tasks and this evidence record
govern the infrastructure SPEAR cycle. No production access or changes.

## Local results, 2026-10-08

The unchanged canonical base accepts/ignores -ProseChatJar=does-not-exist.jar
and reports compileKotlin success (up-to-date). After the repair, configuration
rejects that explicit missing path with the expected missing-API diagnostic.
An initial unquoted PowerShell property attempt split the filename into task
arguments; that shell failure is not behavioral proof and was rerun quoted.

RoseChat cf8a7b04 built from clean exact source as RC-4 on Java 21. Artifact
SHA-256: `f584f00880b7249a8d2e12657a70758316a847021ecded6b9ce707f98df5c163`.
Trivia clean test/shadowJar with the explicit built artifact passed all 41
tests, zero failures/errors/skips. The environment-selected clean build and
unchanged no-argument legacy build also pass all 41 tests. The legacy tracked
binary was not modified. Actual-companion shaded JAR SHA-256:
`d82df21a8e85ce685734221508570cb79fa406c65dd25c7ef9d39668b4a7ff04`.
ZIP inspection confirms no RoseChat runtime classes are bundled; workflow YAML
parses with both verification jobs present.
The new CI job builds the same immutable companion and tests explicit missing
input, actual clean compilation, and compile-only packaging. Hosted results
and review are pending; live chat/mute and client acceptance remain unverified.
