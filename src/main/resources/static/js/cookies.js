/* ── banner ---------------------------------------------------------------- */
function dismissBanner() {
    /* Set cookie for a year */
    document.cookie =
        "cookie_consent=true; path=/; max-age=" + 60*60*24*365 + "; SameSite=Lax";

    /* remove element when clicked */
    const b = document.getElementById("cookie-banner");
    b.classList.remove("cookie-visible");
    b.addEventListener("transitionend", () => b.remove(), { once: true });
}

/* ── modal / policy window ------------------------------------------------- */
function openCookiePolicy() {
    const modal = document.getElementById("cookie-policy-modal");
    if (modal) modal.classList.add("show");
}

function closeCookiePolicy() {
    const modal = document.getElementById("cookie-policy-modal");
    if (modal) modal.classList.remove("show");
}

/* fade-in on load */
window.addEventListener("load", () => {
    const b = document.getElementById("cookie-banner");
    if (b) b.classList.add("cookie-visible");
});