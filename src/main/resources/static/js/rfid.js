// src/main/resources/static/js/rfid.js
;(function() {
    document.addEventListener('DOMContentLoaded', () => {
        console.log("⚡ rfid.js (WebSocket mode) bootstrapped");

        // Find the NFC status icon
        const nfcIcon = document.querySelector('.nfc-icon');
        if (!nfcIcon) {
            console.error('⚠️ .nfc-icon element not found');
            return;
        }

        // CSRF token/header from meta tags
        const csrfMeta   = document.querySelector('meta[name="_csrf"]');
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');
        const csrfToken  = csrfMeta   ? csrfMeta.getAttribute('content')  : null;
        const csrfHeader = headerMeta ? headerMeta.getAttribute('content') : null;

        // Ensure paymentRequestId is defined globally
        if (typeof paymentRequestId === 'undefined') {
            console.error('⚠️ paymentRequestId is not defined!');
            return;
        }

        let socket;

        // Function to (re)connect to the WebSocket server
        function connect() {
            // Close any existing socket
            if (socket && socket.readyState < WebSocket.CLOSED) {
                socket.close();
            }

            socket = new WebSocket('ws://localhost:12345');

            socket.addEventListener('open', () => {
                console.log('🔌 Connected to NFC WebSocket server');
                nfcIcon.style.color = 'var(--green)';
                // Inform server which transaction this client is for
                socket.send(JSON.stringify({
                    type: 'transaction',
                    id: window.paymentRequestId
                }));
            });

            socket.addEventListener('message', async event => {
                let rfid;
                // Ignore the initial "ready" ping or JSON control events
                try {
                    const obj = JSON.parse(event.data);
                    if (obj.event === 'ready') return;
                    if (obj.uid) rfid = obj.uid;
                } catch {
                    // Non-JSON = raw UID string
                    rfid = event.data;
                }
                if (!rfid) return;
                console.log('🎴 RFID UID received for payment:', rfid);

                try {
                    // Build headers
                    const headers = { 'Accept': 'application/json' };
                    if (csrfToken && csrfHeader) {
                        headers[csrfHeader] = csrfToken;
                    }

                    // Call backend payment endpoint
                    const resp = await fetch(
                        `/api/rfid/${encodeURIComponent(rfid)}/pay/${encodeURIComponent(window.paymentRequestId)}`, {
                            method: 'POST',
                            credentials: 'same-origin',
                            headers
                        }
                    );

                    // Parse & validate JSON
                    let data = null;
                    if ([200,400,404,500].includes(resp.status)) {
                        data = await resp.json().catch(() => null);
                    } else {
                        console.error('Unexpected status code:', resp.status);
                    }

                    if (data && typeof data.message === 'string' && typeof data.type === 'string') {
                        triggerNotificationFromData(data);
                        if (data.type === 'success' && !isMultiUse) {
                            setTimeout(() => window.location.href = '/admin/createPaymentRequest', 5000);
                        }
                    } else {
                        console.error('Invalid response payload:', data);
                        triggerNotificationFromData({
                            type: 'error',
                            message: 'Unexpected error occurred'
                        });
                    }
                } catch (err) {
                    console.error('RFID payment error:', err);
                    triggerNotificationFromData({
                        type: 'error',
                        message: 'Unexpected error occurred'
                    });
                }
            });

            socket.addEventListener('error', err => {
                console.error('WebSocket error:', err);
                nfcIcon.style.color = 'var(--red)';
            });

            socket.addEventListener('close', () => {
                console.warn('WebSocket connection closed');
                nfcIcon.style.color = 'var(--red)';
            });
        }

        // Initial connect
        connect();

        // Reconnect when the icon is clicked
        nfcIcon.addEventListener('click', () => {
            console.log('🔄 Reconnecting NFC WebSocket…');
            connect();
        });
    });
})();
