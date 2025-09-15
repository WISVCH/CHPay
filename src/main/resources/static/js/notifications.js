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
        case 'success': return 'icon-[tabler--circle-check]';
        case 'error': return 'icon-[tabler--circle-x]';
        case 'message': return 'icon-[tabler--info-circle]';
        default: return 'icon-[tabler--bell]';
    }
}

function getAlertClass(type) {
    switch (type) {
        case 'success': return 'alert-soft alert-success';
        case 'error': return 'alert-soft alert-error';
        case 'message': return 'alert-soft alert-info';
        default: return 'alert-soft alert-info';
    }
}

function triggerNotificationFromData(data) {
    const container = document.querySelector('.notification-container');
    const msg = data.message.trim();
    const alertId = 'alert-' + Date.now();

    // Create the new notification using FlyonUI alert
    const notif = document.createElement('div');
    notif.className = `alert ${getAlertClass(data.type)} flex items-center gap-4 transition duration-300 ease-in-out`;
    notif.setAttribute('role', 'alert');
    notif.setAttribute('id', alertId);
    
    // Set initial state for entrance animation
    notif.style.transform = 'translateX(100%)';
    notif.style.opacity = '0';
    
    notif.innerHTML = `
        <span class="${getIconClass(data.type)} shrink-0 size-6"></span>
        <p>${msg}</p>
        <button class="ms-auto cursor-pointer leading-none" onclick="dismissNotification('${alertId}')" aria-label="Close Button">
            <span class="icon-[tabler--x] size-5"></span>
        </button>
    `;
    
    container.appendChild(notif);

    // Trigger entrance animation
    setTimeout(() => {
        notif.style.transform = 'translateX(0)';
        notif.style.opacity = '1';
    }, 10);

    // Auto-remove after 5 seconds
    setTimeout(() => {
        dismissNotification(alertId);
    }, 5000);
}

function dismissNotification(alertId) {
    const notif = document.getElementById(alertId);
    if (notif) {
        // Add removing classes for animation
        notif.style.transform = 'translateX(100%)';
        notif.style.opacity = '0';
        
        // Remove after animation completes
        setTimeout(() => {
            if (notif.parentElement) {
                notif.remove();
            }
        }, 300);
    }
}

