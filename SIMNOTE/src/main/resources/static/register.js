const $ = (id) => document.getElementById(id);

const ERRORS = {
    400: 'Check the username and password rules.',
    409: 'Username already taken.',
};

function csrfToken() {
    const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]+)/);
    return match ? decodeURIComponent(match[1]) : '';
}

function post(url, body) {
    return fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': csrfToken() },
        body: JSON.stringify(body),
    });
}

function message(text) {
    $('message').textContent = text;
}

async function createAccount(event) {
    event.preventDefault();
    const username = $('username').value;
    const password = $('password').value;
    if (password !== $('confirmPassword').value) {
        message('Passwords do not match.');
        return;
    }
    const registered = await post('/api/auth/register', { username, password });
    if (!registered.ok) {
        message(ERRORS[registered.status] || 'Account could not be created.');
        return;
    }
    const loggedIn = await post('/api/auth/login', { username, password });
    if (!loggedIn.ok) {
        message('Account created. Please log in.');
        return;
    }
    window.location.href = '/';
}

// The first request makes the server issue the XSRF-TOKEN cookie.
fetch('/api/auth/me').finally(() => {
    $('registerForm').addEventListener('submit', createAccount);
});
