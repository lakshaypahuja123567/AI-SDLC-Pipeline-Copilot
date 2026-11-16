# Implementation Plan

## 1. Status
- **Status: Approved (iteration 1, owner approval 2026-11-15).** Approved by the project owner; DRQ-17 decided 2026-11-15 (title 255, content 10,000 limits stand).
- Source documents and approval state:
  - [docs/requirements.md](docs/requirements.md): iteration 7, Approved (owner approval 2026-11-15, iteration 7).
  - [docs/architecture.md](docs/architecture.md): iteration 5, "Approved for planning". Approver: Lakshay Pahuja (project owner), 2026-11-16; designApproved=true.
  - [docs/design-review.md](docs/design-review.md): iteration 4. DR-026 Resolved by DRQ-16 option A. DR-027 to DR-030 non-blocking.
- Project-owner approval evidence for this plan: none.

## 2. Implementation Overview
- Build SIMNOTE as one Spring Boot 3.x (pinned in T-01), Java 21, Maven application with H2 file storage, Spring Security session login with remember-me, a JSON API under /api, and a static HTML/CSS/vanilla JS frontend served from the same origin.
- Scope is limited to register, login/logout, and note create/view/edit/delete with title and content only. No optional features.
- Tasks are ordered: setup, persistence, security and API, frontend, tests, then docs and verification.

## 3. Phases and Tasks

### Phase 1: Setup and Configuration
| ID | Title / Description | Priority | Depends On | Acceptance Criteria | Reference | Blocked | Status |
|----|---------------------|----------|------------|---------------------|-----------|---------|--------|
| T-01 | Maven project skeleton: pom.xml with Spring Boot 3.x (newest supported 3.x line), Java 21 release, starters for web, data-jpa, security, validation, H2, test; JaCoCo bound to test phase. Run R-1 build gate. | High | None | `mvn -v` shows the JDK used; `mvn test` runs with exit 0; `mvn spring-boot:run` starts on port 8080; no npm files; JaCoCo report written to target/site/jacoco | NFR-006, NFR-019, CON-005, CON-006; R-1, R-2, DR-027 | No (DRQ-18 non-blocking) | Done (commit 360ac97) |
| T-02 | application.properties and config: H2 file jdbc:h2:file:./data/simplenote; ddl-auto=update; H2 console disabled; JSESSIONID HttpOnly and SameSite=Lax; idle timeout 30 min; error stacktrace and message off; remember-me key from SIMPLENOTE_REMEMBER_ME_KEY with placeholder; startup fails if key blank or under 32 characters; data/ git-ignored; Clock bean. | High | T-01 | App fails to start with a blank key; /h2-console returns 404; Set-Cookie for JSESSIONID shows HttpOnly and SameSite=Lax; data/ listed in .gitignore | FR-019, CON-007, NFR-007, NFR-008, NFR-010, NFR-016, FR-022 | No | Done (commit 6610ae9) |

### Phase 2: Persistence and Domain
| ID | Title / Description | Priority | Depends On | Acceptance Criteria | Reference | Blocked | Status |
|----|---------------------|----------|------------|---------------------|-----------|---------|--------|
| T-03 | JPA entities User and Note (Note has title, content only plus storage fields); UserRepository and NoteRepository (findByIdAndUserId, findAllByUserIdOrderByUpdatedAtDescIdDesc); username_normalized UNIQUE; FK notes.user_id; index (user_id, updated_at); persistent_logins table for remember-me. | High | T-02 | Schema created on start; duplicate username_normalized rejected by DB; repository queries filter on user_id; data survives restart (H2 integration test) | FR-019, FR-005, FR-021, FR-013, BR-001, CON-007, FR-022 | No | Done (commit c3c95d0) |

