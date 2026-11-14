'use strict';

const fs = require('node:fs');
const path = require('node:path');

const PATH_KEYS = /^(?:filePath|file_path|path|filename|target|destination|uri)$/i;
const WRITE_TOOLS = new Set(['edit', 'create', 'write', 'apply_patch', 'str_replace_editor', 'create_file', 'write_file', 'delete', 'delete_file', 'remove_file']);
const SHELL_TOOLS = new Set(['bash', 'powershell', 'run_in_terminal', 'execute']);
const AGENT_TOOLS = new Set(['agent', 'task', 'run_subagent']);
const SOURCE_EXTENSIONS = /\.(?:c|cc|cpp|h|hpp|cs|go|java|js|cjs|mjs|jsx|ts|tsx|py|rb|php|rs|scala|swift|kt|sql|html|css|scss|sass|vue|svelte|json|ya?ml|toml|xml|sh)$/i;
const DENY = (permissionDecisionReason) => ({ permissionDecision: 'deny', permissionDecisionReason });

function collectStrings(value, output = []) {
  if (typeof value === 'string') output.push(value);
  else if (Array.isArray(value)) value.forEach((item) => collectStrings(item, output));
  else if (value && typeof value === 'object') Object.values(value).forEach((item) => collectStrings(item, output));
  return output;
}

function collectPathFields(value, output = []) {
  if (!value || typeof value !== 'object') return output;
  for (const [key, child] of Object.entries(value)) {
    if (PATH_KEYS.test(key)) {
      if (typeof child === 'string') output.push(child);
      else if (Array.isArray(child)) output.push(...child.filter((item) => typeof item === 'string'));
    }
    collectPathFields(child, output);
  }
  return output;
}

