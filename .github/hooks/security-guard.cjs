'use strict';

const { execFileSync } = require('node:child_process');
const path = require('node:path');

const PATH_KEYS = /^(?:filePath|file_path|path|filename|target|destination|uri|includePattern|globPattern)$/i;
const SHELL_TOOLS = new Set(['bash', 'powershell', 'run_in_terminal', 'execute']);
const DELETE_TOOLS = new Set(['delete', 'delete_file', 'remove_file']);
const DENY = (permissionDecisionReason) => ({ permissionDecision: 'deny', permissionDecisionReason });

function collectValues(value, keyPattern, output = []) {
  if (!value || typeof value !== 'object') return output;
  for (const [key, child] of Object.entries(value)) {
    if (keyPattern.test(key)) {
      if (typeof child === 'string') output.push(child);
      else if (Array.isArray(child)) output.push(...child.filter((item) => typeof item === 'string'));
    }
    collectValues(child, keyPattern, output);
  }
  return output;
}

function collectStrings(value, output = []) {
  if (typeof value === 'string') output.push(value);
  else if (Array.isArray(value)) value.forEach((item) => collectStrings(item, output));
  else if (value && typeof value === 'object') Object.values(value).forEach((item) => collectStrings(item, output));
  return output;
}

function isSensitivePath(value) {
  const normalized = String(value).replace(/\\/g, '/').toLowerCase();
  const segments = normalized.split('/').filter(Boolean);
  return segments.some((segment) => {
    if (/^\.env(?:$|[._-])/.test(segment)) return !/^\.env\.(?:example|sample)$/.test(segment);
    if (['.npmrc', '.pypirc', 'credentials', 'credentials.json', 'secrets.json', 'id_rsa', 'id_ed25519', 'id_ecdsa', 'id_dsa'].includes(segment)) return true;
    if (/^(?:secrets?|credentials?)(?:[._-].*)?$/.test(segment)) return true;
    if (/\.(?:pem|key|p12|pfx|p8|jks|keystore)$/.test(segment)) return true;
    return segment === '.ssh' || segment === '.aws';
  });
}

function containsSensitiveReference(text) {
  const normalized = String(text).replace(/\\/g, '/').toLowerCase();
  const references = normalized.match(/(?:^|[\s"'`=/])(?:\.env(?:\.[a-z0-9_.-]+)?|\.npmrc|\.pypirc|id_(?:rsa|ed25519|ecdsa|dsa)|[a-z0-9_.-]+\.(?:pem|key|p12|pfx|p8|jks|keystore)|(?:credentials|secrets)(?:\.[a-z0-9_.-]+)?\.(?:json|ya?ml|toml|ini|txt))/g) || [];
  return references.some((reference) => {
    const token = reference.trim().replace(/^['"`=]+/, '');
    return isSensitivePath(token);
  });
}

function isDangerousCommand(command) {
  const text = String(command).replace(/\\/g, '/').toLowerCase();
  return [
    /\brm\s+-[^\s]*r[^\s]*(?:\s|$)/,
    /\bremove-item\b(?=[^\n]*(?:-recurse|-r\b|\/s|-force))/,
    /\b(?:rmdir|rd)\b(?=[^\n]*(?:\/s|\/q|-recurse))/,
    /\b(?:del|erase)\b(?=[^\n]*(?:\/s|\*\.[^\s]*|\*\s|\.\s))/,
    /\bgit\s+reset\b[^\n]*--hard/,
    /\bgit\s+clean\b[^\n]*-[^\s]*f/,
    /\bgit\s+rm\b[^\n]*-[^\s]*r/,
    /\bgit\s+checkout\s+--(?:\s|$)/,
    /\bgit\s+restore\b/,
    /\bgit\s+push\b[^\n]*(?:--force(?:-with-lease)?|\s-f(?:\s|$))/,
    /\bgit\s+branch\s+-d\b/,
    /\bgit\s+commit\b[^\n]*--amend/,
    /\bgit\s+rebase\b/,
    /\bgit\s+stash\s+(?:drop|clear)\b/,
    /\b(?:diskpart|clear-disk|format-volume|remove-partition)\b/,
    /\bformat\s+[a-z]:/,
    /\b(?:shutdown|stop-computer)\b/
  ].some((pattern) => pattern.test(text));
}

function stagedPaths(rootDir) {
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

function evaluate(payload) {
  if (!payload || typeof payload !== 'object' || typeof payload.toolName !== 'string' || !('toolArgs' in payload)) {
    return DENY('The security hook received malformed tool input and blocked the operation.');
  }

  const args = payload.toolArgs;
  if (DELETE_TOOLS.has(payload.toolName.toLowerCase())) {
    return DENY('Direct file deletion is blocked; use an explicitly reviewed edit instead.');
  }
  const paths = collectValues(args, PATH_KEYS);
  if (paths.some(isSensitivePath)) return DENY('Access to protected secret files is blocked.');

  const strings = collectStrings(args);
  if (strings.some((value) => /\*\*\*\s*delete file\s*:/i.test(value))) {
    return DENY('Patch requests that delete files are blocked.');
  }
  if (SHELL_TOOLS.has(payload.toolName.toLowerCase())) {
    const command = strings.join('\n');
    if (containsSensitiveReference(command)) return DENY('Commands that access protected secret files are blocked.');
    if (isDangerousCommand(command)) return DENY('This destructive or unsafe command is blocked to protect project and Git data.');
    if (/\bgit\s+commit\b/i.test(command)) {
      if (typeof payload.cwd !== 'string') return DENY('Cannot verify staged files for this commit; the operation is blocked.');
      const files = stagedPaths(payload.cwd);
      if (!files) return DENY('Cannot verify staged files for this commit; the operation is blocked.');
      if (files.some(isSensitivePath)) return DENY('A staged secret file prevents this commit.');
    }
  }

  return {};
}

function main() {
  let payload;
  try {
    payload = JSON.parse(require('node:fs').readFileSync(0, 'utf8'));
  } catch {
    process.stdout.write(JSON.stringify(DENY('The security hook could not parse its input and blocked the operation.')));
    return;
  }
  process.stdout.write(JSON.stringify(evaluate(payload)));
}

if (require.main === module) main();

module.exports = { evaluate, isSensitivePath, isDangerousCommand, stagedPaths };