### Phase 3: Validation, Errors, and Security
| ID | Title / Description | Priority | Depends On | Acceptance Criteria | Reference | Blocked | Status |
|----|---------------------|----------|------------|---------------------|-----------|---------|--------|
| T-04 | DTO records (RegisterRequest, LoginRequest, NoteRequest, UserResponse, NoteResponse, ApiErrorResponse); PasswordPolicy (8+ chars, one letter, one digit); UsernameNormalizer (trim, lowercase, blank = 400); TrimmedSize rule (title trimmed, not blank, minimum 1); Bean Validation annotations. No password or hash field in any DTO. | High | T-03 | Unit tests pass: 7 vs 8 chars, letter-only and digit-only rejected, blank title rejected, whitespace-only title rejected; DTOs contain only specified fields | FR-001, FR-004, FR-007, FR-013, NFR-011, NFR-015, BR-001 | No | Done (commit b26aa3c) |
| T-05 | Title maximum 255 characters and content maximum 10,000 characters (design limits). | Medium | T-04 | 256-character title returns 400; 10,001-character content returns 400 | FR-004, FR-007, NFR-011 | No (DRQ-17 decided 2026-11-15: limits stand) | Done (commit 44c80ec) |
| T-06 | ApiExceptionHandler and entry points: single ApiErrorResponse {timestamp, status, error, message, path} for 400, 401, 403, 404, 409, 500; no stack trace or credentials; unhandled exceptions logged at ERROR. | High | T-04 | Each status in NFR-012 returns the JSON shape; no response body contains a stack trace; ERROR log on unhandled exception | NFR-012, NFR-013, NFR-007, NFR-017 | No | Done (commit cdd2da1) |
| T-07 | SecurityConfig: Spring Security session login; BCryptPasswordEncoder; SimpleNoteUserDetailsService; /api/notes requires authentication (JSON 401); CSRF via CookieCsrfTokenRepository (XSRF-TOKEN, HttpOnly false) with plain header comparison and token-loading filter (JSON 403); PersistentTokenBasedRememberMeServices, cookie remember-me, 7 days, JDBC persistent_logins; logout handler deletes the remember-me row and clears cookies. | High | T-03, T-04, T-06 | Request without CSRF token returns 403; unauthenticated /api/notes returns 401; BCrypt hash stored, not the password; remember-me cookie HttpOnly and SameSite=Lax | FR-020, FR-002, FR-003, FR-015, FR-022, CON-008, NFR-008, NFR-009, NFR-012 | No | Done (commit e6033af) |

### Phase 4: Services and API
| ID | Title / Description | Priority | Depends On | Acceptance Criteria | Reference | Blocked | Status |
|----|---------------------|----------|------------|---------------------|-----------|---------|--------|
| T-08 | AuthService and AuthController under /api/auth: register (201, no auto-login, 409 on duplicate case-insensitive username); login (200 with session and remember-me; 401 "Invalid username or password."; WARN log without password); logout (204); me (200 or 401). | High | T-04, T-06, T-07 | Register returns 201, duplicate returns 409; login sets cookies; failed login logs WARN with no password; logout then /api/notes returns 401 | FR-001, FR-002, FR-003, FR-013, FR-020, FR-022, CON-009, NFR-014, NFR-017, NFR-018 | No | Done (commit c826890) |
| T-09 | NoteService and NotesController under /api/notes: list (most recently updated first), create, get, update, hard delete; every operation uses the session user; another user's note returns 404; controllers call services only. | High | T-04, T-06, T-07 | Create/list/get/update/delete work for owner; another user's id returns 404 on get, update, delete; delete returns 204 and note is absent from list | FR-004, FR-005, FR-006, FR-007, FR-008, FR-009, FR-010, FR-014, FR-015, FR-021, CON-009, NFR-014 | No (length limits pending T-05) | Done (commit 477586f) |

