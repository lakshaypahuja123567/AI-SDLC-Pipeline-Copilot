---
name: requirement-analysis
description: "Use when extracting, classifying, clarifying, and documenting functional and non-functional requirements from Jira, documents, or stakeholder input."
---

# Requirements Elicitation & Classification Framework

## 1. Purpose
Use this document as the standard reference to extract, classify, validate, and document Functional Requirements (FRs) and Non-Functional Requirements (NFRs) from Jira issues, user stories, acceptance criteria, documents, and stakeholder inputs.

## 2. Requirement Definitions

**Functional Requirement (FR):** Defines WHAT the system must do — its features, behavior, business logic, processing, and responses to user actions or events.

Examples:
- The system shall allow users to log in.
- The system shall calculate order totals.
- The system shall send order confirmation emails.

**Non-Functional Requirement (NFR):** Defines HOW WELL the system must operate — quality attributes, measurable performance, security, operational expectations, and constraints.

Examples:
- The system shall respond within 2 seconds.
- The system shall support 10,000 concurrent users.
- The system shall maintain 99.9% monthly availability.

## 3. Functional Requirement Categories

| ID | Category | Includes |
|---|---|---|
| FR-01 | User Management & Interaction | Registration, login, profiles, roles, permissions |
| FR-02 | Business Processes & Workflows | Approvals, status transitions, task assignment |
| FR-03 | Data Management | CRUD, import, export, search, synchronization |
| FR-04 | Business Rules & Validation | Calculations, eligibility, conditions, restrictions |
| FR-05 | Search & Navigation | Search, filtering, sorting, pagination |
| FR-06 | Notifications & Communication | Email, SMS, alerts, reminders, triggers |
| FR-07 | Integration | APIs, payment gateways, external systems, webhooks |
| FR-08 | Reporting & Analytics | Reports, dashboards, KPIs, aggregations |
| FR-09 | Security Functionality | Authentication, authorization, MFA, account lockout |
| FR-10 | Error & Exception Handling | Error messages, retries, recovery, failure handling |
| FR-11 | Administration & Configuration | Settings, templates, system parameters |
| FR-12 | Scheduling & Automation | Scheduled jobs, batch processing, automated tasks |
| FR-13 | Audit & Traceability | Activity logs, transaction history, change tracking |

## 4. Non-Functional Requirement Categories

| ID | Category | Includes |
|---|---|---|
| NFR-01 | Performance | Response time, latency, throughput, resource usage |
| NFR-02 | Scalability | Concurrent users, workload, data growth, scaling |
| NFR-03 | Availability | Uptime, service accessibility, maintenance windows |
| NFR-04 | Reliability | Failure rate, fault tolerance, consistency, resilience |
| NFR-05 | Security | Encryption, secure access, protection, security standards |
| NFR-06 | Privacy | Consent, personal data protection, anonymization |
| NFR-07 | Usability & Accessibility | Ease of use, UX, WCAG, keyboard/screen-reader support |
| NFR-08 | Compatibility & Interoperability | Browsers, devices, OS, API and platform compatibility |
| NFR-09 | Maintainability | Modularity, code changes, debugging, upgrades |
| NFR-10 | Testability | Automation support, test environments, test data, observability |
| NFR-11 | Portability & Deployability | Deployment, migration, environment independence |
| NFR-12 | Recovery & Disaster Recovery | Backup, restore, RTO, RPO, business continuity |
| NFR-13 | Compliance & Legal | GDPR, PCI DSS, regulatory and policy obligations |
| NFR-14 | Data Quality | Accuracy, completeness, consistency, validity, freshness |
| NFR-15 | Monitoring & Observability | Logs, metrics, tracing, alerts, health checks |
| NFR-16 | Localization & Internationalization | Languages, currencies, time zones, regional formats |
| NFR-17 | Technical & Architectural Constraints | Mandatory technology, frameworks, databases, architecture |
| NFR-18 | Operational Constraints | Infrastructure, network, hardware, data residency |

## 5. Other Information Types (Not FR/NFR)

| Type | Meaning |
|---|---|
| Business Requirement | Business objective or organizational need (WHY) |
| Stakeholder Requirement | Stakeholder's need or expectation |
| User Requirement | User's goal or need |
| Business Rule | Policy, calculation, condition, or decision logic |
| Constraint | Mandatory restriction or limitation |
| Assumption | Unverified belief used for planning |
| Dependency | External condition or prerequisite |
| Acceptance Criteria | Conditions for verifying requirement completion |
| Solution Design | Proposed implementation approach |
| Out of Scope | Explicitly excluded functionality |

