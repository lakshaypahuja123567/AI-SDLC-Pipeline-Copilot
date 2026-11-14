# Branching Strategy

## Permanent branches
- `main`: production-ready and protected. Receives merges only from `release/*` or `hotfix/*`.
- `develop`: integration branch. All feature work merges here.

## Supporting branches
- `feature/task-<number>-<description>`: one per approved implementation-plan task, cut from `develop`, PR back into `develop`.
- `release/<version>`: cut from `develop` when stabilising, PR into `main`, then back-merge to `develop`.
- `hotfix/<version>-<description>`: cut from `main`, PR into `main`, then back-merge to `develop`.

## Rules
- Never commit directly to `main` or `develop`.
- Branch names must match the approved implementation-plan task number.
- Commit subjects start with the plan task ID (for example `IP-02: ...`).
- Feature PRs require `implementationPlanApproved=true` and `prApproved=true`.
- Release and hotfix PRs into `main` require `prApproved=true`.
- Squash-merge feature PRs. Never merge PRs automatically.
- Do not force-push or rewrite shared branches without owner approval.
