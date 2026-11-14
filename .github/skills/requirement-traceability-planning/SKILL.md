---
name: requirement-traceability-planning
description: Converts approved requirements and architecture into dependency-ordered, traceable implementation tasks with acceptance criteria.
---

# Requirement Traceability & Implementation Planning

## Objective
Create an actionable implementation plan from approved requirements and reviewed architecture.

## 1. Inputs
Read:
- `docs/requirements.md`
- `docs/architecture.md`
- `docs/design-review.md`
- Approved clarification decisions

Do not create an implementation plan if essential inputs are missing or approvals are pending.

## 2. Planning Process
1. Identify approved functional and non-functional requirements.
2. Identify architecture components and dependencies.
3. Break the work into small, independently implementable tasks.
4. Arrange tasks in dependency order.
5. Map every task to relevant requirement IDs.
6. Define clear acceptance criteria for each task.
7. Identify required tests and expected evidence.
8. Identify risks, blockers, and external dependencies.
9. Prioritize tasks based on dependencies and project needs.

## 3. Task Documentation
For each task include:
- Task ID and title
- Description and objective
- Related requirement IDs
- Related architecture component
- Dependencies
- Acceptance criteria
- Expected files or areas affected, where known
- Testing expectations
- Priority
- Status

## 4. Planning Rules
- Do not invent requirements or functionality.
- Do not change approved architecture.
- Do not implement code.
- Do not create tasks for unresolved requirements without identifying the blocker.
- Avoid duplicate or unnecessarily large tasks.
- Ensure dependencies are clear and tasks are actionable.
- Keep requirement-to-task traceability.
- Record assumptions and open questions separately.

## 5. Expected Output
Create or update `docs/implementation-plan.md`.

The plan must:
- Cover approved requirements.
- Follow architecture decisions.
- Have dependency-ordered tasks.
- Include measurable acceptance criteria.
- Identify blockers and unresolved decisions.
- Be ready for owner approval before implementation begins.