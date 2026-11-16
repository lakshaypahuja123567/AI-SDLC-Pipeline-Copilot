# Code Review: SIMNOTE (SimpleNote)

## Iteration 6 (current)

Labeling note: the requested label was "iteration 4", but this file already contains an "Iteration 4" section (T-01 to T-15 review). This section is labeled iteration 6 to keep the history unambiguous.

**Overall status:** Approved

**Review scope:**

- Change set reviewed: commits `20d6e5a` (CR-002), `33df944` (CR-003 and CR-001 test), `8b038c9` (I-1 strip fix and Unicode test), `ff2cc2c` (CR-004), `e1ff86c` (XSRF cookie test), and `3a700ff` (I-2, I-3, I-4 tests) on `feature/task-15-readme`, range `fbf805a..3a700ff`. Committed SIMNOTE changes in range: 6 files, 114 insertions, 16 deletions. Files: [NoteService.java](SIMNOTE/src/main/java/com/simnote/note/NoteService.java), [AppUser.java](SIMNOTE/src/main/java/com/simnote/auth/AppUser.java), [SecurityConfig.java](SIMNOTE/src/main/java/com/simnote/config/SecurityConfig.java), [NoteServiceTest.java](SIMNOTE/src/test/java/com/simnote/note/NoteServiceTest.java), [ApiWebMvcTest.java](SIMNOTE/src/test/java/com/simnote/config/ApiWebMvcTest.java), [XsrfCookieTest.java](SIMNOTE/src/test/java/com/simnote/config/XsrfCookieTest.java).
- Context read: [TrimmedSize.java](SIMNOTE/src/main/java/com/simnote/dto/TrimmedSize.java), [NoteRequest.java](SIMNOTE/src/main/java/com/simnote/dto/NoteRequest.java), [RegisterRequest.java](SIMNOTE/src/main/java/com/simnote/dto/RegisterRequest.java), [AuthService.java](SIMNOTE/src/main/java/com/simnote/auth/AuthService.java), [UsernameNormalizer.java](SIMNOTE/src/main/java/com/simnote/service/UsernameNormalizer.java), [AppConfig.java](SIMNOTE/src/main/java/com/simnote/config/AppConfig.java), [SameSiteCookieFilter.java](SIMNOTE/src/main/java/com/simnote/config/SameSiteCookieFilter.java), `docs/architecture.md`.
- Source documents and approval state:
  - Requirements: iteration 8, owner-approved (Lakshay Pahuja). Q-E11 items approved 2026-11-16.
  - Architecture: iteration 6, approver Lakshay Pahuja, 2026-11-16.
  - Design review: owner-approved 2026-11-16.
  - Implementation plan: approved (iteration 1, 2026-11-15).
  - All four are present and the approval state is consistent with the owner's statement. Approval block is cleared.
- Working tree (not part of the change set, not reviewed, not modified): `.github/sdlc/approvals.json` shows the three approval flags as `true` while the committed version has them `false`. `.vscode/mcp.json` is modified and `.github/sdlc/verification-report.md` is untracked. None of these are staged or committed by this review.
- Test evidence: surefire XML in `SIMNOTE/target/surefire-reports` shows 18 suites, 95 tests, 0 failures, 0 errors, 0 skipped. This does not match the owner-reported count of 91; the XML count is used here. Tests were not re-run by this agent.

**Prior finding status:**

