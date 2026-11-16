// Usage: node tests/dom/dom-check.mjs   (app must run on SIMPLENOTE_URL, default http://localhost:8080)
import { spawn } from 'node:child_process';
import { existsSync, mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const BASE_URL = process.env.SIMPLENOTE_URL || 'http://localhost:8080';
const DEBUG_PORT = Number(process.env.DOM_CHECK_PORT || 9222);
const CHROME_CANDIDATES = [
    process.env.CHROME_PATH,
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
    '/usr/bin/google-chrome',
    '/usr/bin/chromium',
].filter(Boolean);

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
const q = JSON.stringify;

function connect(url) {
    return new Promise((resolve, reject) => {
        const ws = new WebSocket(url);
        const pending = new Map();
        const listeners = new Map();
        let nextId = 1;
        ws.onmessage = (event) => {
            const msg = JSON.parse(event.data);
            if (msg.method) {
                (listeners.get(msg.method) || []).forEach((fn) => fn(msg.params));
                listeners.delete(msg.method);
                return;
            }
            const waiter = pending.get(msg.id);
            if (!waiter) return;
            pending.delete(msg.id);
            if (msg.error) waiter.reject(new Error(msg.error.message));
            else waiter.resolve(msg.result);
        };
        ws.onopen = () => resolve({
            send(method, params = {}) {
                const id = nextId++;
                ws.send(JSON.stringify({ id, method, params }));
                return new Promise((res, rej) => pending.set(id, { resolve: res, reject: rej }));
            },
            once(method) {
                return new Promise((res) => listeners.set(method, [...(listeners.get(method) || []), res]));
            },
            close: () => ws.close(),
        });
        ws.onerror = () => reject(new Error('DevTools WebSocket connection failed'));
    });
}

async function getPageTarget() {
    for (let attempt = 0; attempt < 50; attempt++) {
        try {
            const response = await fetch(`http://127.0.0.1:${DEBUG_PORT}/json/list`);
            const page = (await response.json()).find((target) => target.type === 'page');
            if (page) return page.webSocketDebuggerUrl;
        } catch (e) {
            // Chrome is still starting.
        }
        await sleep(200);
    }
    throw new Error('Chrome DevTools endpoint did not start');
}

async function evaluate(cdp, expression) {
    const result = await cdp.send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true });
    if (result.exceptionDetails) {
        throw new Error(result.exceptionDetails.exception?.description || result.exceptionDetails.text);
    }
    return result.result.value;
}

async function waitFor(cdp, expression, description, timeoutMs = 10000) {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() < deadline) {
        try {
            if (await evaluate(cdp, expression)) return;
        } catch (e) {
            // Page may still be navigating.
        }
        await sleep(100);
    }
    throw new Error(`Timed out waiting for: ${description}`);
}

async function navigate(cdp, path) {
    const loaded = cdp.once('Page.loadEventFired');
    await cdp.send('Page.navigate', { url: BASE_URL + path });
    await loaded;
}

async function open(cdp, path) {
    await navigate(cdp, path);
    // Wait until the session-restore request has completed on this page load.
    await waitFor(cdp, `performance.getEntriesByType('resource').some((e) => e.name.endsWith('/api/auth/me'))`,
        'session restore request');
}

function expect(actual, expected, label) {
    if (actual !== expected) {
        throw new Error(`${label}: expected ${q(expected)}, got ${q(actual)}`);
    }
}

const fill = (id, value) => `document.getElementById(${q(id)}).value = ${q(value)};`;
const hasNote = (title) =>
    `[...document.querySelectorAll('#notes li strong')].some((e) => e.textContent === ${q(title)})`;
const signedInAs = (username) =>
    `!document.getElementById('app').hidden && document.getElementById('user').textContent === ${q(username)}`;
const clickItemButton = (title, label) => `(() => {
    const item = [...document.querySelectorAll('#notes li')]
        .find((li) => li.querySelector('strong')?.textContent === ${q(title)});
    [...item.querySelectorAll('button')].find((b) => b.textContent === ${q(label)}).click();
})()`;

