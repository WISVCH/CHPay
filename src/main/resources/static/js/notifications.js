function triggerNotification(endpoint) {
    fetch(endpoint)
        .then(async res => {
            const data = await res.json();
            triggerNotificationFromData(data);
        })
        .catch(err => {
            console.error('Fetch failed:', err);
            triggerNotificationFromData({
                type: 'error',
                message: 'Unexpected error occurred',
            });
        });
}


function getIconClass(type) {
    switch (type) {
        case 'success': return 'fa-circle-check';
        case 'error': return 'fa-circle-xmark';
        case 'message': return 'fa-circle-info';
        default: return 'fa-bell';
    }
}

function triggerNotificationFromData(data) {
    const container = document.querySelector('.notification-container');
    const msg = data.message.trim();


    // Create the new notification
    const notif = document.createElement('div');
    notif.className = `notification ${data.type}`;
    notif.innerHTML = `
    <i class="fa-solid ${getIconClass(data.type)} notification-icon"></i>
    <span>${msg}</span>
    <button class="notification-close" onclick="this.parentElement.remove()">×</button>
    <div class="notification-timer"></div>
  `;
    container.appendChild(notif);

    // Fade‐out/removal logic
    let fadeTimeout = setTimeout(() => notif.classList.add('fade-out'), 3000);
    let removeTimeout = setTimeout(() => notif.remove(), 3500);

    notif.addEventListener('mouseenter', () => {
        notif.classList.add('paused');
        clearTimeout(fadeTimeout);
        clearTimeout(removeTimeout);
    });
    notif.addEventListener('mouseleave', () => {
        notif.classList.remove('paused');
        fadeTimeout = setTimeout(() => notif.classList.add('fade-out'), 3000);
        removeTimeout = setTimeout(() => notif.remove(), 3500);
    });
}