### Phase 5: Frontend
| ID | Title / Description | Priority | Depends On | Acceptance Criteria | Reference | Blocked | Status |
|----|---------------------|----------|------------|---------------------|-----------|---------|--------|
| T-10 | Static resources: index.html (login and register forms, notes view; loads styles.css, api.js, app.js as classic scripts, in that order); styles.css responsive (flexbox or grid, media queries); api.js (fetch with same-origin credentials, X-XSRF-TOKEN on POST/PUT/DELETE, error mapping, global 401 handler). No npm, no modules, no inline handlers. | High | T-01 | Page served at http://localhost:8080; no type="module"; no framework or npm dependency | FR-012, NFR-001, CON-005 | No | Done (commit d280e99) |
| T-11 | app.js: logged-out view (login and register only, no note list); logged-in view (list, create, edit, delete, logout); startup check GET /api/auth/me and re-check on window focus; any 401 returns to login view; all user text via textContent. | High | T-08, T-09, T-10 | Logged-out view shows no notes; reload keeps session; logout returns to login view; no innerHTML used for user data | FR-003, FR-006, FR-008, FR-009, FR-012, FR-015, FR-022, NFR-001 | No (Q-N1 confirmation default assumed, see Section 8) | Done (commit bb533d1) |

### Phase 6: Automated Tests
| ID | Title / Description | Priority | Depends On | Acceptance Criteria | Reference | Blocked | Status |
|----|---------------------|----------|------------|---------------------|-----------|---------|--------|
| T-12 | JUnit 5 and Mockito unit tests: PasswordPolicy boundaries; UsernameNormalizer; TrimmedSize; NoteService ownership (repository called with session user id; other user's id gives not found). | High | T-04, T-09 | `mvn test` passes these tests | NFR-002, FR-001, FR-013, FR-021 | No | Done (commit a744c15) |
| T-13 | Spring Boot tests: @WebMvcTest for controller binding and error format; @SpringBootTest with MockMvc on H2 test profile covering register, login, logout, note CRUD, CSRF 403, 401 without session or after logout, 404 cross-user, 409 duplicate, 400 validation, Set-Cookie attributes, no password, hash, session id, or stack trace in response bodies, /h2-console not served, no password in logs, remember-me cookie replay rejected after logout. | High | T-06, T-07, T-08, T-09 | All listed cases pass under `mvn test` | NFR-002, NFR-007, NFR-008, NFR-009, NFR-010, NFR-011, NFR-012, NFR-013, NFR-018, FR-022 | No | Done (commit 68682db; Set-Cookie XSRF attribute case dropped by owner, gap for T-16) |
| T-14 | DOM check script in tests/dom/ (Node 22 built-in fetch and WebSocket, headless Chrome with remote debugging, temporary profile, no npm): register, login, create, edit, delete, logged-out block, reload persistence; layout at 375 px and 1280 px with no horizontal overflow; non-zero exit on failure. Run node -v and record (R-3). | High | T-02, T-11 | Script exits 0 against the running app; exits non-zero on a forced failure | NFR-001, NFR-002, NFR-003, FR-015, FR-022 | No (viewport sizes assumed, see Section 8) | Done (DOM check exit 0 against live app; no script change; no commit) |

### Phase 7: Documentation and Verification
| ID | Title / Description | Priority | Depends On | Acceptance Criteria | Reference | Blocked | Status |
|----|---------------------|----------|------------|---------------------|-----------|---------|--------|
| T-15 | README: prerequisites (JDK 21 or verified compatible JDK, Maven 3.10.0+, JAVA_HOME, Node 22 and Chrome for DOM check), run command, test command, DOM check command, SIMPLENOTE_REMEMBER_ME_KEY, note that Secure cookie is off for localhost. | Medium | T-02, T-14 | README contains each listed item; commands match those run in T-01, T-14 | NFR-021, NFR-006, NFR-016, R-2, R-8 | No | Done (commit ac51896, SIMNOTE/README.md) |
| T-16 | Verification gate: full `mvn test`; JaCoCo line coverage at least 80 percent on service and controller packages; DOM check run; layer and DTO review; no secrets or data/ committed; no extra features (CON-003). | High | T-12, T-13, T-14, T-15 | Coverage report meets NFR-020; all tests and DOM check pass; review notes recorded | NFR-019, NFR-020, NFR-014, NFR-015, NFR-016, CON-003 | No | Done (mvn test 88/0 fail/0 err, BUILD SUCCESS; JaCoCo service+controller line coverage 86/90 = 95.6%; DOM check exit 0; no data/ or secrets tracked; controllers import no repository classes; GAP: XSRF Set-Cookie attribute test dropped, unverified) |

## 4. Dependency Mapping
- T-01: none
- T-02 <- T-01
- T-03 <- T-02
- T-04 <- T-03
- T-05 <- T-04 (blocked)
- T-06 <- T-04
- T-07 <- T-03, T-04, T-06
- T-08 <- T-04, T-06, T-07
- T-09 <- T-04, T-06, T-07
- T-10 <- T-01
- T-11 <- T-08, T-09, T-10
- T-12 <- T-04, T-09
- T-13 <- T-06, T-07, T-08, T-09
- T-14 <- T-02, T-11
- T-15 <- T-02, T-14
- T-16 <- T-12, T-13, T-14, T-15

## 5. Blocked Tasks
| Task | Blocking Prerequisite / Decision |
|------|----------------------------------|
| T-05 | DRQ-17 (DR-028): owner decides whether the 255 and 10,000 design limits stand, given Q-E5 says "No maximum length is defined". |

## 6. Parallelizable Tasks
- After T-01: T-02 and T-10 run in parallel.
- After T-04: T-06 and T-05 can start (DRQ-17 answered 2026-11-15).
- After T-07: T-08 and T-09 run in parallel.
- After T-09: T-12 and T-13 run in parallel.
- After T-11 and T-02: T-14 runs; T-15 follows T-14.

## 7. Risks and Assumptions
| ID | Risk or Assumption | Mitigation | Task |
|----|--------------------|------------|------|
| R-1 | Spring Boot 3.x may not run on the machine's JDK 27 | Build gate in T-01; if it fails, use JDK 21 with JAVA_HOME; release stays 21 | T-01 |
| R-2 | JAVA_HOME not set on the environment | Document in README; verify mvn -v | T-01, T-15 |
| R-3 | Node 22 version reported as v22.18.0, not re-verified | Record node -v during T-14 | T-14 |
| R-4 | 80 percent coverage target not enforced by build | Measure in T-16 | T-16 |
| R-5 | H2 file database locked by a second process | Single instance supported; documented limit | T-02, T-15 |
| R-6 | BCrypt 72-byte input limit | Reject passwords over 72 bytes with 400 | T-04 |
| R-7 | Remember-me placeholder key is committed | Placeholder only; env var override; DR-030 (refuse placeholder value) not adopted, open | T-02 |
| R-8 | Secure cookie flag off on http://localhost | Enable for any HTTPS deployment | T-15 |
| A-1 | Assumed no note-deletion confirmation prompt (Q-N1 open, architecture default) | Owner to confirm; a prompt would be a new task | T-11 |
| A-2 | Assumed viewport checks at 375 px and 1280 px (Q-N8 open) | Owner to confirm sizes | T-14 |
| A-3 | Assumed digit means 0-9 and spaces count toward the 8 characters (OQ-R14, DR-022) | Owner to confirm | T-04 |
| A-4 | Assumed all approved active FR and NFR are required; FR-003, FR-008, FR-009, and FR-012 are out of scope per owner decision 2026-11-16 (Lakshay Pahuja); tasks T-03, T-10, T-11, T-16 still reference them and need re-scoping | Owner decision 2026-11-16 | All |

## 8. Unresolved Decisions / Open Questions
| Item | Affected Tasks | Blocking? |
|------|----------------|-----------|
| DRQ-17 (DR-028): length limits vs Q-E5 | T-05, T-09 (length part) | Decided 2026-11-15 (limits stand) |
| DRQ-18 (DR-027): Spring Boot 3.x line support | T-01 | No (gate) |
| DRQ-12 (DR-011): approval scope of Proposed items | T-03, T-10, T-11, T-16 | Approved by owner 2026-11-16 |
| Q-N1: delete confirmation prompt | T-11 | No (A-1) |
| Q-N8: viewport sizes and devices | T-14 | No (A-2) |
| OQ-R14: digit definition and spaces in password | T-04 | No (A-3) |
| DR-029: NFR-007 test scope (response bodies only, Set-Cookie checked for attributes) | T-13 | No (adopted in T-13) |
| DR-030: refuse startup on placeholder key | T-02 | No (not adopted; open) |

## 9. Parallelizable Groups
- Group A (after T-01): T-02, T-10
- Group B (after T-07): T-08, T-09
- Group C (after T-09): T-12, T-13
- Group D: T-14 and T-15 are sequential; T-16 last

## 10. Definition of Done
Applies to every task:
- [ ] Acceptance criteria met and recorded
- [ ] Traced to a requirement ID or architecture component
- [ ] Automated test added or updated where the task requires it; no manual-only evidence
- [ ] No secrets, passwords, or hashes in code, logs, or responses
- [ ] No npm, build step, or extra feature added (CON-003, CON-005)
- [ ] Status updated to Done by the implementation agent

Overall release:
- [ ] `mvn test` passes
- [ ] JaCoCo line coverage at least 80 percent on service and controller packages (NFR-020)
- [ ] DOM check passes against the running app (NFR-002)
- [ ] README complete (NFR-021)
- [ ] Every approved FR and NFR traced in Section 11 is covered
- [ ] Project-owner approval of this plan recorded

## 11. Requirement Traceability
| Requirement | Tasks |
|-------------|-------|
| FR-001 | T-04, T-08, T-12 |
| FR-002 | T-07, T-08, T-13 |
| FR-003 | T-07, T-08, T-11, T-13 |
| FR-004 | T-04, T-05, T-09 |
| FR-005 | T-03, T-09 |
| FR-006 | T-09, T-11 |
| FR-007 | T-04, T-05, T-09 |
| FR-008 | T-09, T-11 |
| FR-009 | T-09, T-11 |
| FR-010 | T-09, T-14 |
| FR-012 | T-10, T-11 |
| FR-013 | T-03, T-04, T-08, T-12 |
| FR-014 | T-09, T-13 |
| FR-015 | T-07, T-09, T-11, T-14 |
| FR-019 | T-02, T-03 |
| FR-020 | T-07, T-08 |
| FR-021 | T-03, T-09, T-12, T-13 |
| FR-022 | T-02, T-03, T-07, T-08, T-11, T-13, T-14 |
| BR-001 | T-03, T-04 |
| NFR-001 | T-10, T-11, T-14 |
| NFR-002 | T-12, T-13, T-14 |
| NFR-003 | T-14 |
| NFR-006 | T-01, T-02, T-15 |
| NFR-007 | T-02, T-06, T-13 |
| NFR-008 | T-02, T-07, T-13 |
| NFR-009 | T-07, T-13 |
| NFR-010 | T-02, T-13 |
| NFR-011 | T-04, T-05, T-13 |
| NFR-012 | T-06, T-07, T-13 |
| NFR-013 | T-06, T-13 |
| NFR-014 | T-08, T-09, T-16 |
| NFR-015 | T-04, T-08, T-09, T-16 |
| NFR-016 | T-02, T-15, T-16 |
| NFR-017 | T-06, T-08 |
| NFR-018 | T-08, T-13 |
| NFR-019 | T-01, T-16 |
| NFR-020 | T-16 |
| NFR-021 | T-15 |
| CON-003 | T-16 |
| CON-005 | T-01, T-10, T-14 |
| CON-006 | T-01 |
| CON-007 | T-02, T-03 |
| CON-008 | T-07 |
| CON-009 | T-08, T-09 |

Requirements not covered: none.