| ID | Status | Evidence |
|---|---|---|
| CR-001 | Resolved | `create()` and `update()` use `strip()` ([NoteService.java](SIMNOTE/src/main/java/com/simnote/note/NoteService.java#L36), [NoteService.java](SIMNOTE/src/main/java/com/simnote/note/NoteService.java#L48)), matching the validator ([TrimmedSize.java](SIMNOTE/src/main/java/com/simnote/dto/TrimmedSize.java#L46)). Covered by [NoteServiceTest.java](SIMNOTE/src/test/java/com/simnote/note/NoteServiceTest.java#L74) and [NoteServiceTest.java](SIMNOTE/src/test/java/com/simnote/note/NoteServiceTest.java#L116). |
| CR-002 | Resolved | No `@Value` or manual key check in [SecurityConfig.java](SIMNOTE/src/main/java/com/simnote/config/SecurityConfig.java). Key is validated once by `RememberMeProperties` (`@NotBlank @Size(min = 32)`). |
| CR-003 | Resolved | [NoteService.java](SIMNOTE/src/main/java/com/simnote/note/NoteService.java#L19) injects `Clock`; `clock.instant()` at lines 36 and 48. Bean in [AppConfig.java](SIMNOTE/src/main/java/com/simnote/config/AppConfig.java#L13). Deterministic timestamps tested at [NoteServiceTest.java](SIMNOTE/src/test/java/com/simnote/note/NoteServiceTest.java#L42), [NoteServiceTest.java](SIMNOTE/src/test/java/com/simnote/note/NoteServiceTest.java#L106), and [NoteServiceTest.java](SIMNOTE/src/test/java/com/simnote/note/NoteServiceTest.java#L127). |
| CR-004 | Resolved | [AppUser.java](SIMNOTE/src/main/java/com/simnote/auth/AppUser.java#L22) `usernameNormalized` length 255, matching `docs/architecture.md` (`username_normalized VARCHAR(255)`). |
| XSRF cookie gap | Resolved | [XsrfCookieTest.java](SIMNOTE/src/test/java/com/simnote/config/XsrfCookieTest.java#L27) asserts the value is non-empty (`XSRF-TOKEN=[^;]+;.*`), `SameSite=Lax`, and no `HttpOnly`. |
| I-1 | Resolved | Storage and validator both use `strip()`. Covered by [NoteServiceTest.java](SIMNOTE/src/test/java/com/simnote/note/NoteServiceTest.java#L85) (`createStripsUnicodeWhitespaceFromTitle`). |
| I-2 | Resolved for update trim; residual Low | Update trim covered by [NoteServiceTest.java](SIMNOTE/src/test/java/com/simnote/note/NoteServiceTest.java#L116). The padded 255-character boundary is tested on the create path only ([NoteServiceTest.java](SIMNOTE/src/test/java/com/simnote/note/NoteServiceTest.java#L96)). The update path shares the same validator and `strip()` call, so the residual is a test gap, not a defect. |
| I-3 | Resolved | Fixed clock `Clock.fixed(NOW, ZoneOffset.UTC)` at [NoteServiceTest.java](SIMNOTE/src/test/java/com/simnote/note/NoteServiceTest.java#L42). `createdAt` and `updatedAt` asserted at lines 106 and 127. |
| I-4 | Resolved | [XsrfCookieTest.java](SIMNOTE/src/test/java/com/simnote/config/XsrfCookieTest.java#L27) requires a non-empty token value. |

**Findings:**

Critical: none.

High: none.

Medium: none.

Low:

| ID | File and line | Issue and impact | Recommended fix |
|---|---|---|---|
| N-L1 | [AppConfig.java](SIMNOTE/src/main/java/com/simnote/config/AppConfig.java#L9) and [SecurityConfig.java](SIMNOTE/src/main/java/com/simnote/config/SecurityConfig.java#L35) | `@EnableConfigurationProperties(RememberMeProperties.class)` is declared on both classes. Spring registers the bean once, so there is no runtime defect. Impact: a second registration point that can drift if one is removed. | Keep one declaration (for example in `AppConfig`) and remove the other. |
| N-L2 | [AppUser.java](SIMNOTE/src/main/java/com/simnote/auth/AppUser.java#L33) and [UsernameNormalizer.java](SIMNOTE/src/main/java/com/simnote/service/UsernameNormalizer.java#L15) | Username normalization is implemented twice with different whitespace methods: `trim()` in `AppUser` and `strip()` in `UsernameNormalizer`. Current behavior is identical because `RegisterRequest` only permits ASCII whitespace (`\s`) and `AuthService` passes the already-normalized value. Impact: future changes to one path could make stored and looked-up usernames diverge. | Have `AppUser` accept the normalized value without re-normalizing, or call `UsernameNormalizer.normalize` in one place only. |

**Scope observation (not flagged):** Code for FR-003 (logout), FR-008, FR-009 (delete), and FR-012 is present. Per the owner decision of 2026-11-16, these are out of scope and are not flagged as scope creep.

**Missing test coverage and verification gaps:**

- The padded 255-character boundary is not tested on the update path (I-2 residual).
- No test asserts the `AppConfig` `Clock` bean type or default zone.
- Cookie headers were verified by the `XsrfCookieTest` unit-level assertion only, not by runtime request inspection.
- Not executed by this agent: `mvn test`, coverage, and runtime cookie headers. Results above are from surefire XML artifacts and are not re-verified.

**Dependency/security concerns:**

- No dependency manifest changed in the reviewed range (`pom.xml` is not in the stat). No dependency advisory check was run by this agent. Advisory verification is incomplete.
- Remember-me key: single validation path through `RememberMeProperties`. Startup failure for blank and short keys is covered by `AppConfigTest`. The placeholder in `application.properties` is accepted risk R-7 and is not a new finding.
- Login logs only the normalized username on failure, not the password.

**Recommended actions:**

1. Reconcile the test count in the PR description: surefire XML shows 95 tests, while the owner-reported count is 91.
2. Confirm that `.github/sdlc/approvals.json` and the other working-tree changes are excluded from the PR. Do not stage them as part of this change set.
3. Optional Low fixes: N-L1, N-L2, I-2 residual (update-path 255 boundary test).
4. Run a dependency advisory check and `mvn test` through the testing agent before PR creation. Neither was executed here.

## Iteration 5 (superseded by iteration 6)

**Overall status:** Changes Required

**Review scope:**

- Change set: commits `20d6e5a` (CR-002), `33df944` (CR-003 and CR-001 test), `ff2cc2c` (CR-004), and `e1ff86c` (XSRF cookie test) on `feature/task-15-readme`, range `fbf805a..e1ff86c`. Files: [SecurityConfig.java](SIMNOTE/src/main/java/com/simnote/config/SecurityConfig.java), [NoteService.java](SIMNOTE/src/main/java/com/simnote/note/NoteService.java), [AppUser.java](SIMNOTE/src/main/java/com/simnote/auth/AppUser.java), [NoteServiceTest.java](SIMNOTE/src/test/java/com/simnote/note/NoteServiceTest.java), [ApiWebMvcTest.java](SIMNOTE/src/test/java/com/simnote/config/ApiWebMvcTest.java), [XsrfCookieTest.java](SIMNOTE/src/test/java/com/simnote/config/XsrfCookieTest.java).
- Context read: [TrimmedSize.java](SIMNOTE/src/main/java/com/simnote/dto/TrimmedSize.java), [NoteRequest.java](SIMNOTE/src/main/java/com/simnote/dto/NoteRequest.java), [RememberMeProperties.java](SIMNOTE/src/main/java/com/simnote/config/RememberMeProperties.java), [AppConfig.java](SIMNOTE/src/main/java/com/simnote/config/AppConfig.java), [SameSiteCookieFilter.java](SIMNOTE/src/main/java/com/simnote/config/SameSiteCookieFilter.java), [application.properties](SIMNOTE/src/main/resources/application.properties), `docs/architecture.md`.
- Source documents: approval state carried from iteration 4 and not re-verified in this pass. Owner approval recorded 2026-11-16 (Lakshay Pahuja, project owner): architecture approver recorded in docs/architecture.md; design review approved per design-review.md; implementation plan approved (approvals.json). Approval block is cleared for the approval record; the outcome remains Changes Required until the CR findings are re-reviewed.
- Test evidence (owner-reported): `mvn -B test` in SIMNOTE before these commits, 90 tests, 0 failures, 0 errors, BUILD SUCCESS. Surefire reports in `SIMNOTE/target/surefire-reports` show `XsrfCookieTest` 1 test, 0 failures, and `NoteServiceTest` 9 tests, 0 failures, including `createStoresTrimmedTitle`. Not re-run by this agent.

**CR status for this scope:**

| ID | Status | Evidence |
|---|---|---|
| CR-001 | Partially resolved | `create()` and `update()` use `strip()` ([NoteService.java](SIMNOTE/src/main/java/com/simnote/note/NoteService.java#L36)), matching the validator ([TrimmedSize.java](SIMNOTE/src/main/java/com/simnote/dto/TrimmedSize.java#L46)). I-1 resolved in commit 8b038c9 with `createStripsUnicodeWhitespaceFromTitle`. Update path and 255 boundary untested (I-2). |
| CR-002 | Resolved | `@Value` and manual key check removed from [SecurityConfig.java](SIMNOTE/src/main/java/com/simnote/config/SecurityConfig.java); key comes from `RememberMeProperties` (`@NotBlank @Size(min = 32)`). |
| CR-003 | Resolved | `NoteService` injects `Clock` and uses `clock.instant()`; `AppConfig` provides the bean. Clock is not controlled in tests (I-3). |
| CR-004 | Resolved | [AppUser.java](SIMNOTE/src/main/java/com/simnote/auth/AppUser.java#L22) `usernameNormalized` length 255, matching `docs/architecture.md` (`username_normalized VARCHAR(255)`). `ddl-auto=update`, so the entity drives the schema. |
| XSRF cookie gap | Resolved for issuance attributes | [XsrfCookieTest.java](SIMNOTE/src/test/java/com/simnote/config/XsrfCookieTest.java#L22-L28) asserts `SameSite=Lax` and no `HttpOnly`. Value check is weak (I-4). |

**Findings:**

Critical: none.

High: none.

Medium:

| ID | File and line | Issue and impact | Recommended fix |
|---|---|---|---|
| I-1 (Resolved in 8b038c9) | [NoteService.java](SIMNOTE/src/main/java/com/simnote/note/NoteService.java#L36) `create()`; [NoteService.java](SIMNOTE/src/main/java/com/simnote/note/NoteService.java#L48) `update()`; [TrimmedSize.java](SIMNOTE/src/main/java/com/simnote/dto/TrimmedSize.java#L46) | Validator measures `value.strip().length()`, but storage uses `trim()`. `trim()` removes only characters up to U+0020, so Unicode whitespace such as U+3000 survives. A title of 255 non-space characters plus a trailing U+3000 passes validation but stores 256 characters. `title` is `VARCHAR(255)` ([AppUser.java](SIMNOTE/src/main/java/com/simnote/auth/AppUser.java) and `docs/architecture.md` line 112 "trimmed before storage"). Impact: a request accepted by validation can fail at the database and surface as a server error. Static reasoning only; not run. | Use `strip()` in `create()` and `update()` so storage matches `TrimmedSize`. Add a service test with a trailing U+3000 at 255 stripped characters. |

Low:

| ID | File and line | Issue and impact | Recommended fix |
|---|---|---|---|
| I-2 | [NoteServiceTest.java](SIMNOTE/src/test/java/com/simnote/note/NoteServiceTest.java#L70) `createStoresTrimmedTitle` | Covers `create()` only. The `update()` trim path (NoteService.java line 48) and the padded 255-character boundary requested in CR-001 are not tested. Impact: regression in update trimming would not be caught. | Add an `update()` trimming test and a padded 255-character boundary test. |
| I-3 | [NoteServiceTest.java](SIMNOTE/src/test/java/com/simnote/note/NoteServiceTest.java#L40) `Clock.systemUTC()` | The injected clock is not controlled, and no test asserts `createdAt` or `updatedAt`. Impact: CR-003 (deterministic time) is not verified by tests. | Use `Clock.fixed(...)` in the test and assert timestamps from `create()` and `update()`. |
| I-4 | [XsrfCookieTest.java](SIMNOTE/src/test/java/com/simnote/config/XsrfCookieTest.java#L26) `startsWith("XSRF-TOKEN=")` | Passes for an empty token value (`XSRF-TOKEN=;`). Impact: the test does not prove a token is issued. | Assert a non-empty value, for example a pattern matching `XSRF-TOKEN=[^;]+`. |

**Missing test coverage and verification gaps:**

- No test asserts trimming on update, the padded 255-character boundary, or Unicode whitespace (I-1, I-2).
- No test uses a fixed clock to verify timestamps (I-3).
- The XSRF test checks issuance attributes only, not token value (I-4).
- Not executed by this agent: `mvn test`, coverage, and runtime cookie headers. Results above are owner-reported or surefire evidence only.

**Dependency/security concerns:**

- Remember-me: single validation path via `RememberMeProperties`. Startup failure for blank and short keys is covered by `AppConfigTest` names. The committed placeholder in `application.properties` is accepted risk R-7, not a new finding.
- No dependency advisory check was run. Advisory verification is incomplete.

**Recommended actions:**

1. Blocker: owner records approvals for architecture, design review, and implementation plan (carried from iteration 4).
2. I-1 done (commit 8b038c9): `strip()` in `create()` and `update()`, with the Unicode whitespace test.
3. Optional Low fixes: I-2, I-3, I-4.
4. Re-review this scope after fixes, when requested.

## Iteration 4 (superseded by iteration 5)

**Overall status:** Changes Required

**Review scope:**

- Change set: commits T-01 (`360ac97`) through T-15 (`ac51896`) on `feature/task-15-readme`, base `1bac71d` (`develop`). 55 files, 3542 insertions. Read-only Git check: `SIMNOTE/` has no uncommitted changes against HEAD.
- Code reviewed: `SecurityConfig`, `AuthController`, `AuthService`, `NotesController`, `NoteService`, `Note`, `AppUser`, `ApiExceptionHandler`, `SameSiteCookieFilter`, `AppConfig`, `RememberMeProperties`, `dto/*`, `service/*`, `application.properties`, `.gitignore`. Tests were inspected by name and assertion only, not executed.
- Source documents and approval state:
  - `docs/requirements.md`: iteration 7 approved (2026-11-15). Iteration 8 items FR-001, FR-004 (255 limit), and FR-013 (username format) are marked "Proposed, pending owner confirmation, Q-E11". The code implements them, so they are not owner-approved.
  - `docs/architecture.md`: header "Approved (iteration 6)", but "Approver and date: Not recorded". Approval is unclear.
  - `docs/design-review.md`: current readiness "Pending approval". The note states that `designApproved` does not cover iteration 6. Approval is unclear.
  - `docs/implementation-plan.md`: approval record carried as unresolved from iteration 3. Not re-verified in this pass.
- Approval block: **active**. Required sources are not clearly approved, so the status is Changes Required regardless of the code result.
- Owner-reported evidence (`mvn test` 88/0, DOM check exit 0, coverage 95.6%) is not verified by this agent.

**Findings:**

Critical: none.

High: none.

Medium:

| ID | File and location | Issue and impact | Recommended fix |
|---|---|---|---|
| CR-001 | [NoteService.java](SIMNOTE/src/main/java/com/simnote/note/NoteService.java) `create()` and `update()` | Title is stored as `request.title()` without `strip()`. `TrimmedSize` validates the stripped length, but the raw value is stored. [architecture.md](docs/architecture.md) (title row) requires "trimmed before storage" (FR-004, FR-007). Impact: stored titles keep surrounding whitespace. A padded title with 255 or fewer non-space characters can exceed `VARCHAR(255)`. It then either fails with a 500 from the generic handler or is truncated, depending on the database (not verified here). No test asserts a stored trimmed title; `NoteServiceTest` uses the unpadded value `"title"`. | Store `request.title().strip()` in `create()` and `update()`. Add a service test with a padded title that asserts the stored value, and a padded 255-character boundary test. |

Low:

| ID | File and location | Issue and impact | Recommended fix |
|---|---|---|---|
| CR-002 | [RememberMeProperties.java](SIMNOTE/src/main/java/com/simnote/config/RememberMeProperties.java) and [SecurityConfig.java](SIMNOTE/src/main/java/com/simnote/config/SecurityConfig.java) `rememberMeServices()` | The remember-me key is validated twice: a `@Size(min = 32)` record and a manual `@Value` check. `RememberMeProperties` is not injected in the classes reviewed. Impact: the rule can drift, and one validation path is dead. | Keep one validation path. Either inject `RememberMeProperties` into `SecurityConfig` or remove the record and its `@EnableConfigurationProperties` registration. |
| CR-003 | [NoteService.java](SIMNOTE/src/main/java/com/simnote/note/NoteService.java) `create()` and `update()` | Timestamps use `Instant.now()`. [architecture.md](docs/architecture.md) (config row) requires a `Clock` bean for timestamps. `AppConfig` defines the `Clock` bean, but `NoteService` does not inject it. Impact: timestamps cannot be controlled in tests, and the design deviates. | Inject `Clock` into `NoteService` and use `clock.instant()`. |
| CR-004 | [AppUser.java](SIMNOTE/src/main/java/com/simnote/auth/AppUser.java) `usernameNormalized` | Entity length is 32. [architecture.md](docs/architecture.md) data model specifies `username_normalized VARCHAR(255)`. Impact: documentation and schema drift. There is no runtime defect because the username pattern caps input at 32 characters. | Align the architecture data model or the entity length, and record the decision. |

Resolved since iteration 3 (evidence in code):
- N-L2 (username trim): `RegisterRequest` pattern allows surrounding whitespace and `UsernameNormalizer` strips it.
- N-L1 (ASCII letter rule): implemented in `PasswordPolicy` as ASCII letters. Q-N9 is closed.
- F-M1 (length limits): code uses 255 and 10000, matching the owner's stated values. Owner decision recorded as "stand".

Carried iteration 2 items (F-M2 to F-M8, F-L1 to F-L7) were not re-verified in this pass and remain as recorded in iteration 2.

Security controls inspected and found consistent with the plan: CSRF enforced through `CsrfFilter` with a cookie token repository; JSON 401 and 403 entry points; BCrypt password hashing; generic login failure message; the only logged data on failed login is the username at WARN (no password); generic 500 message with stack trace logging only; `server.error.include-stacktrace=never`; H2 console disabled; `JSESSIONID` HttpOnly and SameSite via `SameSiteCookieFilter`; logout deletes the stored remember-me token and clears cookies; note ownership enforced through `findByIdAndUserId` (404 for another user's note); remember-me key length check (at least 32 characters). The committed placeholder remember-me key is an accepted risk under [architecture.md](docs/architecture.md) R-7, not a finding.

**Missing test coverage and verification gaps:**

- No test asserts that a note title is stored trimmed, and none covers a padded 255-character title (CR-001).
- The `XSRF-TOKEN` Set-Cookie attribute test is a known limitation (owner dropped it). Not a new finding.
- Tests were inspected by name and assertion only. This agent did not run `mvn test`, the DOM check, or coverage. Owner-reported results are not verified here.
- Scenarios covered by test names: duplicate username case-insensitive (409), failed login (401), `/api/auth/me` (401/200), CSRF 403, note 404 cross-user, blank/over-length title and content (400), H2 console not served, passwords not in logs, remember-me row created on login and removed on logout.

**Dependency/security concerns:**

- No dependency advisory check was run, and `pom.xml` was not reviewed for vulnerable versions in this pass. Advisory verification is incomplete.
- The committed remember-me placeholder is an accepted risk (architecture R-7). Any shared deployment must set `SIMPLENOTE_REMEMBER_ME_KEY`.

**Recommended actions:**

1. Blocker: owner records the approvals. Confirm the architecture approver and date, the design review approval for iteration 6, and the implementation plan approval. Confirm the Q-E11 items (FR-001, FR-004 limit, FR-013 format) in the requirements.
2. Blocker: fix CR-001 (store the trimmed title) and add the test that asserts it.
3. Optional Low fixes: CR-002, CR-003, CR-004.
4. Run a non-mutating dependency advisory check on `pom.xml`, and disclose its result in the next iteration.
5. Re-review the changed scope after fixes, when requested.

## Iteration 3 (superseded by iteration 4)

**Overall status:** Changes Required

**Review scope:**

- Changed files reviewed: `SIMNOTE/src/main/java/com/simnote/auth/AuthController.java` (`Credentials`, `LoginRequest`, `login()`), and `SIMNOTE/tests/dom/dom-check.mjs` (new).
- Cross-checked for evidence: `SIMNOTE/src/test/java/com/simnote/SimNoteFlowTest.java`, `index.html` and `app.js` (DOM selectors and labels used by the script), `NoteController.java`, `Note.java`, and `.github/sdlc/approvals.json`.
- Owner orchestrator evidence (reported, not re-run by this agent): `mvn -B test` BUILD SUCCESS, 1 test run, 0 failures (`SimNoteFlowTest`); `node tests/dom/dom-check.mjs` against `http://localhost:8080` printed "DOM check passed", exit 0; same script with `SIMPLENOTE_URL=http://localhost:8099` exit 1.
- Source documents:
  - `docs/requirements.md`: iteration 7, Approved (owner approval 2026-11-15). Primary acceptance source.
  - `docs/architecture.md`: status "Approved for planning"; approver and date not recorded. Approval status unclear.
  - `docs/design-review.md`: readiness recommendation "Pending approval"; the document notes `designApproved` true in `approvals.json`, but no owner approval record is in the document. Approval status unclear.
  - `docs/implementation-plan.md`: status Draft; "Project-owner approval evidence for this plan: none". Conflicts with `implementationPlanApproved=true` in `approvals.json`.
  - `.github/sdlc/approvals.json`: `requirementsApproved=true`, `designApproved=true`, `implementationPlanApproved=true`, `prApproved=false`.
- Git (read-only check): branch `develop`, HEAD `1bac71d`. `SIMNOTE/`, `docs/code-review.md`, and other docs are untracked; no task branch or commit for SIMNOTE. Process gap carried from iteration 2.
- Approval block: **active**. Architecture approver is not recorded, the implementation plan is Draft with no owner approval, and the design approval record is inconsistent. Approval cannot be established, so the outcome is Changes Required regardless of the code findings below.

**Iteration 2 items resolution status (this iteration):**

| ID | Iteration 2 finding | Iteration 3 status | Evidence |
|---|---|---|---|
| N-H1 | Login validated the registration password pattern; wrong password returned 400, not 401 | Resolved | [AuthController.java](SIMNOTE/src/main/java/com/simnote/auth/AuthController.java#L40) `LoginRequest` (`@NotBlank` only); [AuthController.java](SIMNOTE/src/main/java/com/simnote/auth/AuthController.java#L72) `login()` maps `AuthenticationException` to 401; [SimNoteFlowTest.java](SIMNOTE/src/test/java/com/simnote/SimNoteFlowTest.java#L39-L40) expects 401. Test result is orchestrator evidence only. |
| F-H3 | No DOM check (NFR-002, plan T-14) | Resolved | [dom-check.mjs](SIMNOTE/tests/dom/dom-check.mjs) present and covers the T-14 scope. Orchestrator evidence: exit 0 on 8080, exit 1 on 8099. Plan T-14 status still "Pending" in [implementation-plan.md](docs/implementation-plan.md); plan update is an owner/plan-process step. |
| F-M1 | Length limits 255 and 10000 vs FR-004 and FR-007 | Open. Owner decision DRQ-17 | [NoteController.java](SIMNOTE/src/main/java/com/simnote/note/NoteController.java#L27) `@Size(max = 255)` and `@Size(max = 10000)` unchanged |
| N-L1 | ASCII-only letter rule | Open | [AuthController.java](SIMNOTE/src/main/java/com/simnote/auth/AuthController.java#L37) `[A-Za-z]` unchanged |
| N-L2 | Username not trimmed (FR-013) | Open | [AuthController.java](SIMNOTE/src/main/java/com/simnote/auth/AuthController.java#L59) and [AuthController.java](SIMNOTE/src/main/java/com/simnote/auth/AuthController.java#L74) call `toLowerCase` only; no `strip()`/`trim()` |

Carried open items from iteration 2 (F-M2 to F-M8, F-L1 to F-L7) have no new evidence in this iteration and remain as recorded in iteration 2.

**Findings:**

Critical: none.

High: none open. N-H1 and F-H3 are resolved (see table above).

Medium:

| ID | File and line | Evidence | Impact | Recommended fix |
|---|---|---|---|---|
| F-M1 (open owner decision) | [NoteController.java](SIMNOTE/src/main/java/com/simnote/note/NoteController.java#L27); [Note.java](SIMNOTE/src/main/java/com/simnote/note/Note.java#L22-L25); [index.html](SIMNOTE/src/main/resources/static/index.html#L32-L33) | Code, DB, and UI enforce 255 and 10000. FR-004 and FR-007 state "No maximum length is defined." DRQ-17 is open. | Behavior conflicts with approved requirements until the owner decides. Plan T-05 is blocked. | Owner decides DRQ-17. Then update the requirements or remove/align the limits in DTO, DB, and UI, plus tests. This is not a code defect until the decision is made. |

Low:

| ID | File and line | Evidence | Impact | Recommended fix |
|---|---|---|---|---|
| N-L1 | [AuthController.java](SIMNOTE/src/main/java/com/simnote/auth/AuthController.java#L37) | Letter check is ASCII only; `.` excludes line terminators (OQ-R14 open). | Non-ASCII letter passwords return 400. Limited impact. | Owner confirms the definition. If Unicode letters are intended, use `\p{L}` and `(?s)`. |
| N-L2 | [AuthController.java](SIMNOTE/src/main/java/com/simnote/auth/AuthController.java#L59); [index.html](SIMNOTE/src/main/resources/static/index.html#L16) | FR-013 requires trimming; code does not trim, so " alice " fails the pattern with 400. | Minor usability issue with a workaround. | Trim the username before validation and document the normalization. |
| N-L3 (new) | [dom-check.mjs](SIMNOTE/tests/dom/dom-check.mjs#L151-L156) | The layout loop runs after the only note is deleted (step at line 148), so the list is empty during the 375 px and 1280 px overflow checks. | Overflow from long titles or content, a common layout failure, is not tested. The check passes on an almost empty page. | Before the layout loop, create a note with a long unbroken title and long content, check overflow with it present, then delete it. |
| N-L4 (new) | [dom-check.mjs](SIMNOTE/tests/dom/dom-check.mjs#L93) `await loaded`; [dom-check.mjs](SIMNOTE/tests/dom/dom-check.mjs#L21) `connect()` | No timeout on the page load wait or on CDP `send()` promises. | If a navigation or page hangs, the script blocks instead of exiting non-zero, so the failure is not reported. | Wrap `loaded` and CDP sends in a timeout that rejects, so `main()` reports the failure and exits 1. |
| N-L5 (new) | [dom-check.mjs](SIMNOTE/tests/dom/dom-check.mjs#L8) `DEBUG_PORT` default 9222 | `getPageTarget()` takes the first page on port 9222 without checking that the spawned Chrome owns it. | If another Chrome instance holds the port, the script drives that browser and navigates the user's tab. | Use a free port (or fail if the port is in use) and confirm the spawned process is the one listening. |
| N-L6 (new) | [dom-check.mjs](SIMNOTE/tests/dom/dom-check.mjs#L197-L203) | `chrome.kill()` is followed by a fixed 500 ms wait and a silent `rmSync` catch. On Windows, child Chrome processes can keep the profile locked. | Temporary profiles can accumulate with no signal. | Wait for the child `exit` event before removal, and report cleanup failure as a warning. |
| N-L7 (new) | [dom-check.mjs](SIMNOTE/tests/dom/dom-check.mjs#L118) | Each run registers a new account (`dom<stamp>`) in the target app's database and never removes it. | Repeated runs against the dev database accumulate test accounts. | Document this in the script header and README, or use a dedicated test database. |

**Missing test coverage and verification gaps:**

- `SimNoteFlowTest` is unchanged. It has no case for an unknown username at login (401), blank login fields (400), or the registration password and username negatives.
- `dom-check.mjs` does not assert the logged-out UI view at start (only `GET /api/notes` returns 401). It covers only the valid registration path, with no wrong-password or duplicate-username UI case, and no mid-session 401 (F-M6).
- The layout check runs on an empty note list (N-L3).
- Not executed by this agent: `mvn test`, the DOM check (both targets and the negative run), JaCoCo coverage (NFR-020), and runtime cookie flags. All pass/fail results are orchestrator evidence only.

**Dependency/security concerns:**

- No dependency advisory check was run in this iteration (review-only constraint). Gap disclosed; no new dependencies were added by the changed files.
- `dom-check.mjs` launches Chrome with `--remote-debugging-port` and `--remote-allow-origins=*` for the run duration. Any local page can then drive that browser through DevTools. Narrow the allowed origins if the client supports it, and keep the port bound to localhost.
- The password `domcheck1` is a test-only literal. No secrets are committed by the changed files.
- F-M2 (username rule, not in requirements) remains present in [AuthController.java](SIMNOTE/src/main/java/com/simnote/auth/AuthController.java#L35).

**Recommended actions:**

Blockers (approval cannot be established until these are resolved):

1. Owner decides DRQ-17 (F-M1) and the code, requirements, or design review is aligned with the decision.
2. Record the approver for `docs/architecture.md`, and record owner approval for `docs/implementation-plan.md` or correct `implementationPlanApproved` in `approvals.json`. Confirm `designApproved` against the design review record.
3. Create or identify the task branch from `develop` per `docs/branching-strategy.md`, and commit the SIMNOTE change set so the review scope is traceable (process gap).

Before PR (non-blocking, Low):

4. Address N-L1 and N-L2 per owner decision.
5. Address N-L3 to N-L7 in `dom-check.mjs`.
6. Carry-over items F-M2 to F-M8 and F-L1 to F-L7 per iteration 2.
7. Request re-review after items 1 to 3 are complete.

---

# History: Iteration 2

**Iteration 2 status:** Changes Required (superseded by iteration 3)

**Overall status:** Changes Required

**Review scope:**

- Re-review of the SIMNOTE working tree after iteration 1 fixes. Read-only inspection only; no production code, tests, or configuration were changed; no tests, app, or builds were executed.
- Files inspected: all 17 SIMNOTE files listed in iteration 1, plus `docs/requirements.md`, `docs/architecture.md`, `docs/design-review.md`, `docs/implementation-plan.md`, `.github/sdlc/approvals.json`, and the root `.gitignore`.
- Process gap (carried): no git commits or task branch exist for SIMNOTE per the owner's note. The change set cannot be traced to a task branch or PR as required by `docs/branching-strategy.md`.
- Approval state: `approvals.json` flags are `requirementsApproved=true`, `designApproved=true`, `implementationPlanApproved=true`, `prApproved=false`. Document-level evidence conflicts with these flags: the architecture approver is not recorded ("Approved for planning"), and the implementation plan is status Draft with the owner-approval checkbox unchecked. Approval status is unclear, which blocks approval independently of the findings below.
- Open decisions: DRQ-17 (length limits; requirements FR-004 and FR-007 state no maximum, while the code and architecture enforce 255 and 10000) and DRQ-18 (Spring Boot support line).
- Verification limits: Java compile correctness of `AuthController.java` was checked by inspection only (imports, record annotations, regex escaping, and Spring Security 6 API usage). Runtime behavior is inferred from code, not executed.

**Iteration 1 resolution status:**

| ID | Iteration 1 finding | Iteration 2 status | Evidence |
|---|---|---|---|
| F-H1 | Password rule (8+ chars, letter and digit) | Resolved for registration. Login side effect logged as N-H1 | [AuthController.java](SIMNOTE/src/main/java/com/simnote/auth/AuthController.java#L36-L37) |
| F-H2 | Session restore via `/api/auth/me` | Resolved | [AuthController.java](SIMNOTE/src/main/java/com/simnote/auth/AuthController.java#L74-L77); [app.js](SIMNOTE/src/main/resources/static/app.js#L142) startup call |
| F-H3 | Automated DOM checks (NFR-002) and `tests/dom/` | Open. Blocks approval (see Findings) | `tests/dom/` absent in SIMNOTE |
| F-M1 | Length limits 255 and 10000 vs FR-004/FR-007 | Open. Owner decision DRQ-17 required | [NoteController.java](SIMNOTE/src/main/java/com/simnote/note/NoteController.java#L27); [Note.java](SIMNOTE/src/main/java/com/simnote/note/Note.java#L22-L25); [index.html](SIMNOTE/src/main/resources/static/index.html#L32-L33) |
| F-M2 | Username rule (requirements silent) | Open | [AuthController.java](SIMNOTE/src/main/java/com/simnote/auth/AuthController.java#L35) |
| F-M3 | Layering (services, DTOs) | Open | Controllers inject repositories directly; no service or DTO package |
| F-M4 | Error response body (`ApiErrorResponse`) | Open | No exception handler; [SecurityConfig.java](SIMNOTE/src/main/java/com/simnote/config/SecurityConfig.java) entry point writes no body |
| F-M5 | Logging | Open | No logger in any SIMNOTE file |
| F-M6 | 401 handling in UI and focus re-check | Open | [app.js](SIMNOTE/src/main/resources/static/app.js#L142): `/api/auth/me` runs only at startup; no focus listener |
| F-M7 | Spring Boot support line (DRQ-18) | Open | [pom.xml](SIMNOTE/pom.xml#L11) version 3.4.1 |
| F-M8 | Test coverage gaps | Open | [SimNoteFlowTest.java](SIMNOTE/src/test/java/com/simnote/SimNoteFlowTest.java) is a single test method (see Missing test coverage) |
| F-L1 | Register auto-logs in (architecture Decision 18) | Open | [app.js](SIMNOTE/src/main/resources/static/app.js) register path calls login |
| F-L2 | Title and username not trimmed | Open | [NoteController.java](SIMNOTE/src/main/java/com/simnote/note/NoteController.java#L27) |
| F-L3 | Package, table, and schema naming vs architecture | Open | Packages `com.simnote.*`; table `app_users`; no FK on notes |
| F-L4 | Length counted in characters, not bytes | Open | [AuthController.java](SIMNOTE/src/main/java/com/simnote/auth/AuthController.java#L37) |
| F-L5 | `data/` not git-ignored | Open | Root [.gitignore](.gitignore) has no `data/` entry |
| F-L6 | README missing | Open | Not present in SIMNOTE |
| F-L7 | H2 console and error-detail properties not explicit | Open | [application.properties](SIMNOTE/src/main/resources/application.properties) |

**Findings:**

Critical: none.

| ID | Severity | File and line | Evidence | Impact | Recommended fix |
|---|---|---|---|---|---|
| N-H1 | High | [AuthController.java](SIMNOTE/src/main/java/com/simnote/auth/AuthController.java#L55) (`login` with `@Valid`); [AuthController.java](SIMNOTE/src/main/java/com/simnote/auth/AuthController.java#L37) (password `@Pattern`); [SimNoteFlowTest.java](SIMNOTE/src/test/java/com/simnote/SimNoteFlowTest.java#L39-L40) | `login` validates the shared `Credentials` record, which applies the registration password pattern (letter and digit) and the username pattern. The test sends `wrong-password` (no digit) and expects 401. Validation failure returns 400 by default. Architecture Decision 6 says login does not re-check the password policy. FR-002 says the password standard applies to registration. | The existing test is expected to fail under `mvn test` (static analysis; not executed). Any login with a non-conforming password or username returns 400 instead of 401, so the bad-credential contract is wrong. | Split into a `RegisterRequest` with the full rules and a `LoginRequest` with `@NotBlank` only. Return 401 for all failed logins. Keep the test expectation at 401. |
| F-H3 | High | `tests/dom/` (absent); plan T-14, T-16, Definition of Done | No DOM check script exists. NFR-002 requires automated DOM checks. The plan's Definition of Done requires "DOM check passes against the running app". | Required verification is missing. This blocks approval. | Add the DOM check under `tests/dom/` per T-14 and record a passing run by the testing agent. |

| ID | Severity | File and line | Evidence | Impact | Recommended fix |
|---|---|---|---|---|---|
| F-M1 | Medium | [NoteController.java](SIMNOTE/src/main/java/com/simnote/note/NoteController.java#L27); [Note.java](SIMNOTE/src/main/java/com/simnote/note/Note.java#L22-L25); [index.html](SIMNOTE/src/main/resources/static/index.html#L32-L33) | Code enforces title 255 and content 10000. FR-004 and FR-007 state "No maximum length is defined." Architecture and DR-028 define design limits. This is a requirements conflict, not a silent gap. | Behavior conflicts with approved requirements. Plan T-05 is blocked by DRQ-17. | Owner decides DRQ-17. Then either update requirements or remove/adjust the limits and tests. Do not treat the code as approved until resolved. |

| ID | Severity | File and line | Evidence | Impact | Recommended fix |
|---|---|---|---|---|---|
| N-L1 | Low | [AuthController.java](SIMNOTE/src/main/java/com/simnote/auth/AuthController.java#L37) | Letter check `[A-Za-z]` is ASCII only. `.` excludes line terminators. Letter and digit definitions are open (OQ-R14). | Non-ASCII letter passwords are rejected with 400. Limited impact. | Confirm the definition with the owner. If Unicode letters are intended, use `\p{L}` and `(?s)`. |
| N-L2 | Low | [AuthController.java](SIMNOTE/src/main/java/com/simnote/auth/AuthController.java#L35); [index.html](SIMNOTE/src/main/resources/static/index.html#L16) | FR-013 says the username is trimmed. The code does not trim, so " alice " fails the pattern with 400. | Minor usability issue with a workaround. | Trim before validation and document the normalization. |

Medium and Low carried findings (F-M2 to F-M8, F-L1 to F-L7) remain open as listed in the resolution table. They do not add new evidence beyond iteration 1.

**Missing test coverage and verification gaps:**

- The existing `SimNoteFlowTest` is one method. It does not cover `GET /api/auth/me` (200 and 401), registration password-policy negatives (7 characters, no letter, no digit), username negatives, title and content boundary cases (255 and 10000), or blank-title 400 responses.
- N-H1 is a static-analysis finding. The test at line 39-40 is expected to fail. This agent did not execute `mvn test`.
- No DOM checks exist (F-H3).
- Not executed by this agent: `mvn test`, JaCoCo report, app startup, runtime cookie headers (SameSite and HttpOnly on Tomcat-managed session cookies), and remember-me revocation.

**Dependency/security concerns:**

- Dependency advisory checks were not run in this iteration. Status: incomplete. Pin Spring Boot 3.4.1 (F-M7) remains open pending DRQ-18.
- Remember-me key: the default is blank. [SecurityConfig.java](SIMNOTE/src/main/java/com/simnote/config/SecurityConfig.java) rejects keys under 32 characters, so startup fails fast. Acceptable, per inspection.
- Password hashing is BCrypt. CSRF is cookie-based and the XSRF-TOKEN cookie is not HttpOnly, by design in the architecture.
- H2 console and error-detail settings are not explicit (F-L7).

**Recommended actions:**

1. Fix N-H1: split login and register request models, return 401 for failed login, and align the test.
2. Add the DOM check under `tests/dom/` per plan T-14 (F-H3).
3. Owner decides DRQ-17 (F-M1), then align code or requirements.
4. Resolve approval conflicts: record the architecture approver, update the implementation plan status or owner-approval checkbox, and reconcile `approvals.json`.
5. Commit SIMNOTE to a task branch per `docs/branching-strategy.md` (process gap).
6. Address carried Medium and Low items (F-M2 to F-M8, F-L1 to F-L7, N-L1, N-L2).
7. Request re-review after fixes.

Approval remains blocked until items 1 to 4 are complete.

---

# History: Iteration 1

**Iteration 1 status:** Changes Required (superseded by iteration 2)

**Review scope (iteration 1):**

Change set reviewed:
- Location: `SIMNOTE/` (untracked on branch `develop`, HEAD `1bac71d`). Read-only `git status` shows no task branch and no commits for SIMNOTE. This is a process gap: the change set cannot be traced to an approved task branch or PR as required by `docs/branching-strategy.md`. Review was performed on the working-tree files.
- Files reviewed (17): `SIMNOTE/pom.xml`; `src/main/java/com/simnote/SimNoteApplication.java`; `config/SecurityConfig.java`; `config/SameSiteCookieFilter.java`; `auth/AuthController.java`, `auth/AppUser.java`, `auth/AppUserDetailsService.java`, `auth/AppUserRepository.java`; `note/NoteController.java`, `note/Note.java`, `note/NoteRepository.java`; `src/main/resources/application.properties`, `schema.sql`, `static/index.html`, `static/app.js`, `static/style.css`; `src/test/java/com/simnote/SimNoteFlowTest.java`.
- Not found in SIMNOTE: service layer, DTO package, exception handler, `api.js`, `tests/dom/`, README, and any logging.

Source documents and approval state:
- `docs/requirements.md` (iteration 7): approved by the project owner 2026-11-15; `approvals.json` `requirementsApproved=true`. Used as the primary acceptance source.
- `docs/architecture.md` (iteration 5): status "Approved for planning"; approver is not recorded. Approval status is unclear, which blocks approval.
- `docs/design-review.md` (iteration 4): used as reference. DR-027 (Spring Boot support line), DR-028 (length limits vs FR-004), DR-029, DR-030 are open or non-blocking.
- `docs/implementation-plan.md` (iteration 1): status Draft; owner-approval checkbox unchecked. `approvals.json` shows `implementationPlanApproved=true`. The document and the flag disagree, so approval status is unclear. T-05 is blocked by DRQ-17.
- Open decisions: DRQ-17 (length limits) and DRQ-18 (Spring Boot support line) remain open.
- `.github/sdlc/approvals.json`: `prApproved=false`.

Approval block: the architecture approver is not recorded and the plan's approval status is unclear. This review is therefore partial with respect to architecture and plan compliance. Approval is blocked independently of the findings below.

Package divergence: architecture specifies `com.simplenote.*`; code uses `com.simnote.*` (`pom.xml` groupId `com.simnote`, artifactId `simplenote`).

Verification limits: this review did not compile, run tests, run the app, or run dependency advisory checks, per review-only constraints. Statements about runtime behavior below are from code inspection.

Requirement coverage summary (by inspection):

| Requirement | Status | Note |
|---|---|---|
| FR-001 password rule (8+, letter and digit) | Not met | Only length 8 to 72 enforced (F-H1) |
| FR-002 login persists across reloads | Partly met | Server-side remember-me exists; UI restore is broken (F-H2) |
| FR-003 / FR-015 logout and 401 when logged out | Met by inspection | Covered by test |
| FR-004 / FR-007 no maximum length | Not met | 255 and 10000 limits applied with DRQ-17 open (F-M1) |
| FR-004 / FR-007 / FR-009 / FR-010 note CRUD | Implemented | Update and delete not tested (F-M8) |
| FR-013 username uniqueness case-insensitive | Met by inspection | Lowercased; 409 on duplicate; test covers exact duplicate only |
| FR-021 other user's note returns 404 | Met by inspection | `findByIdAndOwnerId`; covered by test |
| FR-022 7-day remember-me; 401 after expiry or logout | Met by inspection | Expiry not tested |
| NFR-002 automated DOM checks | Not met | No `tests/dom/` or DOM check (F-H3) |
| NFR-007 no hash, session id, or stack trace in responses | Met by inspection | Not asserted in tests |
| NFR-008 cookie HttpOnly and SameSite=Lax | Partly verified | Remember-me asserted; JSESSIONID not asserted |
| NFR-009 CSRF 403 | Partly met | CSRF enabled; no test for missing or invalid token |
| NFR-010 H2 console disabled | Met by default | Not set explicitly; not tested |
| NFR-011 / NFR-012 validation and status codes | Partly met | 400, 401, 404, 409 present; 403 untested |
| NFR-013 one consistent JSON error format | Not met | 401 has no body; no `ApiErrorResponse` (F-M4) |
| NFR-014 controller, service, repository layers | Not met | Controllers call repositories directly (F-M3) |
| NFR-015 DTOs, not entities | Partly met | Records used for responses; no DTO package |
| NFR-016 no committed secrets | Met by inspection | Remember-me key from environment; blank default fails startup |
| NFR-017 / NFR-018 logging | Not met | No logging anywhere (F-M5) |
| NFR-019 JaCoCo report | Configured | Bound to `test` phase in `pom.xml` |
| NFR-020 80% line coverage | Not verified | Not measured by this review |
| NFR-021 README | Not met | No README in SIMNOTE (F-L6) |

## Findings

### Critical
None identified by inspection.

### High

| ID | Location | Evidence and impact | Recommended fix |
|---|---|---|---|
| F-H1 | [AuthController.java](SIMNOTE/src/main/java/com/simnote/auth/AuthController.java#L35) `Credentials.password` | Only `@Size(min = 8, max = 72)` is applied. FR-001 requires at least one letter and at least one digit. A password such as `abcdefgh` is accepted at registration, violating an approved requirement. | Add a `PasswordPolicy` check (at least one letter and one digit) on register, return 400 with the standard error body, and add positive and negative tests. |
| F-H2 | [app.js](SIMNOTE/src/main/resources/static/app.js#L142) startup block, `GET /api/auth/me` | No `/api/auth/me` endpoint exists in any controller (grep confirmed). For a logged-in user, the request returns 404, so `result.ok` is false and `showAuth()` runs. A reload shows the login form although the session is valid, which breaks FR-002 (login persists across reloads). | Add `GET /api/auth/me` returning `{username}` for authenticated users and 401 otherwise. Add a DOM check and a Java test for it. |
| F-H3 | Repository tree (`SIMNOTE/tests/dom/` absent); [SimNoteFlowTest.java](SIMNOTE/src/test/java/com/simnote/SimNoteFlowTest.java) | NFR-002 requires automated DOM rendering checks (headless Chrome per the plan). None exist, so frontend behavior, including F-H2, is untested. | Add the `tests/dom/` headless Chrome check per T-14 covering login view, notes list, edit, delete, and logged-out state. |

### Medium

| ID | Location | Evidence and impact | Recommended fix |
|---|---|---|---|
| F-M1 | [NoteController.java](SIMNOTE/src/main/java/com/simnote/note/NoteController.java#L27) `NoteRequest`; [Note.java](SIMNOTE/src/main/java/com/simnote/note/Note.java) column lengths; [index.html](SIMNOTE/src/main/resources/static/index.html) `maxlength` | FR-004 and FR-007 state "No maximum length is defined" (owner answer Q-E5). The code enforces 255 (title) and 10000 (content) in DTO, DB, and UI, while DRQ-17 is open and plan T-05 is blocked. Users with longer text are rejected against approved text. | Obtain the owner decision on DRQ-17. Until then, remove the limits to match FR-004 or record the owner decision and update the requirement or design review. Keep DTO, DB, and UI limits consistent. |
| F-M2 | [AuthController.java](SIMNOTE/src/main/java/com/simnote/auth/AuthController.java#L34) `Credentials.username` | Username rule (3 to 32 characters, letters, digits, underscore) is not in the requirements. Architecture states no format rule is specified (max 255 in the data model). This restricts accepted usernames without an approved source. | Obtain owner approval for the username rule and record it in the requirements or architecture, or remove it. Align `index.html` `minlength`/`maxlength` with the decision. |
| F-M3 | [NoteController.java](SIMNOTE/src/main/java/com/simnote/note/NoteController.java#L37-L38), [AuthController.java](SIMNOTE/src/main/java/com/simnote/auth/AuthController.java#L38) | NFR-014 states controllers shall not access repositories directly. Both controllers inject repositories. Architecture components `AuthService`, `NoteService`, `PasswordPolicy`, `UsernameNormalizer`, `TrimmedSize`, and `Clock` are absent. `Instant.now()` is called directly ([NoteController.java](SIMNOTE/src/main/java/com/simnote/note/NoteController.java#L55)). | Add a service layer per architecture, move repository access and password, username, and time logic into it, and inject a `Clock`. |
| F-M4 | [SecurityConfig.java](SIMNOTE/src/main/java/com/simnote/config/SecurityConfig.java#L53-L54) `authenticationEntryPoint`; no exception handler in the tree | The 401 entry point only sets the status; it writes no body. Other errors rely on Spring Boot defaults. No `ApiErrorResponse` or `ApiExceptionHandler` exists, so NFR-013 (one consistent JSON error format) is not met. The CSRF 403 body is not defined or tested. | Add an `ApiExceptionHandler` and `ApiErrorResponse` record per architecture. Write a JSON body from the 401 entry point and an access-denied handler for 403. Add tests asserting the body shape. |
| F-M5 | Whole backend; [AuthController.java](SIMNOTE/src/main/java/com/simnote/auth/AuthController.java) `login()` catch block | No logger is used anywhere (grep confirmed). Failed logins and unhandled errors are not logged, violating NFR-017. NFR-018 (no password or hash in logs) cannot be checked. | Log failed login attempts (username only, no password) and unhandled exceptions via a central handler. Add a test or log review check for NFR-018. |
| F-M6 | [app.js](SIMNOTE/src/main/resources/static/app.js) `loadNotes()` and `deleteNote()`; no focus listener | A 401 during note load or delete yields an empty list or an error message and never returns the user to the login view. Architecture requires a return to login on any 401 and a session re-check on window focus. The user may keep acting on a logged-out UI. | On any 401 response, call `showAuth()`. Add a focus-triggered session check per architecture. Cover with a DOM check. |
| F-M7 | [pom.xml](SIMNOTE/pom.xml#L10) parent `spring-boot-starter-parent` 3.4.1 | Architecture T-01 requires the newest supported 3.x line, and the pin is an older patch. DR-027 (support line) and DRQ-18 are open. Support status was not verified by this review (no network check performed). | Confirm the supported 3.x line with the owner under DRQ-18 and update the parent version, or record the decision. |
| F-M8 | [SimNoteFlowTest.java](SIMNOTE/src/test/java/com/simnote/SimNoteFlowTest.java) (single test method) | Note update (FR-007), delete (FR-009/FR-010), 400 validation, missing-note 404, 403 CSRF (NFR-009), JSESSIONID cookie flags (NFR-008), and absence of password, hash, and stack trace in responses (NFR-007) are not tested. See the coverage section. | Add tests for each scenario listed under missing coverage. |

### Low

| ID | Location | Evidence and impact | Recommended fix |
|---|---|---|---|
| F-L1 | [app.js](SIMNOTE/src/main/resources/static/app.js) `register()` | Registration calls `login()` automatically. Architecture Decision 18 states registration does not create a session, and DR-022 is open. This UX choice is undocumented. | Obtain the owner decision and document it, or change registration to show the login form. |
| F-L2 | [NoteController.java](SIMNOTE/src/main/java/com/simnote/note/NoteController.java#L27), [Note.java](SIMNOTE/src/main/java/com/simnote/note/Note.java#L37) | Title and content are stored untrimmed. Architecture requires trimming before storage. `@NotBlank` correctly rejects whitespace-only titles. | Trim title (and content as specified) before storage and test it. |
| F-L3 | Package names across `SIMNOTE/src/main/java/com/simnote/**` | Architecture uses `com.simplenote.*`; code uses `com.simnote.*`. Table name `app_users` ([AppUser.java](SIMNOTE/src/main/java/com/simnote/auth/AppUser.java#L11)) differs from architecture `users`. No `username_normalized` column and no FK on `notes.user_id` ([Note.java](SIMNOTE/src/main/java/com/simnote/note/Note.java#L19)). | Decide the canonical package and table names and update architecture or code. Add the FK and the `(user_id, updated_at)` index per T-03. |
| F-L4 | [Credentials.password](SIMNOTE/src/main/java/com/simnote/auth/AuthController.java#L35) | Length is checked in characters (max 72), while BCrypt limits input to 72 bytes. Behavior for multi-byte passwords is not verified. | Enforce the 72-byte limit in bytes (or document the rule) and add a test. |
| F-L5 | Root `.gitignore` | No entry for `data/`. `spring.datasource.url=jdbc:h2:file:./data/simplenote` writes a database file containing user hashes and notes. Architecture T-02 requires it to be git-ignored. | Add `SIMNOTE/data/` to `.gitignore`. |
| F-L6 | `SIMNOTE/` root | No README (NFR-021 requires prerequisites, run command, and test command). | Add `SIMNOTE/README.md` covering JAVA_HOME and remember-me key setup. |
| F-L7 | [application.properties](SIMNOTE/src/main/resources/application.properties) | H2 console and error-detail settings are not set explicitly (defaults are safe). Architecture requires explicit settings. | Set `spring.h2.console.enabled=false` and `server.error.include-*=never` explicitly. |

## Missing test coverage and verification gaps

Inspected tests cover only one `@SpringBootTest` method (`SimNoteFlowTest`). They cover: duplicate registration (409); wrong password (401); remember-me cookie `HttpOnly` and `SameSite=Lax`; note create, get, cross-user 404; unauthenticated list 401; logout 204; remember-me replay after logout 401.

Not covered by inspected tests:
- Password rule negative cases (no digit, no letter) (F-H1).
- Note update (PUT) and delete (DELETE) for owner and other user (FR-007, FR-009/FR-010, FR-021).
- Missing note id 404 (not only cross-user).
- Validation 400 for empty or overlong title and invalid username.
- Missing or invalid CSRF token returns 403 (NFR-009). All tests use `csrf()` post-processor, which bypasses real token checks.
- `JSESSIONID` cookie attributes (NFR-008).
- Absence of password, hash, session id, and stack trace in responses (NFR-007).
- Case-insensitive duplicate username (FR-013).
- Session expiry after 7 days (FR-022); remember-me expiry not tested.
- Logging assertions (NFR-017/018).
- Frontend behavior of any kind (NFR-002, F-H3).
- Startup failure when the remember-me key is blank or under 32 characters (T-02).

Verification not performed by this review (must be run by the testing agent):
- `mvn test` build and test execution; JaCoCo report generation (NFR-019) and 80% line coverage on service and controller code (NFR-020).
- Application startup on JDK 27 (CON-006 build gate, R-1).
- Behavior of `SameSiteCookieFilter` for cookies written by logout (`deleteCookies`) and by Tomcat for `JSESSIONID`. Only `addCookie` is wrapped; the cookie-clearing path is unverified.
- Whether `PersistentTokenBasedRememberMeServices.logout` revokes all tokens for the user, as the test asserts.
- Actual error body shapes for 401 and 403 responses.

## Dependency/security concerns

- Dependencies were not checked for known vulnerabilities. No advisory check was run, to preserve the review-only constraint. This is a disclosed gap.
- Spring Boot 3.4.1 is pinned (F-M7). Support status was not verified.
- No secrets are committed: the remember-me key comes from `SIMPLENOTE_REMEMBER_ME_KEY` with a blank default that fails startup. The test key in `SimNoteFlowTest.java` is a test-only literal.
- Positive security observations by inspection: BCrypt password hashing; ownership check via `findByIdAndOwnerId`; CSRF enabled with cookie repository; session fixation mitigated via `changeSessionId`; frontend uses `textContent` only (no `innerHTML`), so no XSS sink was found in `app.js`.
- The remember-me key startup check is present but does not refuse a committed placeholder (DR-030, not adopted).
- No rate limiting on login. No requirement states one, so it is not raised as a finding.

## Recommended actions

Blockers before PR creation (in priority order):
1. Fix F-H2: add `GET /api/auth/me` and verify the startup session restore.
2. Fix F-H1: enforce the approved password rule (letter and digit) with tests.
3. Address F-H3: add the automated DOM check required by NFR-002.
4. Resolve DRQ-17 and align F-M1 (length limits) with the owner decision; do not leave limits implemented while the decision is open.
5. Obtain owner approval or removal for the username rule (F-M2).
6. Record the approver for `docs/architecture.md` and reconcile the implementation-plan approval status with `approvals.json`.
7. Create a task branch from `develop` per `docs/branching-strategy.md` and commit the SIMNOTE change set so it can be reviewed and traced (process gap).

Required before approval (non-blocking for this round but needed for requirement compliance):
8. Add the service layer and error handling per F-M3 and F-M4; add logging (F-M5); return to login on 401 (F-M6).
9. Expand tests per the missing coverage section (F-M8).
10. Confirm the Spring Boot support line under DRQ-18 (F-M7).
11. Address Low findings F-L1 to F-L7 as scheduled.

After fixes are made, request a re-review of the changed scope. The overall status remains Changes Required until the blockers above are resolved and approval sources are confirmed.
