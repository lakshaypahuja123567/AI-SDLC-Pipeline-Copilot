'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const test = require('node:test');

const security = require('../security-guard.cjs');
const workflow = require('../workflow-approval-guard.cjs');
const verification = require('../verification-guard.cjs');

function withApprovals(approvals, callback) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'copilot-hooks-'));
  const sdlcDirectory = path.join(root, '.github', 'sdlc');
  fs.mkdirSync(sdlcDirectory, { recursive: true });
  fs.writeFileSync(path.join(sdlcDirectory, 'approvals.json'), JSON.stringify(approvals));
  try {
    callback(root);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
}

function approvals(overrides = {}) {
  return {
    requirementsApproved: false,
    designApproved: false,
    implementationPlanApproved: false,
    prApproved: false,
    ...overrides
  };
}

function verificationReport(overall, build, tests, documentation) {
  const section = ([name, status, evidence]) => `## ${name}\nStatus: ${status}\n${status === 'PASS' ? 'Evidence' : 'Reason'}: ${evidence}`;
  return [
    section(['Overall', ...overall]),
    section(['Build', ...build]),
    section(['Tests', ...tests]),
    section(['Documentation', ...documentation])
  ].join('\n\n');
}

test('security guard allows ordinary development commands', () => {
  assert.deepEqual(security.evaluate({ toolName: 'run_in_terminal', toolArgs: { command: 'npm test' } }), {});
  assert.deepEqual(security.evaluate({ toolName: 'run_in_terminal', toolArgs: { command: 'git status --short' } }), {});
  assert.equal(security.isDangerousCommand('Remove-Item ./.cache/one-file.tmp'), false);
});

test('security guard blocks direct secret paths and secret-reading commands', () => {
  assert.equal(security.evaluate({ toolName: 'view', toolArgs: { filePath: '.env' } }).permissionDecision, 'deny');
  assert.equal(security.evaluate({ toolName: 'view', toolArgs: { filePath: 'config/production.pem' } }).permissionDecision, 'deny');
  assert.equal(security.evaluate({ toolName: 'powershell', toolArgs: { command: 'Get-Content .env.production' } }).permissionDecision, 'deny');
});

test('security guard blocks destructive commands and deletion patches', () => {
  assert.equal(security.evaluate({ toolName: 'bash', toolArgs: { command: 'git reset --hard HEAD~1' } }).permissionDecision, 'deny');
  assert.equal(security.evaluate({ toolName: 'powershell', toolArgs: { command: 'Remove-Item -Recurse -Force .\\build' } }).permissionDecision, 'deny');
  assert.equal(security.evaluate({ toolName: 'apply_patch', toolArgs: { patch: '*** Delete File: src/important.js' } }).permissionDecision, 'deny');
});

test('security guard denies malformed payloads', () => {
  assert.equal(security.evaluate(null).permissionDecision, 'deny');
  assert.equal(security.evaluate({ toolName: 'view' }).permissionDecision, 'deny');
});

test('workflow guard blocks architecture and planning without their approvals', () => {
  withApprovals(approvals(), (root) => {
    assert.equal(workflow.evaluate({ toolName: 'edit', cwd: root, toolArgs: { filePath: 'docs/architecture.md' } }).permissionDecision, 'deny');
    assert.equal(workflow.evaluate({ toolName: 'edit', cwd: root, toolArgs: { filePath: 'docs/implementation-plan.md' } }).permissionDecision, 'deny');
  });
});

test('workflow guard detects source files in patch payloads and permits them after plan approval', () => {
  const payload = { toolName: 'apply_patch', cwd: '.', toolArgs: { patch: '*** Update File: src/app.ts\n+const ready = true;' } };
  withApprovals(approvals(), (root) => {
    payload.cwd = root;
    assert.equal(workflow.evaluate(payload).permissionDecision, 'deny');
  });
  withApprovals(approvals({ implementationPlanApproved: true }), (root) => {
    payload.cwd = root;
    assert.deepEqual(workflow.evaluate(payload), {});
  });
});

