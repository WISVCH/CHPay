// Three bursts: top-left, top-right, then bottom-center
window.addEventListener("load", () => {
    // top-left
    confetti({
        particleCount: 120,
        angle: 60,
        spread: 70,
        origin: { x: 0.3, y: 0.2 }
    });

    // top-right + 250ms
    setTimeout(() => {
        confetti({
            particleCount: 120,
            angle: 120,
            spread: 70,
            origin: { x: 0.8, y: 0.2 }
        });
    }, 250);

    // bottom-center + 500ms
    setTimeout(() => {
        confetti({
            particleCount: 120,
            spread: 80,
            origin: { x: 0.5, y: 0.8 }
        });
    }, 500);
});
