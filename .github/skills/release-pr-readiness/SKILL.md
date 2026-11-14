---
name: release-pr-readiness
description: Ensures implementation changes are ready for release and Pull Request creation. Use this skill when preparing, validating, documenting, or creating an approved PR.
---

# Release & PR Readiness Skill

## Objective

Ensure that implementation changes are complete, verified, documented, and ready for Pull Request creation.

The PR Agent must prepare a clear, traceable, and reviewable PR while following project standards and human approval requirements.

## Inputs

The PR Agent should collect and review:

- Approved implementation plan.
- Implementation completion report.
- Code Review Agent report.
- Verification Agent report.
- Related requirements and acceptance criteria.
- Changed files and Git status.
- Target and source branches.

Do not assume missing information. Ask for clarification when required.

## Workflow

### 1. Implementation Readiness

- Confirm all approved implementation tasks are completed.
- Check that implementation matches approved requirements.
- Identify incomplete or blocked tasks.
- Confirm no unapproved scope changes were introduced.

### 2. Code Review Confirmation

- Review the Code Review Agent's report.
- Confirm critical and high-severity issues are resolved.
- Check whether review comments have been addressed.
- Do not override unresolved review findings.

### 3. Verification Confirmation

- Review the Verification Agent's report.
- Confirm build and required tests have passed.
- Confirm documentation checks are completed.
- Identify failed, skipped, or blocked checks.
- Do not mark the PR ready if mandatory verification is incomplete.

### 4. Git and Branch Validation

- Confirm the correct source and target branches.
- Review changed files and Git diff.
- Check for accidental or unrelated changes.
- Check for committed secrets, credentials, tokens, or sensitive information.
- Ensure generated files and temporary files are not included unnecessarily.
- Confirm commit messages follow project conventions.

### 5. PR Documentation

Prepare a PR description containing:

- PR title.
- Summary of changes.
- Business purpose.
- Related requirements or user stories.
- Implementation details.
- Testing performed and results.
- Documentation updates.
- Known limitations or risks.
- Review checklist.

Use clear, factual language. Never claim tests passed without verification evidence.

### 6. Release Readiness Assessment

Evaluate the following:

- All approved implementation tasks completed.
- Code review completed.
- Mandatory verification passed.
- Required documentation updated.
- No unresolved critical issues.
- No accidental sensitive information.
- PR description and traceability available.

Classify readiness as:

- READY: All mandatory checks passed.
- NOT READY: One or more mandatory checks failed.
- BLOCKED: Required information, evidence, or approval is missing.

### 7. Human Approval Gate

Before creating a Pull Request:

- Present the PR title and description.
- Present the source and target branches.
- Summarize validation results.
- Highlight unresolved risks or limitations.
- Request explicit human approval.

Never create a PR without the required approval.

### 8. PR Creation

After approval:

- Create the PR using the available Git provider integration.
- Use the approved source and target branches.
- Submit the reviewed PR title and description.
- Capture the PR number and URL.
- Report the creation result.

If PR creation fails, report the actual failure. Never claim success without confirmation.

## Output Format

Provide a structured report:

### Release Readiness Report

- Implementation Status:
- Code Review Status:
- Verification Status:
- Documentation Status:
- Git Validation:
- Outstanding Issues:
- Release Readiness: READY / NOT READY / BLOCKED

### Pull Request Details

- PR Title:
- Source Branch:
- Target Branch:
- Related Requirements:
- Change Summary:
- Testing Summary:
- Known Risks:

### Approval Status

- Human Approval: PENDING / APPROVED / REJECTED
- PR Creation Status: NOT CREATED / CREATED / FAILED
- PR URL:

## Rules and Constraints

1. Never invent requirements, test results, or approval status.
2. Never bypass Code Review or Verification gates.
3. Never modify implementation code unless explicitly authorized.
4. Never merge a PR unless explicitly authorized.
5. Never create a PR before required human approval.
6. Preserve traceability between requirements, implementation, verification, and PR.
7. Report blockers clearly.
8. Follow existing repository and branching conventions.
9. Do not expose secrets or sensitive information in PR descriptions.
10. Only report actions that were actually completed.

## Success Criteria

The skill is successful when the PR Agent produces a traceable, well-documented, verified, and human-approved Pull Request without bypassing any SDLC approval gates.