Do not automatically classify these as FR/NFR. Extract related system requirements only when explicitly stated or logically derived.

## 6. Classification Rules

1. Classify by meaning, not keywords, Jira labels, or assumptions.
2. WHAT the system does → FR.
3. HOW WELL it operates → NFR.
4. Business policy/calculation → Business Rule; system enforcement of it → FR.
5. Mandatory technology/standard/restriction → Constraint, generally NFR-related.
6. Mixed FR/NFR statements → split into atomic requirements.
7. Functional security behavior → FR; security quality/standards → NFR.
8. Notification behavior → FR; notification delivery SLA → NFR.
9. API integration behavior → FR; API performance/availability → NFR.
10. Never invent missing requirements, metrics, priorities, or stakeholder decisions.
11. Use taxonomy categories; flag unclassifiable requirements for review.
12. Preserve original Jira IDs, source text, and traceability.

## 7. Requirement Quality Validation

Every requirement should be:

- **Clear:** One unambiguous interpretation.
- **Atomic:** One independently verifiable requirement.
- **Testable:** Can be verified through defined criteria.
- **Necessary:** Supports a business need or obligation.
- **Consistent:** Does not contradict other requirements.
- **Feasible:** Achievable within known constraints.
- **Traceable:** Linked to its source.

NFRs should specify measurable targets wherever possible: metric, value, unit, conditions, scope, and measurement method.

## 8. Ambiguity & Clarification

Flag vague terms such as:
`fast`, `secure`, `user-friendly`, `real-time`, `robust`, `scalable`, `reliable`, `seamless`, `quickly`, `appropriate`.

Identify missing details:
- Performance → response time, throughput, load, concurrency.
- Availability → uptime %, measurement period, maintenance exclusions.
- Security → protected data, controls, standards, threat context.
- Scalability → expected users, transactions, and data growth.
- Usability → measurable outcomes or accessibility standards.

Ask focused clarification questions only when information is missing, ambiguous, contradictory, or materially impacts requirements. Never assume answers.

## 9. Explicit, Derived & Inferred Requirements

- **Explicit:** Directly stated in the source.
- **Derived:** Logically necessary to satisfy another requirement.
- **Inferred:** Potential requirement identified through analysis, pending confirmation.
- **Assumption:** Unverified condition temporarily accepted.
- **Needs Clarification:** Insufficient information to finalize.

Never present inferred or derived requirements as confirmed stakeholder statements.

## 10. Duplicate & Conflict Detection

Compare requirements across Jira stories, epics, acceptance criteria, and documents.

- Flag semantically equivalent requirements as potential duplicates.
- Identify contradictory business rules, targets, constraints, and behaviors.
- Preserve both source references.
- Explain conflicts and ask stakeholders for resolution.
- Never silently delete, merge, or prioritize conflicting requirements.

## 11. Standard Requirement Record

Each extracted requirement should contain:

| Field | Value |
|---|---|
| Requirement ID | Unique identifier |
| Statement | Clear, atomic, testable requirement |
| Type | FR / NFR / Other |
| Category | Framework category |
| Source | Jira ID / document / stakeholder |
| Origin | Explicit / Derived / Inferred |
| Status | Proposed / Confirmed / Needs Clarification / Approved |
| Priority | Source-defined or Not Specified |
| Rationale | Business justification |
| Acceptance Criteria | Verification conditions |
| Dependencies | Related requirements or systems |
| Questions | Unresolved clarification |
| Related Items | Duplicates, conflicts, parent requirements |

## 12. Expected Agent Output

1. **Source Summary:** Source, context, business objective, scope.
2. **Functional Requirements:** ID, statement, category, source, origin, status.
3. **Non-Functional Requirements:** ID, statement, category, metric/target, conditions, source, origin, status.
4. **Business Rules & Constraints:** Separately classified.
5. **Clarification Questions:** Question, related requirement, reason, priority, status.
6. **Assumptions & Inferences:** Clearly identified and unconfirmed.
7. **Quality Review:** Ambiguities, gaps, untestable requirements, duplicates, conflicts.
8. **Summary:** Total FRs, NFRs, questions, unresolved conflicts, inferred requirements.

## 13. Golden Rule

**FR = WHAT the system must do.**  
**NFR = HOW WELL the system must do it or the constraints under which it operates.**  
**Business Requirement = WHY the business needs it.**  
**Acceptance Criteria = HOW completion is verified.**  
**Business Rule = WHAT policy or logic governs behavior.**

When classification is uncertain, flag it and seek clarification instead of making unsupported assumptions.

---

End of Framework