test('workflow guard blocks approval and hook policy edits', () => {
  withApprovals(approvals(), (root) => {
    assert.equal(workflow.evaluate({ toolName: 'edit', cwd: root, toolArgs: { filePath: '.github/sdlc/approvals.json', content: '{}' } }).permissionDecision, 'deny');
    assert.equal(workflow.evaluate({ toolName: 'edit', cwd: root, toolArgs: { filePath: '.github/hooks/sdlc-hooks.json', content: '{}' } }).permissionDecision, 'deny');
    assert.equal(workflow.evaluate({ toolName: 'run_in_terminal', cwd: root, toolArgs: { command: "node -e \"require('fs').writeFileSync('.github/sdlc/approvals.json', '{}')\"" } }).permissionDecision, 'deny');
  });
});

test('workflow guard allows approved requirements-only commits', () => {
  const reason = workflow.commitBlockReason('.', approvals({ requirementsApproved: true }), ['docs/requirements.md']);
  assert.equal(reason, null);
});

test('workflow guard requires PR approval for release-note commits', () => {
  assert.match(workflow.commitBlockReason('.', approvals({ implementationPlanApproved: true }), ['CHANGELOG.md']), /prApproved/);
  assert.equal(workflow.commitBlockReason('.', approvals({ prApproved: true }), ['CHANGELOG.md']), null);
});

test('workflow guard blocks PR creation and push until PR approval', () => {
  withApprovals(approvals(), (root) => {
    assert.equal(workflow.evaluate({ toolName: 'run_in_terminal', cwd: root, toolArgs: { command: 'gh pr create --title test' } }).permissionDecision, 'deny');
    assert.equal(workflow.evaluate({ toolName: 'run_in_terminal', cwd: root, toolArgs: { command: 'git push origin feature' } }).permissionDecision, 'deny');
    assert.equal(workflow.evaluate({ toolName: 'run_in_terminal', cwd: root, toolArgs: { command: 'gh pr merge 42' } }).permissionDecision, 'deny');
  });
  withApprovals(approvals({ prApproved: true }), (root) => {
    assert.deepEqual(workflow.evaluate({ toolName: 'run_in_terminal', cwd: root, toolArgs: { command: 'gh pr create --title test' } }), {});
    assert.deepEqual(workflow.evaluate({ toolName: 'run_in_terminal', cwd: root, toolArgs: { command: 'git push origin feature' } }), {});
  });
});

