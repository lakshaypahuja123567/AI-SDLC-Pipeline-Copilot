const $ = (id) => document.getElementById(id);

const ERRORS = {
    400: 'Check the username and password rules.',
    401: 'Invalid username or password.',
    409: 'Username already taken.',
};

function csrfToken() {
    const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]+)/);
    return match ? decodeURIComponent(match[1]) : '';
}

async function api(method, url, body) {
    const options = { method, headers: { 'X-XSRF-TOKEN': csrfToken() } };
    if (body !== undefined) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(body);
    }
    const response = await fetch(url, options);
    const text = response.status === 204 ? '' : await response.text();
    let data = null;
    try {
        data = text ? JSON.parse(text) : null;
    } catch (e) {
        data = null;
    }
    return { ok: response.ok, status: response.status, data };
}

function message(text) {
    $('message').textContent = text;
}

function showAuth() {
    $('auth').hidden = false;
    $('app').hidden = true;
}

function showApp(username) {
    $('auth').hidden = true;
    $('app').hidden = false;
    $('user').textContent = username;
    loadNotes();
}

async function login() {
    const body = { username: $('username').value, password: $('password').value };
    const result = await api('POST', '/api/auth/login', body);
    if (!result.ok) {
        message(ERRORS[result.status] || 'Login failed.');
        return;
    }
    message('');
    showApp(result.data.username);
}

async function logout() {
    await api('POST', '/api/auth/logout');
    resetNoteForm();
    showAuth();
}

async function loadNotes() {
    const result = await api('GET', '/api/notes');
    const list = $('notes');
    list.replaceChildren();
    (result.data || []).forEach((note) => {
        const item = document.createElement('li');
        const title = document.createElement('strong');
        title.textContent = note.title;
        const content = document.createElement('p');
        content.textContent = note.content;
        const edit = document.createElement('button');
        edit.type = 'button';
        edit.textContent = 'Edit';
        edit.addEventListener('click', () => fillNoteForm(note));
        const remove = document.createElement('button');
        remove.type = 'button';
        remove.textContent = 'Delete';
        remove.addEventListener('click', () => deleteNote(note.id));
        item.append(title, content, edit, remove);
        list.append(item);
    });
}

function fillNoteForm(note) {
    $('noteId').value = note.id;
    $('title').value = note.title;
    $('content').value = note.content;
}

function resetNoteForm() {
    $('noteId').value = '';
    $('noteForm').reset();
}

async function saveNote(event) {
    event.preventDefault();
    const id = $('noteId').value;
    const body = { title: $('title').value, content: $('content').value };
    const result = id
        ? await api('PUT', `/api/notes/${id}`, body)
        : await api('POST', '/api/notes', body);
    if (!result.ok) {
        message(`Could not save note (${result.status}).`);
        return;
    }
    resetNoteForm();
    loadNotes();
}

async function deleteNote(id) {
    const result = await api('DELETE', `/api/notes/${id}`);
    if (!result.ok) {
        message(`Could not delete note (${result.status}).`);
        return;
    }
    loadNotes();
}

$('authForm').addEventListener('submit', (event) => {
    event.preventDefault();
    login();
});
$('logoutBtn').addEventListener('click', logout);
$('noteForm').addEventListener('submit', saveNote);
$('cancelEdit').addEventListener('click', resetNoteForm);

// First request also makes the server issue the XSRF-TOKEN cookie.
api('GET', '/api/auth/me').then((result) => {
    if (result.ok) {
        showApp(result.data.username);
    } else {
        showAuth();
    }
});
