let confettiDefaults = {};

// Three bursts: top-left, top-right, then bottom-center
window.addEventListener("load", () => {
    const meta = document.querySelector('[data-confetti-meta]');
    if (window.confettiUtils && typeof window.confettiUtils.getDefaultsFromMeta === 'function') {
        confettiDefaults = window.confettiUtils.getDefaultsFromMeta(meta);
    }

    // top-left
    confetti({
        ...confettiDefaults,
        particleCount: 120,
        angle: 60,
        spread: 70,
        origin: { x: 0.3, y: 0.2 }
    });

    // top-right + 250ms
    setTimeout(() => {
        confetti({
            ...confettiDefaults,
            particleCount: 120,
            angle: 120,
            spread: 70,
            origin: { x: 0.8, y: 0.2 }
        });
    }, 250);

    // bottom-center + 500ms
    setTimeout(() => {
        confetti({
            ...confettiDefaults,
            particleCount: 120,
            spread: 80,
            origin: { x: 0.5, y: 0.8 }
        });
    }, 500);
});

// Function to trigger confetti at click position
function triggerConfettiAtClick(event) {
    if (window.confettiUtils && typeof window.confettiUtils.launchAtEvent === 'function') {
        window.confettiUtils.launchAtEvent(event, confettiDefaults, 'a, button');
    }
}