test('verification guard blocks missing or incomplete reports', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'copilot-hooks-'));
  try {
    assert.equal(verification.evaluate({ cwd: root }).decision, 'block');
    const sdlcDirectory = path.join(root, '.github', 'sdlc');
    fs.mkdirSync(sdlcDirectory, { recursive: true });
    fs.writeFileSync(path.join(sdlcDirectory, 'verification-report.md'), '## Overall\nStatus: PASS\nEvidence: complete\n');
    assert.equal(verification.evaluate({ cwd: root }).decision, 'block');
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('verification guard accepts evidence-backed PASS and honest FAIL or NOT_APPLICABLE', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'copilot-hooks-'));
  const sdlcDirectory = path.join(root, '.github', 'sdlc');
  fs.mkdirSync(sdlcDirectory, { recursive: true });
  const reportPath = path.join(sdlcDirectory, 'verification-report.md');
  try {
    const passing = verificationReport(
      ['PASS', 'All applicable checks passed.'],
      ['PASS', 'npm run build exited 0.'],
      ['PASS', 'node --test passed 10 tests.'],
      ['PASS', 'Required sections and links were checked.']
    );
    fs.writeFileSync(reportPath, passing);
    assert.deepEqual(verification.evaluate({ cwd: root }), {});

    const failing = verificationReport(
      ['FAIL', 'The build failed.'],
      ['FAIL', 'Compiler returned exit code 1.'],
      ['NOT_RUN', 'Tests were not run because the build failed.'],
      ['PASS', 'Documentation was reviewed.']
    );
    fs.writeFileSync(reportPath, failing);
    assert.deepEqual(verification.evaluate({ cwd: root }), {});

    const notApplicable = verificationReport(
      ['NOT_APPLICABLE', 'No build, tests, or docs are in scope.'],
      ['NOT_APPLICABLE', 'This change has no build step.'],
      ['NOT_APPLICABLE', 'No executable code changed.'],
      ['NOT_APPLICABLE', 'No documentation changed.']
    );
    fs.writeFileSync(reportPath, notApplicable);
    assert.deepEqual(verification.evaluate({ cwd: root }), {});
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('verification guard rejects unsupported PASS claims', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'copilot-hooks-'));
  const sdlcDirectory = path.join(root, '.github', 'sdlc');
  fs.mkdirSync(sdlcDirectory, { recursive: true });
  try {
    fs.writeFileSync(path.join(sdlcDirectory, 'verification-report.md'), verificationReport(
      ['PASS', 'All checks are claimed to pass.'],
      ['PASS', 'Build command completed successfully.'],
      ['FAIL', 'Test runner reported failures.'],
      ['PASS', 'Documentation was reviewed.']
    ));
    assert.equal(verification.evaluate({ cwd: root }).decision, 'block');
    assert.equal(verification.evaluate(null).decision, 'block');
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('workflow guard allows architecture and planning writes after their approvals', () => {
  withApprovals(approvals({ requirementsApproved: true, designApproved: true }), (root) => {
    assert.deepEqual(workflow.evaluate({ toolName: 'edit', cwd: root, toolArgs: { filePath: 'docs/architecture.md' } }), {});
    assert.deepEqual(workflow.evaluate({ toolName: 'edit', cwd: root, toolArgs: { filePath: 'docs/implementation-plan.md' } }), {});
  });
});

test('workflow guard treats missing or malformed approval files as unapproved', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'copilot-hooks-'));
  try {
    assert.equal(workflow.evaluate({ toolName: 'edit', cwd: root, toolArgs: { filePath: 'docs/architecture.md' } }).permissionDecision, 'deny');
    const approvalFile = path.join(root, '.github', 'sdlc', 'approvals.json');
    fs.mkdirSync(path.dirname(approvalFile), { recursive: true });
    fs.writeFileSync(approvalFile, '{broken');
    assert.equal(workflow.evaluate({ toolName: 'edit', cwd: root, toolArgs: { filePath: 'docs/architecture.md' } }).permissionDecision, 'deny');
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('verification guard accepts an accurately reported partial or unexecuted check', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'copilot-hooks-'));
  const sdlcDirectory = path.join(root, '.github', 'sdlc');
  fs.mkdirSync(sdlcDirectory, { recursive: true });
  try {
    fs.writeFileSync(path.join(sdlcDirectory, 'verification-report.md'), verificationReport(
      ['PARTIAL', 'Integration checks remain blocked by an unavailable service.'],
      ['PASS', 'Build command completed successfully.'],
      ['NOT_RUN', 'Integration tests were not run because the service was unavailable.'],
      ['PASS', 'Documentation was reviewed.']
    ));
    assert.deepEqual(verification.evaluate({ cwd: root }), {});
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('workflow guard gates identifiable specialist delegation calls', () => {
  withApprovals(approvals(), (root) => {
    assert.equal(workflow.evaluate({ toolName: 'task', cwd: root, toolArgs: { agent: 'architecture-agent' } }).permissionDecision, 'deny');
    assert.equal(workflow.evaluate({ toolName: 'agent', cwd: root, toolArgs: { task: 'Call implementation-plan-agent next' } }).permissionDecision, 'deny');
    assert.deepEqual(workflow.evaluate({ toolName: 'task', cwd: root, toolArgs: { agent: 'PR Agent', task: 'Prepare a PR proposal' } }), {});
  });
  withApprovals(approvals({ requirementsApproved: true, designApproved: true }), (root) => {
    assert.deepEqual(workflow.evaluate({ toolName: 'task', cwd: root, toolArgs: { agent: 'architecture-agent' } }), {});
    assert.deepEqual(workflow.evaluate({ toolName: 'agent', cwd: root, toolArgs: { task: 'Invoke implementation-plan-agent' } }), {});
  });
});