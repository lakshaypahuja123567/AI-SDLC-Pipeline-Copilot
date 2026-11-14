---
name: coding-standards-task-execution
description: Guides implementation of approved tasks using project architecture, coding standards, testing practices, and requirement traceability.
---

# Coding Standards & Task Execution

## Objective
Implement approved tasks safely, consistently, and incrementally without changing agreed requirements or architecture.

## 1. Inputs
Read:
- `docs/requirements.md`
- `docs/architecture.md`
- `docs/implementation-plan.md`
- Relevant approved task and acceptance criteria
- Existing source code and project conventions

Do not start tasks that are unapproved, blocked, or dependent on incomplete tasks.

## 2. Implementation Process
1. Select one approved, dependency-ready task.
2. Understand its requirements and acceptance criteria.
3. Inspect existing code before making changes.
4. Follow the approved architecture and existing coding conventions.
5. Implement only the scope of the selected task.
6. Add or update relevant tests.
7. Run appropriate tests and inspect results.
8. Review the diff for unintended changes.
9. Update task status and implementation evidence.
10. Commit locally only when authorized.

Wait for owner approval before starting the next task.

## 3. Coding Standards
- Write readable, maintainable, reusable code.
- Follow existing naming and formatting conventions.
- Avoid unnecessary duplication.
- Handle errors and edge cases appropriately.
- Validate inputs where required.
- Follow security and privacy requirements.
- Avoid unnecessary dependencies.
- Do not introduce unrelated refactoring.

## 4. Testing Expectations
- Add or update tests for implemented behavior.
- Cover relevant positive and negative scenarios.
- Test acceptance criteria.
- Do not claim tests passed unless they were actually executed.
- Report failures and environment limitations honestly.

## 5. Safety Rules
- Never invent requirements or change approved architecture.
- Never implement multiple tasks without approval.
- Preserve existing user changes.
- Do not overwrite unrelated files.
- Do not modify secrets or expose credentials.
- Do not push branches or create PRs.
- Report blockers instead of making unsupported assumptions.

## 6. Expected Output
For each task, report:
- Task ID and objective
- Files changed
- Implementation summary
- Tests executed and actual results
- Acceptance criteria status
- Remaining issues or blockers
- Commit hash, if committed

Update `docs/implementation-plan.md` with accurate task status and evidence.