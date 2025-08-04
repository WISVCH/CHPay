export function triggerExplosion(targetElement = null) {
    const overlay = document.createElement('div');
    overlay.id = 'explosion-overlay';
    overlay.style.cssText = `
    pointer-events: none;
    position: fixed;
    top: 0; left: 0;
    width: 100vw; height: 100vh;
    z-index: 9999;
    overflow: hidden;
  `;
    document.body.appendChild(overlay);

    // Compute explosion center
    let centerX = window.innerWidth / 2;
    let centerY = window.innerHeight / 2;
    if (targetElement) {
        const rect = targetElement.getBoundingClientRect();
        centerX = rect.left + rect.width / 2;
        centerY = rect.top + rect.height / 2;
    }

    // Flash
    const flash = document.createElement('div');
    flash.className = 'flash';
    flash.style.cssText = `
    position: absolute;
    width: 100%;
    height: 100%;
    background: white;
    opacity: 0;
    animation: flash 0.3s ease-out;
  `;
    overlay.appendChild(flash);

    // Shockwave
    const shockwave = document.createElement('div');
    shockwave.className = 'shockwave';
    shockwave.style.cssText = `
    position: absolute;
    width: 50px;
    height: 50px;
    margin-left: -25px;
    margin-top: -25px;
    border: 5px solid white;
    border-radius: 50%;
    opacity: 0.8;
    transform: scale(0);
    animation: shockwave 0.7s ease-out forwards;
    left: ${centerX}px;
    top: ${centerY}px;
  `;
    overlay.appendChild(shockwave);

    // Particles
    for (let i = 0; i < 100; i++) {
        const particle = document.createElement('div');
        particle.className = 'particle';
        const angle = Math.random() * 2 * Math.PI;
        const distance = Math.random() * 400 + 100;
        const dx = Math.cos(angle) * distance;
        const dy = Math.sin(angle) * distance;
        particle.style.cssText = `
      position: absolute;
      width: 6px;
      height: 6px;
      background: orange;
      border-radius: 50%;
      transform: translate(0, 0);
      animation: particle 1s ease-out forwards;
      left: ${centerX}px;
      top: ${centerY}px;
    `;
        particle.animate([
            { transform: 'translate(0, 0)', opacity: 1 },
            { transform: `translate(${dx}px, ${dy}px) scale(0)`, opacity: 0 }
        ], {
            duration: 1000,
            easing: 'ease-out',
            fill: 'forwards'
        });
        overlay.appendChild(particle);
    }

    // Screen shake
    document.body.classList.add('shake');
    setTimeout(() => {
        document.body.classList.remove('shake');
        overlay.remove();
    }, 1000);
}

// Styles
const style = document.createElement('style');
style.textContent = `
@keyframes flash {
  0% { opacity: 1; }
  100% { opacity: 0; }
}

@keyframes shockwave {
  to {
    transform: scale(20);
    opacity: 0;
  }
}

@keyframes shake {
  0% { transform: translate(0px, 0px); }
  20% { transform: translate(-10px, 5px); }
  40% { transform: translate(10px, -5px); }
  60% { transform: translate(-6px, 4px); }
  80% { transform: translate(6px, -4px); }
  100% { transform: translate(0px, 0px); }
}

.shake {
  animation: shake 0.5s ease-in-out;
}
`;
document.head.appendChild(style);
