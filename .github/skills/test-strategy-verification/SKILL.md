---
name: test-strategy-verification
description: Guides final software verification through build validation, automated testing, requirement coverage, and documentation checks.
---

# Test Strategy & Verification

## Objective
Independently verify implementation quality and provide evidence-based verification results before PR preparation.

## 1. Inputs
Read:
- `docs/requirements.md`
- `docs/architecture.md`
- `docs/implementation-plan.md`
- `docs/code-review.md`
- Application source code and test files
- Project build and test configuration

Do not assume unapproved requirements are final.

## 2. Verification Process

### A. Build Verification
- Inspect build configuration.
- Run the appropriate build command.
- Check compilation errors and dependency issues.
- Record actual results.

### B. Test Verification
- Run available unit tests.
- Run integration tests where configured.
- Check test failures and skipped tests.
- Verify relevant positive and negative scenarios.
- Identify missing test coverage.

### C. Requirement Verification
- Map approved requirements to implemented behavior.
- Check acceptance criteria from the implementation plan.
- Identify missing or incomplete functionality.
- Record unsupported or unverified requirements.

### D. Documentation Verification
- Check whether project documentation reflects the implementation.
- Identify outdated or missing documentation.
- Verify consistency between requirements, architecture, and implementation.

### E. Regression Verification
- Identify potentially affected existing functionality.
- Run relevant regression tests where available.
- Report areas that could not be verified.

## 3. Verification Results

Classify each check as:
- Passed: Successfully verified with evidence.
- Failed: Verification executed and did not meet expectations.
- Blocked: Could not execute due to environment or dependency issues.
- Not Run: Verification was not attempted.

Never report Blocked or Not Run as Passed.

## 4. Defect Routing
- Implementation defect → Implementation Agent.
- Missing or incorrect test → Responsible implementation/testing agent.
- Architecture mismatch → Architecture Agent.
- Requirement ambiguity → Requirements Agent.
- Environment issue → Report blocker and pause.

After corrections, rerun affected verification checks.

## 5. Rules
- Do not modify application source code to fix defects.
- Do not invent test results or execution evidence.
- Do not skip failed tests without documenting the reason.
- Do not approve verification when required checks fail.
- Preserve logs and relevant test evidence.
- Report environment limitations honestly.
- Do not proceed to PR preparation while required verification remains failed or blocked.

## 6. Expected Output
Create or update the verification report using the existing project convention.

Include:
- Verification summary.
- Build results.
- Test execution results.
- Requirement and acceptance-criteria coverage.
- Documentation checks.
- Regression results.
- Failed, blocked, and unexecuted checks.
- Defects and recommended actions.
- Overall verification status.

The report must contain actual evidence and be ready for the owner’s review.