# Design Review

## Architecture Iteration 6 Impact Note (2026-11-15)
- Owner decision (2026-11-16, Lakshay Pahuja, project owner): approved. designApproved in approvals.json is true. The Pending approval readiness below applies to iteration 4 history only.
- Trigger: docs/requirements.md iteration 8 and docs/architecture.md iteration 6 (Draft). Owner re-approval of requirements is pending.
- Status: Re-review required. The Iteration 4 Pending approval outcome below applies to architecture iteration 5 only. The designApproved approval in .github/sdlc/approvals.json does not cover iteration 6. No re-review was run in this note; the outcome is not changed here.
- Changes affecting the design:
  - Username (FR-013): 3 to 32 characters after trim, letters A-Z, a-z, digits, and underscore (Proposed, Q-E11). Extends DR-009 (trim and case-insensitive match, owner-accepted); the format check is new and needs review.
  - Password letter (FR-001): ASCII A-Z and a-z only (Proposed, Q-N9). Relates to DR-022 and DRQ-14 item 4 (digit and space definition).
  - Title maximum (FR-004, FR-007): 255 kept as a design value, pending Q-E11. DR-028 is partly answered: the owner stated "10000 is max" for content; the title limit is still open.
- Open items to confirm: Q-E11 (title maximum and username format) and Q-N9 (letter definition). Owner decisions are required before the next review.
- Owner risk acceptance (2026-11-15, option 2): the Iteration 4 review outcome is accepted for architecture iteration 6 without re-review. The three changes above (username format, letter rule, title limit) were not reviewed by design-review-agent. Recorded as owner risk acceptance; not a design review pass. designApproved in approvals.json was not modified.
- Unchanged: remember-me DRQ-16 option A and the Iteration 4 findings and decisions, which remain recorded as history.

## Iteration 4 Review Context (2026-11-15)
- Review iteration: 4. Scope: mandatory checks only (requirement coverage, security, data flow and API, persistence, testability, technology feasibility, blocking open decisions). Cosmetic and nice-to-have items are not reviewed.
- Source documents: docs/requirements.md iteration 7 (Approved - owner approval 2026-11-15, iteration 7). docs/architecture.md iteration 5 (Draft, pending design review and owner approval).
- Read only, not modified: docs/sdlc-status.md, .github/sdlc/approvals.json.
- Reopened: the iteration 3 outcome (Pending approval) applied to architecture iteration 4. It does not apply to iteration 5, which replaces the browser-only design (LocalStorage, SHA-256) with a Spring Boot, H2, and server-session design.
- Owner-delegated defaults accepted and not re-opened: Spring Boot 3.x, Java 21, Maven, H2 file mode, BCrypt, vanilla JS with fetch, same-origin static serving, headless Chrome for DOM checks, 7-day login persistence (FR-022), NFR-020 coverage target.
- Missing inputs: none.
- Not performed: no build, no test run, no browser run, no online check of Spring Boot support dates.

## Iteration 4 Executive Summary
- Overall assessment: the component structure, package layers, DTO separation, API table, data model, and test plan are consistent with requirements iteration 7. One mandatory defect blocks readiness: the remember-me design cannot revoke a login on logout, so FR-022 ("after logout, 401") is not met.
- Principal risk: none blocking. DR-026 resolved by DRQ-16 option A (persistent remember-me tokens, deleted on logout).
- Readiness recommendation: **Pending approval**. Owner sets designApproved after review.

## Iteration 4 Mandatory Check Results
| Check | Result | Evidence and Finding |
|-------|--------|----------------------|
| 1. Requirement coverage | Pass | All 18 active FR (FR-001 to FR-010, FR-012 to FR-015, FR-019 to FR-022), all 19 active NFR (NFR-001 to NFR-003, NFR-006 to NFR-021), CON-003, CON-005 to CON-009, and BR-001 appear in the architecture Requirement Traceability table. Superseded items are listed as not designed. |
| 2. Security | Pass | Pass: BCrypt; server-side session; CSRF with 403; ownership by user_id and 404 for another user's note; H2 console disabled; no password, hash, or stack trace in error bodies or logs. DR-026 resolved (DRQ-16 option A): logout deletes the persistent remember-me token. Note: the architecture sentence on the signature including the password hash no longer applies to persistent tokens; treat as non-blocking wording fix. |
| 3. Data flow and API | Pass | All nine endpoints list method, path, auth, and status codes. Applicable 400, 401, 403, 404, and 409 codes are present (for example, 409 on register; 404 on GET, PUT, and DELETE by id; 403 on state-changing requests). |
| 4. Persistence | Pass | H2 file mode (jdbc:h2:file:./data/simplenote). Schema management decided: ddl-auto=update (Decision 4, Data Model). Data ownership: notes.user_id is an FK; every note query filters on it. The DR-026 fix adds one table. |
| 5. Testability | Pass | JUnit 5 and Mockito unit tests; @WebMvcTest and MockMvc; @SpringBootTest integration tests; JaCoCo bound to the test phase; headless Chrome DOM check in tests/dom/ with Node 22 built-in WebSocket and no npm. Includes a replay-after-logout remember-me test (DR-026 fix, DRQ-16 option A). |
| 6. Technology feasibility | Pass, with gates | Spring Boot 3.x, Java 21 release, Maven. R-1 (JDK 27 compatibility) is recorded as a build gate, not a design blocker. Support status of the pinned 3.4.x line is open (DR-027). |
| 7. Open decisions | None blocking | Architecture Open Questions lists none. DRQ-16 decided (option A). DRQ-17 and DRQ-18 are not blocking. |

## Iteration 4 Findings
| ID | Severity | Type | Source / Requirement | Evidence and Impact | Recommendation | Status / Resolution |
|----|----------|------|----------------------|---------------------|----------------|---------------------|
| DR-026 | High | Security: logout and session revocation (blocking) | FR-003; FR-022; FR-015; CON-008; NFR-008 | Architecture Security Design (Remember-me) and Decision 5 use Spring TokenBasedRememberMeServices. That token is stateless: a signature over username, expiry, and the stored password hash, checked on every request. Logout only clears the browser cookie. Nothing is stored on the server to revoke. A copied remember-me cookie is therefore accepted until its 7-day expiry or a password change. FR-022 requires the server to reject note requests with 401 after logout, FR-003 requires logout to end the server-side session, and CON-008 requires a server-side session. The Testing Design has no replay test. | Exact fix (proposed, not applied): (1) Replace TokenBasedRememberMeServices with PersistentTokenBasedRememberMeServices backed by a JDBC token repository. (2) Add table persistent_logins (series, username, token, last_used) to the Data Model; it is created under the Decision 4 ddl-auto=update rule or by a matching script. (3) Logout deletes the series token on the server and clears the cookie. (4) Add the integration test: a remember-me cookie captured before logout returns 401 after logout. (5) Update Decision 5, Security Design, Drivers, Traceability (FR-003, FR-022), and Testing Design. This reverses the rejected option B in Technology Stack (Remember-me), because FR-003 and FR-022 take precedence. Needs owner agreement (DRQ-16). | Resolved. DRQ-16 option A agreed by owner (2026-11-15); architecture iteration 5 applies the fix. |
| DR-027 | Medium | Technology feasibility (support status) | CON-006; R-1 | CON-006 requires the "current stable 3.x line". The architecture pins Spring Boot 3.4.x. The reviewer recalls that 3.4 and 3.5 OSS support ended before 2026-11-15, but this was not verified online. An unsupported pin is a maintenance risk, not a design blocker. | Verification of the support schedule in the R-1 gate; pin the newest supported 3.x line, or record owner acceptance of 3.4.x (DRQ-18). | Open. Non-blocking. |
| DR-028 | Medium | Requirements conflict: length limits | FR-004; FR-007; Q-E5 | Architecture imposes a title limit of 255 characters (TrimmedSize, VARCHAR(255)) and a content limit of 10,000 characters (@Size, VARCHAR(10000)). Q-E5 (owner, 2026-11-14) states "No maximum length is defined." The architecture calls these design limits but does not mark them as an exception to the requirement. A user with a longer title is rejected. | Owner decides (DRQ-17): accept the limits as design limits, with the requirement text left as is, or remove them (unbounded text storage for title and content). | Open. Non-blocking pending owner decision. |
| DR-029 | Low | Testability wording | NFR-007; NFR-008 | Testing Design asserts "no password, hash, session id, or stack trace in any response". Login must set JSESSIONID in Set-Cookie (NFR-008), so this assertion fails as written. NFR-007 and the mandatory scope cover error and response bodies. | Scope the NFR-007 test to response and error bodies; check Set-Cookie for attributes only. | Open. Non-blocking. |
| DR-030 | Low | Secrets handling | NFR-016; R-7; NFR-006 | The committed remember-me placeholder is accepted at startup if it is 32 characters or longer, so a deployment that omits SIMPLENOTE_REMEMBER_ME_KEY runs with a key that is in the repository. | Refuse to start when the configured key equals the committed placeholder. | Open. Non-blocking. |

