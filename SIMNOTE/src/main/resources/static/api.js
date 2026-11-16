const CSRF_METHODS = ['POST', 'PUT', 'DELETE'];
const API_UNAUTHORIZED_EVENT = 'simnote:unauthorized';
const API_ERROR_MESSAGES = {
    400: 'Check the entered values and try again.',
    401: 'Please log in again.',
    403: 'You do not have permission to do that.',
    404: 'Not found.',
    409: 'That conflicts with existing data.',
};

function csrfToken() {
    const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]+)/);
    return match ? decodeURIComponent(match[1]) : '';
}

// Resolves to { ok, status, data, error }; never rejects.
async function api(method, url, body) {
    const headers = {};
    if (CSRF_METHODS.includes(method)) {
        headers['X-XSRF-TOKEN'] = csrfToken();
    }
    if (body !== undefined) {
        headers['Content-Type'] = 'application/json';
    }
    let response;
    try {
        response = await fetch(url, {
            method,
            headers,
            credentials: 'same-origin',
            body: body === undefined ? undefined : JSON.stringify(body),
        });
    } catch (e) {
        return { ok: false, status: 0, data: null, error: 'Network error. Check your connection.' };
    }

    const text = response.status === 204 ? '' : await response.text();
    let data = null;
    try {
        data = text ? JSON.parse(text) : null;
    } catch (e) {
        data = null;
    }

    // A 401 from login means bad credentials, not an expired session.
    if (response.status === 401 && url !== '/api/auth/login') {
        document.dispatchEvent(new CustomEvent(API_UNAUTHORIZED_EVENT));
    }

    const error = response.ok ? '' : (API_ERROR_MESSAGES[response.status] || 'Request failed.');
    return { ok: response.ok, status: response.status, data, error };
}
