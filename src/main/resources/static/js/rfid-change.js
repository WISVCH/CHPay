// src/main/resources/static/js/rfid-change.js
;(function() {
    document.addEventListener('DOMContentLoaded', () => {
        console.log("⚡ rfid-change.js (WebSocket mode) bootstrapped");

        const changeButton = document.getElementById('change-rfid-button');
        const clearButton = document.getElementById('clear-rfid-button');

        if (!changeButton) {
            console.error('⚠️ #change-rfid-button element not found');
        }

        if (!clearButton) {
            console.error('⚠️ #clear-rfid-button element not found');
        }

        // CSRF token/header from meta tags
        const csrfMeta   = document.querySelector('meta[name="_csrf"]');
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');
        const csrfToken  = csrfMeta   ? csrfMeta.getAttribute('content')  : null;
        const csrfHeader = headerMeta ? headerMeta.getAttribute('content') : null;

        let socket;

        function connect() {
            if (socket && socket.readyState < WebSocket.CLOSED) {
                socket.close();
            }
            socket = new WebSocket('ws://localhost:12345');

            socket.addEventListener('open', () => {
                console.log('🔌 Connected to RFID WebSocket server');
                triggerNotificationFromData({ type: 'success', message: 'Connected to RFID server' });
                // Inform server what we're scanning for
                socket.send(JSON.stringify({
                    type: 'user',
                    id: window.userOpenId
                }));
            });

            socket.addEventListener('message', async event => {
                let rfid;
                try {
                    const obj = JSON.parse(event.data);
                    if (obj.event === 'ready') return;
                    if (obj.uid) rfid = obj.uid;
                } catch {
                    rfid = event.data;
                }
                if (!rfid) return;
                console.log('🎴 RFID UID received for change:', rfid);

                // Notify user that a scan has been received
                triggerNotificationFromData({ type: 'message', message: `RFID UID received: ${rfid}` });

                // Attempt to update the RFID via backend
                try {
                    const headers = { 'Accept': 'application/json' };
                    if (csrfToken && csrfHeader) {
                        headers[csrfHeader] = csrfToken;
                    }

                    const resp = await fetch(
                        `/api/rfid/${encodeURIComponent(rfid)}/change/${encodeURIComponent(window.userOpenId)}`, {
                            method: 'POST',
                            credentials: 'same-origin',
                            headers
                        }
                    );

                    // Handle specific HTTP statuses
                    if (resp.status === 403) {
                        triggerNotificationFromData({ type: 'error', message: 'Not authorized to change RFID' });
                        return;
                    }

                    // Parse JSON if present
                    let data = null;
                    try {
                        data = await resp.json();
                    } catch {
                        console.error('Failed to parse JSON from response');
                    }

                    // Notify based on payload
                    if (data && typeof data.message === 'string' && typeof data.type === 'string') {
                        triggerNotificationFromData(data);
                        if (data.type === 'success') {
                            setTimeout(() => window.location.reload(), 3000);
                        }
                    } else {
                        console.error('Unexpected response or payload', resp.status, data);
                        triggerNotificationFromData({ type: 'error', message: 'Unexpected server response' });
                    }
                } catch (err) {
                    console.error('RFID change error:', err);
                    triggerNotificationFromData({ type: 'error', message: 'Unexpected error occurred' });
                }
            });

            socket.addEventListener('error', err => {
                console.error('WebSocket error:', err);
                triggerNotificationFromData({ type: 'error', message: 'WebSocket connection error' });
            });

            socket.addEventListener('close', () => {
                console.warn('WebSocket connection closed');
                triggerNotificationFromData({ type: 'error', message: 'WebSocket disconnected' });
            });
        }

        // Connect only on button click
        if (changeButton) {
            changeButton.addEventListener('click', () => {
                console.log('🔄 Initiating RFID change via WebSocket…');
                connect();
            });
        }

        // Clear RFID on click
        if (clearButton) {
            clearButton.addEventListener('click', async () => {
                console.log('🗑️ Clearing RFID…');

                if (!confirm('Are you sure you want to clear this user\'s RFID?')) {
                    return;
                }

                const headers = { 'Accept': 'application/json' };
                if (csrfToken && csrfHeader) {
                    headers[csrfHeader] = csrfToken;
                }

                try {
                    const resp = await fetch(
                        `/api/rfid/clear/${encodeURIComponent(window.userOpenId)}`, {
                            method: 'DELETE',
                            credentials: 'same-origin',
                            headers
                        }
                    );

                    if (resp.status === 403) {
                        triggerNotificationFromData({ type: 'error', message: 'Not authorized to clear RFID' });
                        return;
                    }

                    const data = await resp.json();
                    triggerNotificationFromData(data);

                    if (data.type === 'success') {
                        setTimeout(() => window.location.reload(), 2000);
                    }
                } catch (err) {
                    console.error('Error clearing RFID:', err);
                    triggerNotificationFromData({ type: 'error', message: 'Unexpected error occurred' });
                }
            });
        }
    });
})();
