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
        case 'warning': return 'icon-[tabler--alert-triangle]';
        case 'message': return 'icon-[tabler--info-circle]';
        default: return 'icon-[tabler--bell]';
    }
}

function getAlertClass(type) {
    switch (type) {
        case 'success': return 'alert-soft alert-success';
        case 'error': return 'alert-soft alert-error';
        case 'warning': return 'alert-soft alert-warning';
        case 'message': return 'alert-soft alert-info';
        default: return 'alert-soft alert-info';
    }
}

function triggerNotificationFromData(data) {
    const container = document.querySelector('.notification-container');
    const msg = typeof data.message === 'string' ? data.message.trim() : '';
    const alertId = 'alert-' + Date.now();

    // Create the new notification using FlyonUI alert
    const notif = document.createElement('div');
    notif.className = `alert ${getAlertClass(data.type)} flex items-center gap-4 transition duration-300 ease-in-out`;
    notif.setAttribute('role', 'alert');
    notif.setAttribute('id', alertId);
    
    // Set initial state for entrance animation
    notif.style.transform = 'translateX(100%)';
    notif.style.opacity = '0';
    
    const icon = document.createElement('span');
    icon.className = `${getIconClass(data.type)} shrink-0 size-6`;

    const message = document.createElement('p');
    message.textContent = msg;

    const closeButton = document.createElement('button');
    closeButton.className = 'ms-auto cursor-pointer leading-none';
    closeButton.setAttribute('aria-label', 'Close Button');
    closeButton.addEventListener('click', () => dismissNotification(alertId));

    const closeIcon = document.createElement('span');
    closeIcon.className = 'icon-[tabler--x] size-5';
    closeButton.appendChild(closeIcon);

    notif.appendChild(icon);
    notif.appendChild(message);
    notif.appendChild(closeButton);
    
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
