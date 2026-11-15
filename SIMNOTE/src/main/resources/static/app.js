const $ = (id) => document.getElementById(id);

const ERRORS = {
    400: 'Check the username and password rules.',
    401: 'Invalid username or password.',
    409: 'Username already taken.',
};

function message(text) {
    $('message').textContent = text;
}

function reportError(action, result) {
    // A 401 already returns the user to the login view; do not overwrite that state.
    if (result.status !== 401) {
        message(`Could not ${action} (${result.status}).`);
    }
}

function showAuth() {
    $('auth').hidden = false;
    $('app').hidden = true;
    $('notes').replaceChildren();
    resetNoteForm();
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
    $('password').value = '';
    showApp(result.data.username);
}

async function logout() {
    await api('POST', '/api/auth/logout');
    $('password').value = '';
    message('');
    showAuth();
}

async function loadNotes() {
    const result = await api('GET', '/api/notes');
    const list = $('notes');
    list.replaceChildren();
    if (!result.ok) {
        reportError('load notes', result);
        return;
    }
    result.data.forEach((note) => {
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
        reportError('save note', result);
        return;
    }
    resetNoteForm();
    loadNotes();
}

async function deleteNote(id) {
    const result = await api('DELETE', `/api/notes/${id}`);
    if (!result.ok) {
        reportError('delete note', result);
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

document.addEventListener('simnote:unauthorized', () => {
    if (!$('app').hidden) {
        message('Your session has ended. Please log in again.');
    }
    showAuth();
});

async function checkSession() {
    const result = await api('GET', '/api/auth/me');
    if (result.ok) {
        if ($('app').hidden) {
            showApp(result.data.username);
        }
    } else if (result.status === 401 || $('app').hidden) {
        showAuth();
    }
}

// The first request also makes the server issue the XSRF-TOKEN cookie.
checkSession();
window.addEventListener('focus', checkSession);
