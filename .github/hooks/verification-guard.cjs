'use strict';

const fs = require('node:fs');
const path = require('node:path');

const REQUIRED_SECTIONS = ['Build', 'Tests', 'Documentation'];
const VALID_STATUSES = new Set(['PASS', 'FAIL', 'PARTIAL', 'NOT_APPLICABLE', 'NOT_RUN']);
const OVERALL_STATUSES = VALID_STATUSES;

function block(reason) {
  return { decision: 'block', reason };
}

function meaningful(value) {
  const normalized = String(value || '').trim().toLowerCase();
  return normalized.length > 0 && !/^(?:none|n\/a|na|tbd|todo|not provided|placeholder|unknown)$/.test(normalized);
}

function parseSection(report, title) {
  const escaped = title.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const heading = new RegExp(`^#{1,4}\\s+${escaped}\\s*\\r?\\n`, 'im').exec(report);
  if (!heading) return null;
  const remainder = report.slice(heading.index + heading[0].length);
  const nextHeading = /^#{1,4}\s+/m.exec(remainder);
  const section = nextHeading ? remainder.slice(0, nextHeading.index) : remainder;
  const status = section.match(/^\s*(?:[-*]\s*)?Status\s*:\s*(PASS|FAIL|PARTIAL|NOT_APPLICABLE|NOT_RUN)\s*$/im);
  const evidence = section.match(/^\s*(?:[-*]\s*)?Evidence\s*:\s*(.+?)\s*$/im);
  const reason = section.match(/^\s*(?:[-*]\s*)?Reason\s*:\s*(.+?)\s*$/im);
  return {
    status: status ? status[1].toUpperCase() : null,
    evidence: evidence ? evidence[1].trim() : '',
    reason: reason ? reason[1].trim() : ''
  };
}

function validateReport(report) {
  if (typeof report !== 'string' || report.trim().length === 0) return 'The verification report is empty.';
  const overall = parseSection(report, 'Overall');
  if (!overall) return 'The verification report is missing the Overall section.';
  if (!overall.status || !OVERALL_STATUSES.has(overall.status)) return 'The Overall status must be PASS, FAIL, PARTIAL, NOT_APPLICABLE, or NOT_RUN.';
  if (overall.status === 'PASS' && !meaningful(overall.evidence)) return 'The Overall PASS result requires evidence.';
  if (overall.status !== 'PASS' && !meaningful(overall.evidence || overall.reason)) return `The Overall ${overall.status} result requires evidence or a reason.`;

  const sections = [];
  for (const title of REQUIRED_SECTIONS) {
    const section = parseSection(report, title);
    if (!section) return `The verification report is missing the ${title} section.`;
    if (!section.status || !VALID_STATUSES.has(section.status)) return `The ${title} status must be PASS, FAIL, PARTIAL, NOT_APPLICABLE, or NOT_RUN.`;
    if (section.status === 'PASS' && !meaningful(section.evidence)) return `The ${title} PASS result requires evidence.`;
    if ((section.status === 'FAIL' || section.status === 'PARTIAL' || section.status === 'NOT_APPLICABLE' || section.status === 'NOT_RUN') && !meaningful(section.evidence || section.reason)) {
      return `The ${title} ${section.status} result requires evidence or a reason.`;
    }
    sections.push(section);
  }
  if (overall.status === 'PASS' && sections.some((section) => section.status === 'FAIL' || section.status === 'NOT_RUN')) {
    return 'Overall PASS conflicts with a failed or unexecuted Build, Tests, or Documentation check.';
  }
  if (overall.status === 'PASS' && sections.some((section) => section.status !== 'PASS' && section.status !== 'NOT_APPLICABLE')) {
    return 'Overall PASS requires every check to PASS or be explicitly NOT_APPLICABLE.';
  }
  return null;
}

function evaluate(payload) {
  if (!payload || typeof payload !== 'object' || typeof payload.cwd !== 'string') {
    return block('The verification hook received malformed input. Provide a complete verification report before finishing.');
  }

  const reportPath = path.join(payload.cwd, '.github', 'sdlc', 'verification-report.md');
  let report;
  try {
    report = fs.readFileSync(reportPath, 'utf8');
  } catch {
    return block('Create .github/sdlc/verification-report.md and record Build, Tests, and Documentation statuses with evidence before finishing.');
  }

  const error = validateReport(report);
  if (error) return block(`${error} Update .github/sdlc/verification-report.md before finishing.`);
  return {};
}

function main() {
  let payload;
  try {
    payload = JSON.parse(fs.readFileSync(0, 'utf8'));
  } catch {
    process.stdout.write(JSON.stringify(block('The verification hook could not parse its input. Provide the required verification report before finishing.')));
    return;
  }
  process.stdout.write(JSON.stringify(evaluate(payload)));
}

if (require.main === module) main();

module.exports = { evaluate, validateReport, parseSection };