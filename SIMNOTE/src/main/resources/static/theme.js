const THEME_KEY = 'simnote-theme';

function applyTheme(theme) {
    document.documentElement.dataset.theme = theme;
}

function currentTheme() {
    return document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light';
}

applyTheme(localStorage.getItem(THEME_KEY) === 'dark' ? 'dark' : 'light');

document.addEventListener('DOMContentLoaded', () => {
    const toggle = document.getElementById('themeToggle');
    const label = () => {
        toggle.textContent = currentTheme() === 'dark' ? 'Light theme' : 'Dark theme';
    };
    toggle.addEventListener('click', () => {
        const next = currentTheme() === 'dark' ? 'light' : 'dark';
        localStorage.setItem(THEME_KEY, next);
        applyTheme(next);
        label();
    });
    label();
});