## Iteration 4 Clarifying Questions
| ID | Question | Decision / Finding Affected | Blocking? | Answer / Status |
|----|----------|-----------------------------|-----------|-----------------|
| DRQ-16 | Logout and remember-me (DR-026). Options: (a) persistent server-side token table, deleted on logout (recommended; keeps 7-day persistence and the server-side revocation the requirements need); (b) keep the stateless token and change FR-022 to accept replay until expiry (requires a requirements change by the owner). | DR-026; FR-003, FR-022 | Yes | Open. Owner decision. |
| DRQ-17 | Length limits (DR-028): (a) accept title 255 and content 10,000 as design limits; (b) remove the limits and use unbounded text storage. | DR-028; FR-004, FR-007 | No | Decided 2026-11-15 by owner: option (a), title 255 and content 10,000 stand as design limits. No re-review run per owner instruction. |
| DRQ-18 | Spring Boot line (DR-027): verify support and pin the newest supported 3.x line (recommended), or accept 3.4.x with a recorded risk. | DR-027; CON-006 | No | Open. Owner decision. |

## Iteration 4 Agreed Design
- No architecture change is agreed in this iteration. The owner has not reviewed architecture iteration 5. The structure (layers, components, API, data model, test plan) is proposed, not agreed.

## Iteration 4 Decision Log
| Date | Decision | Options and Trade-offs | Rationale / Requirements | User Agreement |
|------|----------|------------------------|--------------------------|----------------|
| 2026-11-15 | Iteration 4 review started against requirements iteration 7 (Approved) and architecture iteration 5 (Draft). Mandatory checks 1 to 7 only. | Full review versus mandatory-only scope. Chose mandatory-only, as requested. | Review request; design-review-agent mode. | Not required |
| 2026-11-15 | DR-026 raised as High, blocking. Outcome: Blocked. | Pending approval rejected: a mandatory defect (FR-022 logout) is open. Approved rejected: no owner approval. | Blocking rule: mandatory-scope defect with exact fix. | Not agreed. Pending DRQ-16. |
| 2026-11-15 | DR-027 to DR-030 raised, non-blocking. | Blocking versus non-blocking: none of them breaks a mandatory check on its own. | Checks 1, 6, 2 (secrets wording), and length-limit conflict. | Not agreed. Pending DRQ-17 and DRQ-18. |
| 2026-11-15 | No change made to docs/architecture.md. | Apply the DR-026 fix now, or hold for owner agreement. Chose to hold. | No owner agreement recorded for any architecture change. | Pending |
| 2026-11-15 | docs/requirements.md, docs/sdlc-status.md, and .github/sdlc/approvals.json not modified. No staging, commit, or push performed. | None | Review request and mode constraints. | N/A |

## Iteration 4 Accepted Risks and Follow-ups
| Finding | Risk / Follow-up | Acceptance and Rationale | Mitigation | Owner / Target if Agreed |
|---------|------------------|--------------------------|------------|--------------------------|
| DR-026 | Logout does not revoke remember-me | Not accepted. Blocks readiness. | Exact fix in the findings table | Architecture-agent after DRQ-16; owner decides DRQ-16 |
| DR-027 | Spring Boot 3.4.x support status unverified | Not accepted. Non-blocking follow-up | Verify in the R-1 gate | Verification agent; owner (DRQ-18) |
| DR-028 | Length limits conflict with Q-E5 wording | Not accepted. Owner decision pending | DRQ-17 | Owner (DRQ-17) |
| DR-029 | NFR-007 test wording conflicts with Set-Cookie | Not accepted. Follow-up | Scope the test to bodies | Architecture-agent |
| DR-030 | Committed placeholder key accepted at startup | Not accepted. Follow-up | Refuse start on placeholder | Architecture-agent |

## Iteration 4 Architecture Updates
- None. No architecture change is agreed. Proposed change for DR-026 and DR-029 to DR-030 awaits DRQ-16 and the next architecture iteration.

## Iteration 4 Readiness Decision
Status: **Pending approval**

Blocking conditions:
1. DR-026 (High): Resolved by DRQ-16 option A (owner, 2026-11-15). No blocking finding remains.

Non-blocking follow-ups: DR-027 to DR-030.

Owner approval: designApproved is true in .github/sdlc/approvals.json (2026-11-15).

# History: Iterations 1 to 3

## Review Context
- Review date: 2026-11-15 (iteration 3); iteration 2 was 2026-11-14
- Review iteration: 3 (iterations 1 and 2 are retained in the Findings, Decision Log, and the history sections below). Iteration 2 context follows as written.
- Project: SIMNOTE (SimpleNote), static HTML5/CSS3/vanilla JavaScript, classic script tags, no build step, opened from disk via file://, browser LocalStorage only, Google Chrome only.
- Source documents reviewed:
  - docs/requirements.md, iteration 6. Header status: Approved - owner approval 2026-11-14 (iteration 6). Consistent with .github/sdlc/approvals.json (requirementsApproved: true). DR-006 resolved.
  - docs/architecture.md, iteration 4, status Draft (pending owner approval). Iteration 3 of this review re-read it in full and corrected three wording defects that did not match the owner decisions (DRQ-07, DRQ-08, DRQ-10); see Architecture Updates. Iteration 2 reviewed iteration 3 of the architecture.
  - docs/design-review.md, iteration 1 findings (carried forward).
  - .github/sdlc/approvals.json: designApproved false, implementationPlanApproved false, prApproved false. Read only.
  - docs/sdlc-status.md: read only. Records owner decisions DRQ-01 (option B), DRQ-02 (option C), DRQ-03 (P-1 agreed), and OQ-A3 (option B).
