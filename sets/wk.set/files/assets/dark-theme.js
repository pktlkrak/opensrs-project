function loadTheme() {
    const theme = localStorage.getItem("theme");
    if(theme) {
        window.document.body.parentElement.setAttribute("data-theme", theme);
    }
}

function toggleDarkTheme() {
    if(localStorage.getItem("theme") === 'dark') {
        localStorage.setItem("theme", "light");
    } else {
        localStorage.setItem("theme", "dark");
    }
    loadTheme();
}


loadTheme();