async function runChecks(cdp) {
    const stamp = Date.now().toString(36);
    const username = `dom${stamp}`;
    const password = 'domcheck1';
    const title = `Title ${stamp}`;
    const edited = `Edited ${stamp}`;

    console.log('- logged-out block: notes API rejects anonymous requests');
    await open(cdp, '/');
    expect(await evaluate(cdp, `fetch('/api/notes').then((r) => r.status)`), 401, 'anonymous /api/notes status');

    console.log('- create account page: register and auto-login');
    await navigate(cdp, '/register.html');
    await waitFor(cdp, `!!document.getElementById('registerForm')`, 'create account page');
    await evaluate(cdp, `${fill('username', username)}${fill('password', password)}${fill('confirmPassword', password)}
        document.getElementById('registerForm').requestSubmit();`);
    await waitFor(cdp, signedInAs(username), 'signed in after register');

    console.log('- create note');
    await evaluate(cdp, `${fill('title', title)}${fill('content', 'First body')}
        document.getElementById('noteForm').requestSubmit();`);
    await waitFor(cdp, hasNote(title), 'new note listed');

    console.log('- edit note');
    await evaluate(cdp, clickItemButton(title, 'Edit'));
    await waitFor(cdp, `document.getElementById('title').value === ${q(title)}`, 'edit form populated');
    await evaluate(cdp, `${fill('title', edited)}document.getElementById('noteForm').requestSubmit();`);
    await waitFor(cdp, `(${hasNote(edited)}) && !(${hasNote(title)})`, 'edited note listed');

    console.log('- reload keeps session and notes');
    await open(cdp, '/');
    await waitFor(cdp, `${signedInAs(username)} && (${hasNote(edited)})`, 'session and note after reload');

    console.log('- delete note');
    await evaluate(cdp, clickItemButton(edited, 'Delete'));
    await waitFor(cdp, `!(${hasNote(edited)})`, 'note removed');

    console.log('- layout: no horizontal overflow at 375 px and 1280 px');
    for (const width of [375, 1280]) {
        await cdp.send('Emulation.setDeviceMetricsOverride', { width, height: 800, deviceScaleFactor: 1, mobile: false });
        await sleep(200);
        expect(await evaluate(cdp, 'document.documentElement.scrollWidth <= window.innerWidth'), true,
            `horizontal overflow at ${width} px`);
    }
    await cdp.send('Emulation.clearDeviceMetricsOverride');

    console.log('- log out blocks the app');
    await evaluate(cdp, `document.getElementById('logoutBtn').click()`);
    await waitFor(cdp, `!document.getElementById('auth').hidden && document.getElementById('app').hidden`,
        'logged-out view');
    await open(cdp, '/');
    expect(await evaluate(cdp, `fetch('/api/notes').then((r) => r.status)`), 401, 'notes status after logout');

    console.log('- log in with the login form');
    await evaluate(cdp, `${fill('username', username)}${fill('password', password)}
        document.getElementById('authForm').requestSubmit();`);
    await waitFor(cdp, signedInAs(username), 'signed in after login');
}

async function main() {
    console.log(`Node ${process.version}, target ${BASE_URL}`);
    const chromePath = CHROME_CANDIDATES.find((path) => existsSync(path));
    if (!chromePath) throw new Error('Chrome not found; set CHROME_PATH');

    const profileDir = mkdtempSync(join(tmpdir(), 'simnote-dom-'));
    const chrome = spawn(chromePath, [
        '--headless=new',
        `--remote-debugging-port=${DEBUG_PORT}`,
        '--remote-allow-origins=*',
        `--user-data-dir=${profileDir}`,
        '--no-first-run',
        '--no-default-browser-check',
        '--disable-gpu',
        'about:blank',
    ], { stdio: 'ignore' });

    let cdp;
    try {
        cdp = await connect(await getPageTarget());
        await cdp.send('Page.enable');
        await runChecks(cdp);
    } finally {
        if (cdp) cdp.close();
        chrome.kill();
        await sleep(500);
        try {
            rmSync(profileDir, { recursive: true, force: true });
        } catch (e) {
            // Chrome may still hold the temporary profile.
        }
    }
}

main().then(
    () => console.log('DOM check passed'),
    (error) => {
        console.error(`DOM check failed: ${error.message}`);
        process.exitCode = 1;
    },
);
