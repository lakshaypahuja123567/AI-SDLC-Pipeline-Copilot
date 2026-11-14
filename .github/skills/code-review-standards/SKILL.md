---
name: code-review-standards
description: Guides independent, evidence-based code reviews against approved requirements, architecture, implementation plans, and coding standards.
---

# Code Review Standards

## Objective
Evaluate implementation changes and identify defects, risks, and gaps without modifying application code.

## 1. Inputs
Read:
- `docs/requirements.md`
- `docs/architecture.md`
- `docs/implementation-plan.md`
- Relevant implementation and test files
- Git staged and unstaged changes

Read `docs/design-review.md` when relevant.

If required approvals or documents are missing, report the limitation. Never assume approval.

## 2. Review Process
1. Identify review scope and changed files.
2. Inspect the complete relevant diff.
3. Validate changes against approved requirements.
4. Check architecture and implementation-plan compliance.
5. Review correctness and edge cases.
6. Inspect error handling and input validation.
7. Check security, privacy, and sensitive-data handling.
8. Evaluate test quality and coverage.
9. Identify regressions and unnecessary scope changes.
10. Document findings with evidence.

## 3. Review Checklist
- Requirement coverage and traceability.
- Correctness and business logic.
- Architecture and coding-standard compliance.
- Error handling and edge cases.
- Input validation.
- Security and privacy.
- Test coverage and test quality.
- Regression risks.
- Unnecessary or unrelated changes.
- Dependency and configuration risks.

## 4. Finding Classification
- Critical: Severe security issue, data loss, or system-breaking defect.
- High: Major functional defect or significant requirement violation.
- Medium: Non-critical defect or maintainability concern.
- Low: Minor improvement or code-quality suggestion.

For every finding provide:
- Finding ID and severity.
- File and line reference.
- Evidence.
- Impact.
- Recommended fix.

Do not report unsupported or speculative findings as confirmed defects.

## 5. Review Outcomes
- Approved: No unresolved Critical or High findings.
- Changes Required: One or more unresolved Critical or High findings.
- Approved with Observations: No unresolved Critical or High findings, but lower-severity findings remain.

The owner retains final approval authority.

## 6. Rules
- Review only; do not modify application source code or tests.
- Do not execute functionality tests; Verification Agent owns execution.
- Do not hide or silently dismiss findings.
- Do not approve changes with unresolved Critical or High findings.
- Route code defects to Implementation Agent.
- Route architecture issues to Architecture Agent.
- Route requirement ambiguity to Requirements Agent.
- Route missing test implementation to the responsible agent.
- Re-review corrected changes.
- Preserve existing user changes.

## 7. Expected Output
Create or update `docs/code-review.md` with:
- Overall review status.
- Review scope.
- Findings.
- Missing test coverage and verification gaps.
- Dependency and security concerns.
- Recommended actions.

Keep findings concise, actionable, and evidence-based.