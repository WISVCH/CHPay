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

// Function to trigger confetti at click position
function triggerConfettiAtClick(event) {
    // Don't trigger confetti if clicking on buttons or links
    if (event.target.closest('a, button')) {
        return;
    }
    
    function randomInRange(min, max) {
        return Math.random() * (max - min) + min;
    }

    // Get click position relative to the viewport
    const x = event.clientX / window.innerWidth;
    const y = event.clientY / window.innerHeight;

    confetti({
        angle: randomInRange(55, 125),
        spread: randomInRange(50, 70),
        particleCount: randomInRange(50, 100),
        origin: { x: x, y: y }
    });
}
