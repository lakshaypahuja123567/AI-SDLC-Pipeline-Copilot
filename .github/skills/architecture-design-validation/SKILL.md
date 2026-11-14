---
name: architecture-design-validation
description: Guides architecture creation and independent architecture review using approved, traceable requirements.
---

# Architecture & Design Validation

## Objective
Ensure architecture is based on approved requirements and independently reviewed before implementation planning.

## 1. Inputs
- `docs/requirements.md`
- Approved clarification decisions
- Existing project constraints
- Relevant technical constraints

Never treat Draft or unapproved requirements as final.

## 2. Architecture Agent Responsibilities
Create and maintain `docs/architecture.md`.

1. Analyze approved functional and non-functional requirements.
2. Identify system components and their responsibilities.
3. Define component interactions and data flow.
4. Define APIs, integrations, and dependencies where applicable.
5. Address data management, security, and privacy.
6. Consider performance, scalability, reliability, and maintainability.
7. Map architecture decisions to requirement IDs.
8. Document assumptions, risks, and open questions.

Document relevant sections:
- Architecture overview
- Components and responsibilities
- Data flow and interactions
- API and integration design
- Data design
- Security considerations
- Non-functional requirements
- Technology decisions and rationale
- Risks and assumptions
- Requirement traceability

Do not invent requirements or select technologies without justification.

## 3. Design Review Agent Responsibilities
Independently evaluate `docs/architecture.md`.

Check:
- Coverage of all approved requirements.
- Requirement traceability.
- Component responsibilities and interactions.
- Data flow and integration consistency.
- Security and privacy considerations.
- Non-functional requirements.
- Dependencies, risks, and failure scenarios.
- Justification and maintainability of design decisions.

Do not redesign the architecture silently. Document findings and recommendations.

## 4. Finding Classification
- Critical: Fundamental design flaw or major security risk.
- High: Major requirement gap or architectural risk.
- Medium: Maintainability, scalability, or design concern.
- Low: Minor improvement or documentation gap.

For each finding include:
- Finding ID and severity
- Affected requirement or component
- Evidence
- Impact
- Recommended correction

## 5. Rules
- Never invent or silently change requirements.
- Separate facts, assumptions, and recommendations.
- Request clarification for ambiguous requirements.
- Route architecture corrections to the Architecture Agent.
- Do not approve unresolved Critical or High findings.
- Record accepted risks and owner decisions.
- Maintain requirement traceability.

## 6. Expected Outcome
Architecture must be traceable to approved requirements. Design review must provide documented findings and a clear readiness recommendation for the owner's approval gate.