- Owner decisions made 2026-11-15 and verified against architecture iteration 4 in iteration 3: DRQ-06 option A (no salt, risk accepted within NFR-004); DRQ-07 option 1 (headless Chrome --dump-dom; FR-018 covered by Node unit tests); DRQ-08 accept recommendation; DRQ-09 option (a); DRQ-10 option (b) with order session, users, notes and STORAGE_ERROR on failure. Source of these decisions: the owner's instruction for iteration 3. docs/sdlc-status.md does not yet record them (read only; see DR-025).
- Owner decisions treated as established (not re-opened): password rule letter and digit, minimum 8, with legacy login allowed (Q-E8); SHA-256 via crypto.subtle; NFR-004 accepted risk (DRQ-02 option C); P-1 cross-tab guard; P-2 startup via Notes list(); Node.js built-in test runner; Chrome-only DOM and file:// path with no npm and no build (Q-E10 option b).
- Scope: structural design of architecture iteration 4 (iteration 3 of this review; iteration 2 covered architecture iteration 3) against requirements iteration 6: FR-001 to FR-018, NFR-001 to NFR-005, CON-001 to CON-004, BUS-001, BR-001.
- Missing inputs: none. Architecture open items A-1 to A-4 are listed as design inputs, not as missing inputs.
- Out of scope: production code, tests, configuration, and implementation details. No browser or test run was performed. A-3 (crypto.subtle under file://) is not verified.

## Executive Summary
Iteration 3 (2026-11-15):
- Overall assessment: architecture iteration 4 is structurally sound and every owner decision DRQ-06 to DRQ-10 is now recorded and consistent across the data model, flows, test strategy, traceability, open items, and decision log. Verification found three recorded decisions that did not match the owner's decision (DRQ-08 reset-the-key instead of block-writes, DRQ-10 order shown as unconfirmed and absent from the flow, DRQ-07 FR-018 coverage assigned to a manual checklist instead of Node unit tests). These were corrected in architecture.md as owner-decision recording only.
- Previously blocking findings: DR-013 Accepted (owner risk acceptance, DRQ-06 option A); DR-014 and DR-007 Resolved (DRQ-07 option 1); DR-008 Resolved (DRQ-08); DR-009 Resolved (DRQ-09, owner-accepted design decision); DR-016 Resolved (DRQ-10). DR-017 is Accepted on the basis of DRQ-02 and DRQ-06.
- FR-018 automated coverage (owner request): adequate for the data-integrity control (Notes write guard), partial for the re-check logic, and not automated for real two-tab event delivery. Recorded as DR-023 (Medium, non-blocking).
- New findings: DR-023 (Medium), DR-024 (Low), DR-025 (Low). All are non-blocking.
- Readiness recommendation: **Pending approval**. No blocking finding remains. The owner has not approved the design; designApproved remains false. DR-011 (DRQ-12) must be confirmed before an Approved outcome.

Iteration 2 summary (history, 2026-11-14):
- Overall assessment: architecture iteration 3 is structurally sound for a client-only, classic-script app. Component boundaries, the single LocalStorage module, the per-user key model, the session guard, and the startup path are consistent with requirements iteration 6. Every FR, NFR, CON, BUS, and BR ID is traced.
- Iteration 1 progress: DR-001, DR-003, DR-004, DR-005, and DR-006 are Resolved. DR-002 is Accepted by the owner (DRQ-02 option C). DR-007 is Open and narrowed to the browser mechanism (A-2).
- Principal open risks:
  1. A-1 password salt is undecided (DR-013, High). Register and login cannot be implemented without it.
  2. A-2 browser mechanism is undecided (DR-014, High). NFR-002 requires automated DOM and file:// checks, and no mechanism is designed.
  3. A-4 owner confirmations are open: corrupt-data handling (DR-008), username normalization (DR-009), and clear-all confirmation (DR-016). Blocking per the review instruction.
  4. Accepted-risk wording in the architecture goes beyond the owner's recorded acceptance (DR-017, Medium).
  5. A-3 crypto.subtle under file:// is unverified (DR-015). Assessed as a Verification gate, not a design blocker.
- Readiness recommendation: **Blocked**. Blocking conditions: DR-013, DR-014, DR-008, DR-009, DR-016. No owner approval is recorded. Outcome is not Pending approval because the blockers are unresolved.
- Structural defects: none found that change component boundaries. The open items are decisions and confirmations, not redesign.

## Requirements Coverage
| Requirement / Source Section | Architectural Coverage | Gap / Finding |
|------------------------------|------------------------|---------------|
| BUS-001 | All components; whole app | Covered. Status Proposed (DR-011) |
| FR-001 | Auth Module validatePassword (register only); Register flow | Covered. Q-E7 closed (iteration 4, option B). Digit and space definition open (OQ-R14; DR-022) |
| FR-002 | Auth Module login against stored hash; Startup, Cross-tab flows | Covered. Legacy login per Q-E8 (DR-004 Resolved). Hash input is the password only, no salt (DR-013 Accepted). Invalid session handling per DRQ-08 (DR-008 Resolved) |
| FR-003 | Auth Module logout; Logout flow | Covered. Status Proposed (DR-011) |
| FR-004 | Notes Module validateTitle; Create flow | Covered |
| FR-005 | Notes write guard; per-user key; Create flow | Covered. Cross-tab attribution resolved by P-1 (DR-003) |
| FR-006 | Notes Module list; App Controller render; Startup flow | Covered. Startup routed through list() (DR-005 Resolved). Ordering open (Q-N2) |
| FR-007 | Notes Module validateTitle; Edit flow | Covered |
| FR-008 | App Controller render; Edit flow | Covered. Status Proposed (DR-011) |
| FR-009 | Notes Module remove; App Controller; Delete flow | Covered. Confirmation open (Q-N1), not blocking |
| FR-010 | Notes Module remove; Storage; Delete flow | Covered. P-1 guard applies (DR-003 Resolved) |
| FR-011 | Storage Module; SHA-256 via crypto.subtle; data model | Covered. No salt (DR-013 Accepted). Corrupt data per DRQ-08 (DR-008 Resolved). crypto.subtle under file:// unverified (DR-015, Verification gate). Full or unavailable storage (OQ-R5) open, non-blocking |
| FR-012 | UI Layer | Covered. Status Proposed (DR-011) |
| FR-013 | Auth Module uniqueness; Register flow | Covered. Username normalization (trim, case-insensitive, stored normalized) is an owner-accepted design decision (DR-009 Resolved); not stated in requirements (DR-022 traceability note); login-side normalization and empty-username result not explicit (DR-025) |
| FR-014 | Per-user storage keys; Notes Module guard | Covered. Shared file:// origin accepted (DR-002) |
| FR-015 | Notes Module session check; App Controller; Logout, Startup, Notes flows | Covered |
| FR-016 | UI Layer warning banner | Covered. Visible in login and notes views |
| FR-017 | Auth Module clearAllData; Storage removeKeysByPrefix; clear-all flow | Covered. Confirmation, order (session, users, notes), and STORAGE_ERROR recorded (DR-016 Resolved). Control must be reachable when logged out for corrupt-data recovery (DR-024) |
| FR-018 | Notes Module guard; App Controller storage-event re-check; cross-tab flow | Covered by design. Automated via Node unit tests of the guard and re-check logic (owner, DRQ-07); real two-tab delivery not automated; re-check logic sits in app.js, outside the unit-test scope (DR-023) |
| NFR-001 | UI Layer; plain CSS3 media queries | Covered. Viewport targets open (Q-N8), not blocking |
| NFR-002 | Testability Design; Testing row | Covered. Unit path: Node runner. DOM and file:// path: headless Chrome --dump-dom (DR-014, DR-007 Resolved). FR-018 residual gap (DR-023) |
| NFR-003 | Chrome-only stack | Covered. Chrome file:// LocalStorage behavior unverified (DR-012) |
| NFR-004 | Cross-Cutting Security; FR-016, FR-017 mitigations | Covered as accepted risk (DRQ-02 option C; no-salt risk DRQ-06). Offline guessing treated as accepted (DR-017 Accepted); one stale sentence in the architecture (DR-025) |
| NFR-005 | Hosting row; script load order; no ES modules | Covered. file:// checks use --dump-dom (DR-014 Resolved); crypto.subtle check is a Verification gate (DR-015) |
| CON-001 | Tech Stack (HTML5, CSS3, vanilla JS) | Covered |
| CON-002 | Storage Module; LocalStorage only | Covered. Status Proposed (DR-011) |
| CON-003 | No frameworks; no extra features | Covered. Console-only observability (DR-010) |
| CON-004 | Scripting row; Testing row; Testability Design | Covered. Browser mechanism must stay npm-free (A-2 options 1 and 2 comply; Puppeteer, Playwright, Jest excluded) |
| BR-001 | Notes Module; note record id, title, content | Covered. Status Proposed (DR-011) |

## Findings
| ID | Severity | Type | Source / Requirement | Evidence and Impact | Recommendation | Status / Resolution |
|----|----------|------|----------------------|---------------------|----------------|---------------------|
| DR-001 | High | Requirements gap; approval conflict | FR-001, FR-002; Q-E7 | Q-E7 closed in requirements iteration 4 (option B). FR-001 and FR-002 state minimum 8 characters, at least one letter, and at least one digit. Architecture validatePassword matches. | None | Resolved (iteration 2). Evidence: requirements Q-E7 closed; FR-001 and FR-002 text; architecture Auth Module row. |
| DR-002 | High | Security and risk acceptance scope | NFR-004; FR-011; FR-014; FR-015; Q-E3 | Owner decision DRQ-02 option C (2026-11-14) accepts that client-side login is not an access control and that other local HTML pages in the same Chrome profile can read stored data. Mitigations FR-016 and FR-017 are present in the architecture. NFR-004 approved in requirements iteration 6. | None for this finding. Scope wording is covered by DR-017. | Accepted (owner, 2026-11-14, DRQ-02 option C; requirements iteration 6). Residual: DR-017. |
| DR-003 | High | Architecture: data integrity and privacy | FR-005, FR-010, FR-014, FR-018 | P-1 is in architecture iteration 3: Notes write guard compares session user with displayed user (SESSION_MISMATCH); storage-event re-check on simplenote.session and simplenote.users; re-read before each write; cross-tab sequence diagram. Owner agreed P-1 (DRQ-03; FR-018). Within one tab the check and write run as one synchronous step. | None | Resolved (iteration 2). Evidence: architecture Notes Module, Cross-tab flow, iteration 3 Decision Log. Residual: DR-021. |
| DR-004 | Medium | Requirements: password rule lifecycle | FR-002; FR-001; Q-E8 | Owner confirmed Q-E8 (iteration 6): legacy accounts may log in; new registrations meet FR-001. Architecture login compares the stored hash only and does not call validatePassword. | None | Resolved (iteration 2). Evidence: FR-002 text; architecture Login flow and data model notes. Implementation must test legacy login. |
| DR-005 | Medium | Architecture: internal inconsistency | FR-015; FR-006 | P-2 applied: startup flow calls Notes list() first; list() calls currentUserId() before loadNotes. Interfaces table lists list() for App Controller. | None | Resolved (iteration 2). Evidence: Startup sequence diagram; Interfaces table; iteration 3 Decision Log. |
| DR-006 | Medium | Requirements: approval record conflict | Requirements Approval section; OQ-R15 | Requirements header reads Approved - owner approval 2026-11-14 (iteration 6). approvals.json requirementsApproved true. sdlc-status matches. | None | Resolved (iteration 2). Evidence: requirements Approval section; approvals.json; sdlc-status. |
| DR-007 | Medium | Architecture: test harness | NFR-002; NFR-005; CON-004 | Unit path decided: Node.js built-in runner (owner, iteration 4). DOM and file:// path: Chrome-only, no npm (Q-E10 option b). Browser mechanism not selected (A-2). Narrowed to DR-014. | See DR-014 | Resolved (iteration 3, via DR-014). Evidence: architecture Testing row, Testability Design, and A-2 select headless Chrome --dump-dom for DOM and file:// checks and the Node runner for unit tests, both npm-free (NFR-002, CON-004). Residual: DR-023. Prior status (iteration 2): Open, blocking through DR-014. |
| DR-008 | Medium | Architecture: resilience (A-4) | FR-002; FR-011; OQ-R5 | Architecture iteration 3 specifies recommended behavior (data model notes): an unparseable session is treated as logged out and removed; unreadable users or notes return DATA_CORRUPT and writes to that key are blocked; recovery is clear-all. Owner has not agreed the behavior or the user-facing message. Clear-all is the only recovery and removes all users and notes. | Owner answers DRQ-08. Architecture records the agreed rule and message. | Resolved (iteration 3). Owner decision DRQ-08 (accept recommendation). Evidence after correction: architecture Result contract, Data model corrupt-data bullet, Availability and Error handling rows, A-4, and Decision Log record invalid session treated as logged out and removed; unreadable users or notes return DATA_CORRUPT with writes to that key blocked; clear-all is recovery. Iteration 4 had recorded "reset only the unparseable key", which contradicted the decision and was corrected. Residual: recovery depends on clear-all being reachable when logged out (DR-024). Prior status: Open, blocking. |
| DR-009 | Medium | Requirements: unsourced assumption (A-4) | FR-013; OQ-R4a | Architecture assumes an exact, case-sensitive match after trimming, and recommends normalization (A-4). Requirements iteration 6 defines no username rules and has no question for them (DR-022). The uniqueness rule depends on these rules. | Owner answers DRQ-09. If the rule is new, Requirements records it. | Resolved (iteration 3) as an owner-accepted design decision (DRQ-09 option a). Evidence: architecture Drivers (FR-013), Data model (username stored trimmed and lowercase; uniqueness note), Traceability FR-013, A-4, Decision Log. Traceability note: requirements FR-013 says only "username already exists"; case-insensitive and trimmed matching is a stricter interpretation, not a conflict, so no requirements iteration is needed. The rule is tracked for the record as OQ-R4a (DR-022). Minor wording gaps: DR-025. Prior status: Open, blocking. |
| DR-010 | Low | Architecture: operability | CON-003 | Console-only observability. Consistent with CON-003; no logging requirement exists. | None | Open (Low, noted). Non-blocking. |
| DR-011 | Low | Requirements: status consistency | Requirements Business, Functional, and Constraints tables | Iteration 6 still lists BUS-001, BR-001, CON-002, FR-003, FR-008, FR-009, and FR-012 as Proposed, while the architecture marks them Covered. Owner approval covers only items marked Approved. | Owner approved the approval scope (DRQ-12) on 2026-11-16, Lakshay Pahuja. | Resolved 2026-11-16. |
| DR-012 | Low | Verification assumption | NFR-003; NFR-005 | Chrome LocalStorage behavior under file:// is unverified. No browser run in this review. | Verification confirms it through the automated path (A-2). | Open. Verification item. Non-blocking. |
| DR-013 | High | Architecture decision open: security (A-1) | FR-011; NFR-004; FR-001; FR-002 | A-1 is undecided. Option A (no salt): identical passwords share a hash, and precomputed lookups apply across accounts. Option B (per-user random salt via crypto.getRandomValues): the user record gains a salt field. The hash input and record shape depend on this choice, so register and login cannot be implemented. Changing it later changes the stored record format. | Owner answers DRQ-06. Recommendation: option B. Architecture records the chosen record shape. | Accepted (owner, DRQ-06 option A, no salt; owner accepts the risk within NFR-004). Evidence: architecture Data model (no salt field, hash input is the password UTF-8 bytes), Register and Login flows, Password storage row, A-1, Traceability FR-011, Decision Log. The owner chose against the review recommendation (option B); the risk is accepted, not removed. Prior status: Open, blocking. |
| DR-014 | High | Architecture decision open: testability (A-2) | NFR-002; NFR-005; CON-004; FR-018 | A-2 is undecided. NFR-002 (approved) requires automated DOM rendering and file:// checks, and no mechanism is designed. Option 1 (--dump-dom) cannot drive two tabs, so the FR-018 cross-tab check cannot be automated with it. Option 2 (remote debugging, Node built-in WebSocket) meets the no-npm rule and covers two tabs, but needs Node 22 or later and a small custom protocol client. | Owner answers DRQ-07. Recommendation: option 2, with Node 22 or later confirmed as acceptable. | Resolved (iteration 3). Owner decision DRQ-07 option 1 (headless Chrome --dump-dom), chosen against the review recommendation. Evidence: architecture Testing row, Testability Design, A-2, Traceability NFR-002 and NFR-005, Decision Log. The owner accepts that two-tab FR-018 cannot be driven by this mechanism; coverage assessed in DR-023. Prior status: Open, blocking. |
| DR-015 | Medium | Verification gate: crypto.subtle under file:// (A-3) | FR-011; NFR-004; CON-004; NFR-005 | A-3 assessed: not a design blocker. (1) Structure does not change: hashing is one function in the Auth Module, and the fallback (pure-JavaScript SHA-256) changes only that function, but it adds code and needs owner approval. (2) Chrome is expected to treat file: pages as potentially trustworthy, which would make crypto.subtle available. This is an expectation, not verified here. (3) A failure is deterministic and would be caught by the first automated check. The check depends on DR-014; manual-only evidence is not accepted. | Make the availability check the first automated Verification item in Implementation planning. If it fails, reopen design review and request the fallback decision (DRQ-13). | Open. Verification gate. Non-blocking for design. Becomes blocking only if the check fails. |
| DR-016 | Medium | Architecture: destructive action (A-4) | FR-017; Clear-all flow; Storage removeKeysByPrefix | Clear-all deletes all users and notes with no undo and no confirmation (A-4 recommends none). Keys are removed one at a time, so a failure midway can leave the session, users, and notes inconsistent. | Owner answers DRQ-10. Recommendation: confirmation step; remove session first, then users, then notes; report STORAGE_ERROR on any failure. | Resolved (iteration 3). Owner decision DRQ-10 (option b). Evidence after correction: architecture Clear-all flow diagram, narrative, Data model design notes, Traceability FR-017, A-4, and Decision Log record the confirmation step, removal order session then users then notes, and STORAGE_ERROR on any failure with retry. Iteration 4 had marked the order unconfirmed and the flow showed unordered enumeration; corrected. Prior status: Open, blocking. |
| DR-017 | Medium | Security: accepted-risk wording | NFR-004; DRQ-02 | Architecture Cross-Cutting Security says weak passwords and offline guessing are "part of the accepted risk". The owner's recorded acceptance names client-side authentication and shared file:// exposure. Offline guessing of unsalted SHA-256 hashes is not named. | Owner answers DRQ-11. If the owner does not accept offline guessing, the risk is not accepted and DR-013 becomes a stronger requirement. | Accepted (iteration 3). Basis: DRQ-02 option C (readers of the profile data can read stored hashes) and DRQ-06 option A (no salt, risk accepted within NFR-004). Offline guessing is the direct consequence of those two decisions. This is the reviewer's reading; the owner can overrule at approval. DRQ-11 treated as answered unless the owner states otherwise. The architecture Security row still says the scope is unconfirmed (DR-025). Prior status: Open, non-blocking. |
| DR-018 | Low | Architecture: state handling | FR-002; FR-018 | A storage event on simplenote.users (for example, registration in another tab) re-runs startup. This re-renders the notes view and can discard unsaved text in an open editor. | Re-run startup only when the session changes or the displayed user is removed. | Open. Non-blocking. |
| DR-019 | Low | Architecture: result contract | FR-006; FR-008; FR-009 | Result codes have no NOTE_NOT_FOUND. An edit or delete of a note removed in another tab of the same user has no defined result. | Add NOTE_NOT_FOUND and define the UI re-render. | Open. Non-blocking. |
| DR-020 | Low | Deployment and data evolution | NFR-005; CON-002; FR-011 | Stored data has no schema version. All file:// copies of the app share one origin, so a different app version in the same Chrome profile can misread data. | Owner answers DRQ-14 (version key). | Open. Non-blocking. |
| DR-021 | Low | Concurrency (accepted scope) | FR-006; FR-010; FR-018 | Tabs of the same user do not refresh notes when the other tab edits them. The re-read before write prevents overwriting unrelated notes; the same note can still last-write-win. | Owner answers DRQ-14 (accept, or request a notes-change signal). | Open. Non-blocking. Owner acceptance pending. |
| DR-022 | Low | Requirements routing gap | FR-001; FR-002; FR-006; FR-013 | Architecture routes OQ-R3 (automatic login after registration), OQ-R4a (username rules), and OQ-R14 (digit definition; whether spaces count) to Requirements. Requirements iteration 6 has no matching question ID for these. | Requirements records them as open questions, or the owner answers DRQ-09 and DRQ-14. | Open. Non-blocking, except that DR-009 depends on it. |
| DR-023 | Medium | Test coverage gap (owner-accepted limitation) | FR-018; NFR-002; DRQ-07 | Owner decision: --dump-dom cannot drive two tabs, so FR-018 is covered by Node unit tests of the Notes Module guard and session re-check logic. Assessment against architecture iteration 4: (1) Guard (SESSION_MISMATCH for no session, mismatch, match; no write on rejection): adequate; Notes Module is loaded in the unit-test vm context with an in-memory storage fake. (2) Session re-check logic: the storage-event handler and re-run of startup live in the App Controller (app.js), but the unit-test loader evaluates only storage.js, auth.js, and notes.js. Unless the re-check decision is a DOM-free function in a loaded module, it is not unit-testable; partial. (3) Real cross-tab delivery of the storage event and handler registration: not automated; manual checklist item 1 only. NFR-002 does not accept manual-only testing. The statement that --dump-dom cannot drive interactive clicks is overstated: an in-page test page can dispatch synthetic click and StorageEvent events. Impact: the data-integrity control is automated, but the tab-switch behavior of FR-018 could ship unverified by automation. | Architecture: state that the session re-check outcome (which user or view after a session or users change) is a DOM-free function in the Auth or Notes Module, loaded by the unit-test loader, and the App Controller handler only calls it. Optionally allow the --dump-dom in-page test to dispatch a synthetic StorageEvent. Owner: confirm that real two-tab event delivery stays a manual, supplementary check (DRQ-15). | Open. Non-blocking. Not High because the owner decided the mechanism, and the guard that protects note integrity is automated. Recommended owner: architecture-agent (design text), project owner (DRQ-15). |
| DR-024 | Low | Architecture: recovery path | FR-017; FR-016; DRQ-08 | DRQ-08 makes clear-all the only recovery from unreadable users or notes data, and blocks writes to that key, so the user may be unable to log in or register. Architecture does not say the clear-all control is available in the login (logged-out) view; FR-016 banner is stated for both views, the control is not. The DATA_CORRUPT message is also not required to point to clear-all. | Architecture: state that the clear-all control is visible in the login and notes views, and that the DATA_CORRUPT message names clear-all as the recovery. | Open. Non-blocking. Recommended owner: architecture-agent. |
| DR-025 | Low | Architecture and status consistency | DRQ-06, DRQ-09; NFR-004 | (a) Decisions DRQ-06 to DRQ-10 are dated 2026-11-14 in architecture, the owner reports 2026-11-15; the Decision Log carries a date note. (b) Username normalization is stated for storage and uniqueness but not for the login lookup, and no result is defined for a username empty after trimming. (c) The Security row says the offline-guessing scope is unconfirmed (DRQ-11), which is stale after DR-017. (d) Iteration 3 entries sit under the Iteration 4 heading of the Decision Log; history is kept and marked superseded. (e) docs/sdlc-status.md still lists DRQ-06 to DRQ-10 as undecided and the review as iteration 2 Blocked (read only). | Architecture: fix (b) and (c) in the next iteration. Orchestrator: update sdlc-status. Owner: confirm the decision date if it matters. | Open. Non-blocking. Recommended owner: architecture-agent (a to d), orchestrator (e). |

## Clarifying Questions
| ID | Question | Decision / Finding Affected | Blocking? | Answer / Status |
|----|----------|-----------------------------|-----------|-----------------|
| DRQ-01 | Q-E7 / OQ-R13: Which password rules apply beyond minimum 8 characters and at least one digit? | DR-001; FR-001, FR-002 | Was blocking | Answered (owner, iteration 4, option B): minimum 8 characters, at least one letter, at least one digit. Closed. |
| DRQ-02 | Do you accept that client-side login is not an access control, and that local HTML pages opened from disk in the same Chrome profile can read the stored data? | DR-002; NFR-004 | Was blocking | Answered (owner, 2026-11-14, option C): accepted with mitigations FR-016 and FR-017. Scope wording: see DRQ-11. |
| DRQ-03 | Do you agree rule P-1 (write guard and storage-event re-check)? | DR-003; FR-005, FR-010, FR-014, FR-018 | Was blocking | Answered (owner): P-1 agreed. Implemented in architecture iteration 3. |
| DRQ-04 | After Q-E7, should accounts created under an older password rule be allowed to log in? | DR-004; FR-002 | No | Answered (owner, iteration 6, Q-E8): yes, legacy login allowed; new registrations meet FR-001. |
| DRQ-05 | Confirm the requirements approval basis (OQ-R15). | DR-006 | No | Answered by evidence: requirements iteration 6 Approval section and approvals.json requirementsApproved true. |
| DRQ-06 | Password salt (A-1): option A, no salt; or option B, per-user random salt stored with the user record, generated with crypto.getRandomValues. Recommended: option B. | DR-013; FR-011, NFR-004 | Yes. Blocks register and login implementation. | Answered (owner, 2026-11-15): option A, no salt; risk accepted within NFR-004. Recorded in architecture iteration 4 (verified). DR-013 Accepted. |
| DRQ-07 | Browser mechanism for automated DOM and file:// checks (A-2): option 1, headless Chrome --dump-dom on an in-page test (cannot drive two tabs); or option 2, headless Chrome with remote debugging driven by a Node script over the DevTools protocol using the built-in WebSocket (covers the two-tab FR-018 check; requires Node 22 or later). Recommended: option 2. | DR-014, DR-007; NFR-002, NFR-005 | Yes. Blocks DOM and file:// test setup. | Answered (owner, 2026-11-15): option 1, headless Chrome --dump-dom. Two-tab FR-018 not drivable; covered by Node unit tests. Recorded in architecture (verified; wording corrected in iteration 3). DR-014 and DR-007 Resolved; residual DR-023. |
| DRQ-08 | Corrupt stored data (A-4, DR-008): accept the recommendation? An invalid session is treated as logged out and removed. Unreadable users or notes return DATA_CORRUPT, writes to that key are blocked, and clear-all is the recovery. Options: (a) accept the recommendation; (b) state a different user-facing behavior. | DR-008; FR-002, FR-011 | Yes. Blocking per review instruction. | Answered (owner, 2026-11-15): option (a), accept the recommendation. Recorded in architecture after correction. DR-008 Resolved. |
| DRQ-09 | Username rules (A-4, DR-009): options (a) trim surrounding spaces, compare case-insensitively, store the normalized value (recommended); (b) exact, case-sensitive match after trimming. If the rule is new, Requirements records it. | DR-009, DR-022; FR-013 | Yes. Blocking per review instruction. | Answered (owner, 2026-11-15): option (a). Recorded in architecture (verified). Owner-accepted design decision; no requirements iteration needed. DR-009 Resolved. |
| DRQ-10 | Clear-all data (A-4, DR-016): options (a) no confirmation (current architecture); (b) confirmation step before deletion (recommended, because the action is irreversible). Also confirm order: session removed first, then users, then notes. | DR-016; FR-017 | Yes. Blocking per review instruction. | Answered (owner, 2026-11-15): option (b); order session, users, notes; STORAGE_ERROR on any failure. Recorded in architecture after correction. DR-016 Resolved. |
| DRQ-11 | Does the NFR-004 acceptance include offline guessing of stored password hashes by anyone who can read the Chrome profile data? Options: (a) yes, accepted as stated; (b) no, requirement change needed. | DR-017; NFR-004 | No. Escalates to High if (b). | Treated as answered by DRQ-02 and DRQ-06 (DR-017 Accepted). Owner may overrule at approval. |
| DRQ-12 | Do you approve the Proposed items BUS-001, BR-001, CON-002, FR-003, FR-008, FR-009, and FR-012 as part of requirements iteration 6? Options: (a) yes; (b) no, list them for requirements update. | DR-011 | No for design. Required before an Approved outcome. | Decided 2026-11-16: approved by owner (Lakshay Pahuja). |
| DRQ-13 | Accept A-3 (crypto.subtle under file://) as the first automated Verification gate rather than a design blocker? Options: (a) accept, with design reopen if the check fails; (b) require a fallback design now (pure-JavaScript SHA-256, owner approval for added code). | DR-015 | No | Open. Owner decision. Recommendation: (a). |
| DRQ-14 | Non-blocking items: (1) add a schema version key to stored data (DR-020)? (2) accept same-user multi-tab staleness (DR-021)? (3) automatic login after registration (OQ-R3)? (4) is digit limited to 0-9, and do spaces count toward 8 characters (OQ-R14)? | DR-020, DR-021, DR-022 | No | Open. Owner decision. |
| DRQ-15 | FR-018 (DR-023): accept that real two-tab storage-event delivery is checked only by the supplementary manual checklist, provided the guard and the re-check decision logic are unit-tested? Options: (a) yes; (b) no, require an additional automated mechanism (this would reopen DRQ-07). | DR-023; FR-018, NFR-002 | No | Open. Owner decision. Recommendation: (a). |
| OQ-R2 | In what order are notes listed? | FR-006 | No | Open. Requirements (Q-N2). |
| OQ-R3a | Is there an accessibility standard (for example, WCAG level)? | NFR gap | No | Open. Requirements (Q-N3). |
| OQ-R4 | Are there performance targets (page load, response time, maximum notes)? | NFR gap; LocalStorage limit | No | Open. Requirements (Q-N4). |
| OQ-R5 | What happens when LocalStorage is full or unavailable, and what message is shown? | DR-008; FR-011 | No (error path designed). Needed for Verification expectations. | Open. Requirements (Q-N5). |
| OQ-R7 | Should deleting a note require a confirmation prompt? | FR-009 | No | Open. Requirements (Q-N1). |
| OQ-R8 | Which viewport sizes and devices must the responsive UI support? | NFR-001 | No | Open. Requirements (Q-N8). |
| OQ-R9 | Are issue links, subtasks, comments, or attachments expected for SIMNOTE? | Source completeness | No | Open. Requirements (Q-N6). |
| OQ-R10 | Confirm the intended characters in SIMNOTE-1 and SIMNOTE-7 (encoding artifacts). | Source completeness | No | Open. Requirements (Q-N7). |

## Agreed Design
Explicitly agreed by the owner (sources cited):
- Tech stack: HTML5, CSS3, vanilla JavaScript, classic script tags, LocalStorage, no build step (CON-001, CON-004; owner, iteration 3).
- Hosting: double-click index.html from disk (file://), no server, no ES modules (NFR-005; owner, iteration 3).
- Testing: automated tests required; manual-only testing not accepted (NFR-002; owner, iteration 3).
- Unit tests: Node.js built-in test runner (owner, iteration 4, OQ-A3 option B).
- DOM and file:// path: Chrome only, no npm, no build (Q-E10 option b; owner, iteration 6). Mechanism: headless Chrome --dump-dom (owner, DRQ-07, 2026-11-15). FR-018 covered by Node unit tests of the guard and re-check logic (DR-023 residual).
- Password rule: minimum 8 characters, at least one letter, at least one digit; legacy accounts may log in (FR-001, FR-002; Q-E7 option B; Q-E8).
- Password hashing: SHA-256 via crypto.subtle (FR-011; owner, iteration 4). No salt (owner, DRQ-06 option A; risk accepted within NFR-004).
- Accepted risk NFR-004: client-side login is not an access control; shared file:// LocalStorage exposure; mitigations FR-016 and FR-017 (DRQ-02 option C, 2026-11-14). Offline-guessing scope: accepted on the basis of DRQ-02 and DRQ-06 (DR-017).
- Cross-tab isolation P-1: Notes write guard, storage-event re-check, re-read before write (FR-018; DRQ-03, DR-003 Resolved).
- Startup through Notes list() (P-2; DR-005 Resolved).

Also agreed by the owner (2026-11-15):
- Corrupt data (DRQ-08): invalid session treated as logged out and removed; unreadable users or notes return DATA_CORRUPT and writes to that key are blocked; clear-all is the recovery (DR-008).
- Usernames (DRQ-09): trim surrounding spaces, compare case-insensitively, store the normalized value (DR-009).
- Clear-all (DRQ-10): confirmation step; removal order session, users, notes; STORAGE_ERROR on any failure (DR-016).

Still not agreed: crypto.subtle verification gate (DR-015, DRQ-13); non-blocking proposals DR-018, DR-019, DR-020, DR-021, DR-023, DR-024, DR-025 (DRQ-12, DRQ-14, DRQ-15 for the owner). The architecture structure (components, dependencies, single LocalStorage module) is consistent with the requirements, but owner agreement to the structure is not separately recorded and is covered by the design approval. Design approval is pending.

Iteration 2 statement (history): Not yet agreed (pending owner decision): A-1 password salt (DR-013); A-2 browser mechanism (DR-014); A-4 corrupt-data handling (DR-008), username rules (DR-009), and clear-all confirmation and order (DR-016); accepted-risk scope (DR-017); crypto.subtle verification gate (DR-015).

## Decision Log
| Date | Decision | Options and Trade-offs | Rationale / Requirements | User Agreement |
|------|----------|------------------------|--------------------------|----------------|
| 2026-11-14 | Design review iteration 1 started against requirements iteration 3 and architecture iteration 2. | Review now versus wait for owner answers. Chose a review with questions recorded, so open items stay visible. | Design Review stage in sdlc-status; mode constraints. | Not required |
| 2026-11-14 | Outcome set to Blocked. | Pending approval would hide three High findings. Approved is not valid while High findings are open. | Blocking rules: High findings unresolved; decision-blocking question Q-E7 unanswered. | Not agreed. Owner has not reviewed the outcome. |
| 2026-11-14 | No change made to docs/architecture.md. | Apply proposals now, or hold them pending owner agreement. Chose to hold. | No owner agreement recorded for any architecture change. | Pending |
| 2026-11-14 | Requirements, sdlc-status, and approval files not modified. | None | Mode constraint; owned by Requirements and owner. | N/A |
| 2026-11-14 | Iteration 2 review started against requirements iteration 6 (Approved) and architecture iteration 3 (Draft). | Full structural re-review versus targeted delta check. Chose full re-review. | Mode: review all FR, NFR, CON, BUS, and BR IDs; iteration 1 cannot be closed without re-checking. | Not required |
| 2026-11-14 | Resolved: DR-001, DR-003, DR-004, DR-005, DR-006. Accepted: DR-002. Open and narrowed: DR-007 (to A-2). Open: DR-008 to DR-012. | Closure only on evidence in requirements iteration 6, approvals.json, or architecture iteration 3. | Requirements Q-E7 and Q-E8, FR-018, NFR-004, and architecture P-1 and P-2 sections. | Owner decisions per sdlc-status: DRQ-01 option B, DRQ-02 option C, DRQ-03 P-1, OQ-A3 option B. Q-E8 and Q-E10 per requirements iteration 6. |
| 2026-11-14 | Outcome: Blocked. | Pending approval rejected: DR-013 and DR-014 (High) are open, and A-4 items DR-008, DR-009, and DR-016 are unresolved and blocking under the review instruction. Approved rejected: no owner approval. | Blocking rules in Severity and Readiness. | Not agreed. Owner has not reviewed the outcome. |
| 2026-11-14 | A-3 (crypto.subtle under file://) assessed as a Verification gate, not a design blocker. | Blocker: stop design now. Gate: first automated Verification item, reopen on failure. Chose the gate. | Structure is unchanged either way; fallback changes only the hashing function. Expectation not verified. | Pending owner confirmation (DRQ-13). |
| 2026-11-14 | No change made to docs/architecture.md in iteration 2. | Apply recommendations now, or hold until owner agreement. Chose to hold. | No owner agreement recorded for any new architecture change. | Pending |
| 2026-11-14 | Requirements, sdlc-status, and approvals.json not modified. No staging, commit, or push performed. | None | Mode constraint and task instruction. | N/A |
| 2026-11-15 | Iteration 3 started against requirements iteration 6 and architecture iteration 4 (Draft). All findings DR-001 to DR-022 rechecked. | Full recheck versus blockers only. Chose full recheck of every finding, with focus on the five blockers. | Task instruction; iteration 2 blockers were open only for lack of recorded owner decisions. | Not required |
| 2026-11-15 | DR-013 Accepted (DRQ-06 option A, no salt). | Owner chose against the recommendation (option B). Trade-off: less code versus shared precomputed lookups across accounts. | FR-011, NFR-004; owner accepts the risk within NFR-004. | Agreed by the owner (2026-11-15) |
| 2026-11-15 | DR-014 and DR-007 Resolved (DRQ-07 option 1, --dump-dom). | Option 2 (remote debugging, Node 22+, two tabs) was recommended. Option 1 cannot drive two tabs; FR-018 moves to Node unit tests. | NFR-002, NFR-005, CON-004, FR-018. | Agreed by the owner (2026-11-15) |
| 2026-11-15 | DR-008, DR-009, DR-016 Resolved (DRQ-08, DRQ-09, DRQ-10). | DRQ-09 option (b) exact match not chosen. DRQ-10 option (a) no confirmation not chosen. | FR-002, FR-011, FR-013, FR-017. DRQ-09 is a design decision not in requirements; no requirements iteration needed (stricter reading of FR-013). | Agreed by the owner (2026-11-15) |
| 2026-11-15 | Corrected docs/architecture.md wording for DRQ-07, DRQ-08, and DRQ-10 so it matches the owner decisions. No other architecture change. | Edit the architecture versus list proposals only. The task permits editing only to record owner decisions not yet recorded or recorded incorrectly. Chose a minimal edit. | DR-008 (reset versus block), DR-016 (order and STORAGE_ERROR), DR-014 (FR-018 coverage). | Owner decisions already given; wording only |
| 2026-11-15 | DR-017 Accepted on the basis of DRQ-02 and DRQ-06. | Keep Open pending DRQ-11 versus accept on the owner's recorded decisions. Chose accept because offline guessing follows directly from them. | NFR-004. | Reviewer reading; owner may overrule at approval |
| 2026-11-15 | New findings DR-023 (Medium), DR-024 (Low), DR-025 (Low), all non-blocking. | Blocking versus non-blocking for DR-023: owner chose the mechanism and the guard is automated, so non-blocking. | NFR-002, FR-018, FR-017. | Not agreed. Proposals await the owner. |
| 2026-11-15 | Outcome: Pending approval. | Blocked rejected: no Critical or High finding is open and no decision-blocking question is unanswered. Approved rejected: the owner has not approved. | Blocking rules in Severity and Readiness. | Not agreed. Owner has not reviewed the outcome. |
| 2026-11-15 | Requirements, sdlc-status, and approvals.json not modified. No staging, commit, or push performed. | None | Mode constraint and task instruction. | N/A |

## Accepted Risks and Follow-ups
| Finding | Risk / Follow-up | Acceptance and Rationale | Mitigation | Owner / Target if Agreed |
|---------|------------------|--------------------------|------------|--------------------------|
| DR-002 (NFR-004) | Client-side authentication is not an access control; any local HTML page in the same Chrome profile can read stored data, including password hashes and notes | Accepted by owner 2026-11-14 (DRQ-02 option C) | FR-016 warning banner; FR-017 clear-all control; no backend | Owner (accepted 2026-11-14) |
| DR-017 | Offline guessing of unsalted SHA-256 hashes by anyone who can read the Chrome profile data | Accepted on the basis of DRQ-02 and DRQ-06 (iteration 3); owner may overrule at approval | FR-016 banner; FR-017 clear-all | Owner |
| DR-015 | crypto.subtle may be unavailable under file:// (unverified) | Not accepted. Verification gate | First automated check in Implementation planning; reopen design review on failure | Verification agent (gate) |
| DR-012 | Chrome LocalStorage under file:// unverified | Not accepted. Verification item | Automated check through the A-2 mechanism | Verification agent |
| DR-021 | Same-user tabs can show stale notes; the same note can last-write-win | Not accepted. Owner acceptance pending | Re-read before write prevents overwriting unrelated notes | Owner (DRQ-14) |
| DR-010 | Console-only observability | Noted only; no logging requirement (CON-003). Not formally accepted | None required | Architecture |
| DR-020 | No schema version; shared file:// origin across app copies | Not accepted. Owner decision pending | Option: schema version key | Owner (DRQ-14) |
| DR-013 | No password salt: identical passwords share a hash; precomputed lookups apply across accounts | Accepted by owner 2026-11-15 (DRQ-06 option A), within NFR-004 | FR-016, FR-017; client-side login is already an accepted non-access-control | Owner (accepted 2026-11-15) |
| DR-023 | Real two-tab storage-event delivery is not automated; re-check logic must be placed where unit tests can reach it | Owner decided the mechanism (DRQ-07); acceptance of the residual manual check pending (DRQ-15) | Node unit tests of the guard and re-check decision logic; supplementary manual checklist | Architecture-agent (design text); Owner (DRQ-15) |
| DR-024 | Clear-all is the only recovery from corrupt data; reachability when logged out is not stated | Not accepted. Proposal pending | State the control and message in the architecture | Architecture-agent |

## Architecture Updates
Iteration 3 (2026-11-15): applied to docs/architecture.md, wording only, to record owner decisions that were missing or recorded incorrectly. No structural change.
- DRQ-08: replaced "reset only the unparseable key" with the owner decision (invalid session logged out and removed; DATA_CORRUPT with writes blocked; clear-all recovery) in the Overview, Result contract, Data model, Availability row, A-4, and Decision Log. Fixes DR-008.
- DRQ-10: recorded the removal order (session, users, notes) and STORAGE_ERROR on any failure in the Clear-all flow, narrative, Data model notes, Traceability FR-017, A-4, and Decision Log. Fixes DR-016.
- DRQ-07: recorded that FR-018 is covered by Node unit tests of the guard and re-check logic, and that the manual checklist is supplementary, in the Overview, Drivers, Testing row, Testability Design, Traceability FR-018 and NFR-002, A-2, and Decision Log. Residual: DR-023.
- DRQ-06 and DRQ-09: verified present and consistent; no edit.

Proposed architecture changes (not applied; pending owner agreement or next architecture iteration):
- DR-023: place the session re-check decision in a DOM-free function in a unit-tested module; optionally let the --dump-dom in-page test dispatch a synthetic StorageEvent; correct the "cannot drive interactive clicks" statement.
- DR-024: state that the clear-all control is visible in the login view, and that the DATA_CORRUPT message names clear-all.
- DR-025: state username normalization for login; define the result for a username empty after trimming; update the Security row sentence about DRQ-11; fix the decision date if needed.
- DR-018, DR-019, DR-020: as in iteration 2.
- DR-015: A-3 stays a Verification gate (DRQ-13).

Iteration 2 statement and proposals (history; status in iteration 3 as above, A-1, A-2, and A-4 applied in iteration 4, DR-017 closed as Accepted): No changes applied to docs/architecture.md in iteration 2. The owner has not agreed to any new architecture change. Proposed changes, pending owner agreement:
- A-1 (DR-013, DRQ-06): record the salt decision. If option B, add the salt field to the users record and define the hash input. Traces to FR-011, NFR-004.
- A-2 (DR-014, DRQ-07): record the selected mechanism and the Verification read-back method. Traces to NFR-002, NFR-005, CON-004.
- A-3 (DR-015, DRQ-13): add the crypto.subtle availability check as the first automated Verification item. No design text change unless the check fails.
- A-4 (DR-008, DR-009, DR-016): record the corrupt-data rules, username rules, and clear-all confirmation and order once answered.
- DR-018 (proposed): re-run startup on simplenote.users changes only when the displayed user is removed.
- DR-019 (proposed): add NOTE_NOT_FOUND to the result contract.
- DR-020 (proposed, DRQ-14): add a schema version key if approved.
- DR-017 (after DRQ-11): update the Cross-Cutting Security row to match the owner's accepted scope.
- Previously proposed P-1, P-2, and P-6 are reflected in architecture iteration 3 (P-1, P-2) or await owner acceptance (P-6, DR-002 wording).

## Readiness Decision
Status (iteration 3, 2026-11-15): **Pending approval**

Blocking conditions: none. DR-013, DR-014, DR-008, DR-009, DR-016, and DR-007 are closed (Accepted or Resolved) on evidence in architecture iteration 4 as corrected in this iteration. No Critical or High finding is open. No decision-blocking question is unanswered.

Remaining follow-ups (non-blocking): DR-023 (Medium; DRQ-15), DR-024, DR-025, DR-018, DR-019, DR-020, DR-021, DR-022, DR-010, DR-012, and DR-015 (Verification gate; reopen if crypto.subtle is unavailable under file://). DR-011 (DRQ-12) must be confirmed before an Approved outcome.

Explicit user approval evidence: none. designApproved is false in .github/sdlc/approvals.json. Approval is requested for architecture iteration 4 as corrected on 2026-11-15; the outcome becomes Approved only after the owner says so. No implementation starts from this review.

### Iteration 2 readiness (history, 2026-11-14)
Status: **Blocked**

Blocking conditions:
1. DR-013 (High, A-1): password salt undecided. Owner answers DRQ-06.
2. DR-014 (High, A-2): browser mechanism for NFR-002 DOM and file:// checks undecided. Owner answers DRQ-07. This also closes DR-007.
3. DR-008 (Medium, A-4): corrupt-data handling not agreed. Owner answers DRQ-08. Blocking per review instruction.
4. DR-009 (Medium, A-4): username rules not agreed. Owner answers DRQ-09. Blocking per review instruction.
5. DR-016 (Medium, A-4): clear-all confirmation and failure order not agreed. Owner answers DRQ-10. Blocking per review instruction.

Non-blocking follow-ups: DR-010, DR-011 (must be confirmed before Approved, DRQ-12), DR-012, DR-015 (Verification gate, DRQ-13), DR-017 (DRQ-11; High if declined), DR-018, DR-019, DR-020, DR-021, DR-022.

Iteration 1 findings: DR-001, DR-003, DR-004, DR-005, and DR-006 Resolved; DR-002 Accepted; DR-007 Open (blocking through DR-014); DR-008 and DR-009 Open (blocking); DR-010, DR-011, and DR-012 Open (non-blocking).

Explicit user approval evidence: none. designApproved remains false in .github/sdlc/approvals.json. Next step after owner answers: update architecture to iteration 4 for agreed changes only, then rerun design review.

Explicit user approval evidence: none. designApproved remains false in .github/sdlc/approvals.json. No approval has been recorded.