function normalize(value) {
  return String(value).replace(/\\/g, '/').replace(/^\.\//, '').toLowerCase();
}

function mentions(text, expectedPath) {
  return normalize(text).includes(expectedPath.toLowerCase());
}

function extractFileTokens(text) {
  return String(text).match(/(?:[a-z]:)?(?:\.{1,2}\/)?(?:[\w.@+-]+\/)*[\w.@+-]+\.[a-z0-9]{1,10}/gi) || [];
}

function extractPatchTargets(text) {
  return [...String(text).matchAll(/^\*\*\*\s+(?:Add|Update|Delete) File:\s*(.+?)\s*$/gim)].map((match) => match[1]);
}

function isApplicationSource(target) {
  const normalized = normalize(target).replace(/^[a-z]:\//, '');
  if (!SOURCE_EXTENSIONS.test(normalized)) return false;
  if (/^(?:\.github|\.vscode|docs|\.git|node_modules)\//.test(normalized)) return false;
  return true;
}

function isShellWrite(toolName, command) {
  if (!SHELL_TOOLS.has(toolName.toLowerCase())) return false;
  return /\b(?:set-content|add-content|out-file|new-item|remove-item|copy-item|move-item|touch|tee|sed\s+-i|apply_patch|writefile(?:sync)?|appendfile(?:sync)?|rename(?:sync)?|unlink(?:sync)?|chmod(?:sync)?|mkdir(?:sync)?|rmdir(?:sync)?|git\s+(?:commit|push))\b|(?:^|[^<])>{1,2}\s*\S+|(?:^|\s)(?:cp|mv|rm|del|erase)\s/i.test(command);
}

function loadApprovals(rootDir) {
  try {
    const file = path.join(rootDir, '.github', 'sdlc', 'approvals.json');
    const value = JSON.parse(fs.readFileSync(file, 'utf8'));
    return {
      requirementsApproved: value.requirementsApproved === true,
      designApproved: value.designApproved === true,
      implementationPlanApproved: value.implementationPlanApproved === true,
      prApproved: value.prApproved === true
    };
  } catch {
    return { requirementsApproved: false, designApproved: false, implementationPlanApproved: false, prApproved: false };
  }
}

function stagedFiles(rootDir) {
  try {
    const output = execFileSync('git', ['diff', '--cached', '--name-only', '-z'], {
      cwd: rootDir,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'ignore'],
      timeout: 1000
    });
    return output.split('\0').filter(Boolean);
  } catch {
    return null;
  }
}

function commitBlockReason(rootDir, approvals, files = stagedFiles(rootDir)) {
  if (!files || files.length === 0) return 'Cannot verify staged files for this commit; the operation is blocked.';
  const normalizedFiles = files.map(normalize);
  const hasSource = files.some(isApplicationSource);
  const hasReleaseNotes = normalizedFiles.some((file) => /(?:^|\/)(?:changelog(?:\.[^/]+)?|release-notes(?:\.[^/]+)?)$/i.test(file));
  if (hasReleaseNotes && !approvals.prApproved) return 'PR changelog/release-note commits require prApproved=true.';
  if (hasSource && !approvals.implementationPlanApproved && !approvals.prApproved) {
    return 'Source-code commits require implementationPlanApproved=true in the human-managed approvals file.';
  }
  if (normalizedFiles.every((file) => file === 'docs/requirements.md') && approvals.requirementsApproved) return null;
  if (approvals.prApproved || approvals.implementationPlanApproved) return null;
  return 'This commit requires an approved implementation plan or explicit PR approval.';
}

function delegationBlockReason(toolName, argumentsText, approvals) {
  if (!AGENT_TOOLS.has(toolName.toLowerCase())) return null;
  const text = argumentsText.toLowerCase();
  if (/\barchitecture-agent\b/.test(text) && !approvals.requirementsApproved) {
    return 'Architecture Agent delegation requires requirementsApproved=true.';
  }
  if (/\bdesign-review-agent\b/.test(text) && !approvals.requirementsApproved) {
    return 'Design Review Agent delegation requires approved requirements.';
  }
  if (/\bimplementation-plan-agent\b/.test(text) && !approvals.designApproved) {
    return 'Implementation Plan Agent delegation requires designApproved=true.';
  }
  if (/\bimplementation-agent\b/.test(text) && !approvals.implementationPlanApproved) {
    return 'Implementation Agent delegation requires implementationPlanApproved=true.';
  }
  return null;
}

function evaluate(payload, rootDir = payload && payload.cwd) {
  if (!payload || typeof payload !== 'object' || typeof payload.toolName !== 'string' || typeof rootDir !== 'string' || !('toolArgs' in payload)) {
    return DENY('The workflow approval hook received malformed input and blocked the operation.');
  }

  const toolName = payload.toolName.toLowerCase();
  const strings = collectStrings(payload.toolArgs);
  const joined = strings.join('\n');
  const pathFields = collectPathFields(payload.toolArgs);
  const command = SHELL_TOOLS.has(toolName) ? joined : '';
  const writingTool = WRITE_TOOLS.has(toolName) || isShellWrite(toolName, command);
  const allTargets = pathFields.concat(extractPatchTargets(joined), extractFileTokens(command));
  const approvalsPathMentioned = mentions(joined, '.github/sdlc/approvals.json') || mentions(joined, 'approvals.json');
  const protectedHookTarget = allTargets.some((target) => normalize(target).includes('.github/hooks/'));

  if (writingTool && protectedHookTarget) {
    return DENY('Hook configuration and scripts are protected from agent modification.');
  }

  if (approvalsPathMentioned && (writingTool || /\b(?:writefile|appendfile|rename|unlink|chmod)\b/i.test(joined))) {
    return DENY('The SDLC approvals file is human-managed and cannot be changed by an agent.');
  }

  const approvals = loadApprovals(rootDir);
  const delegationReason = delegationBlockReason(toolName, joined, approvals);
  if (delegationReason) return DENY(delegationReason);

  if (writingTool && allTargets.some((target) => mentions(target, 'docs/architecture.md')) && !approvals.requirementsApproved) {
    return DENY('Architecture changes require requirementsApproved=true in the human-managed approvals file.');
  }
  if (writingTool && allTargets.some((target) => mentions(target, 'docs/implementation-plan.md')) && !approvals.designApproved) {
    return DENY('Implementation planning requires designApproved=true in the human-managed approvals file.');
  }

  const sourceTarget = allTargets.find(isApplicationSource);
  if (writingTool && sourceTarget && !approvals.implementationPlanApproved) {
    return DENY('Source-code changes require implementationPlanApproved=true in the human-managed approvals file.');
  }

  const isMerge = /\bgh\s+pr\s+merge\b|\bgh\s+api\b[^\n]*\/pulls\/[^\s/]+\/merge\b/i.test(joined)
    || /(?:^|[._/-])merge[_-]?pull[_-]?request(?:$|[._/-])/i.test(toolName);
  if (isMerge) return DENY('PR merging is not automated by this workflow.');

  const isPush = /\bgit\s+push\b/i.test(joined) || /(?:^|[._/-])push(?:$|[._/-])/i.test(toolName);
  const isPrCreation = /\bgh\s+pr\s+create\b|\bgh\s+api\b[^\n]*\/pulls(?:\s|$)/i.test(joined)
    || /(?:^|[._/-])(?:create[_-]?pull[_-]?request|pull[_-]?request[_-]?create)(?:$|[._/-])/i.test(toolName);
  if ((isPush || isPrCreation) && !approvals.prApproved) {
    return DENY('Pushing or creating a PR requires prApproved=true in the human-managed approvals file.');
  }

  const isCommit = /\bgit\s+commit\b/i.test(joined) || /(?:^|[_-])commit(?:$|[_-])/i.test(toolName);
  if (isCommit) {
    const reason = commitBlockReason(rootDir, approvals);
    if (reason) return DENY(reason);
  }

  return {};
}

function main() {
  let payload;
  try {
    payload = JSON.parse(fs.readFileSync(0, 'utf8'));
  } catch {
    process.stdout.write(JSON.stringify(DENY('The workflow approval hook could not parse its input and blocked the operation.')));
    return;
  }
  process.stdout.write(JSON.stringify(evaluate(payload)));
}

if (require.main === module) main();

module.exports = { evaluate, loadApprovals, isApplicationSource, extractPatchTargets, commitBlockReason, delegationBlockReason };