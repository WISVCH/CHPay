// qr.js

;(function() {

    // Grab the paymentRequestId from a global variable that we’ll set in the Thymeleaf template
    if (!window.paymentRequestId) {
        console.error("qr.js: window.paymentRequestId is not defined. Make sure you set it in the template.");
        return;
    }
    const requestId = window.paymentRequestId;

    // Keep track of seen transactions
    const seenTxIds = new Set();

    function showNotification(payerName, amount) {
        // Build a simple “success” notification payload:
        const payload = {
            type: "success",
            message: `${payerName} just paid €${amount}`
        };

        triggerNotificationFromData(payload);
    }

    // Poll for new query results
    function pollPayments() {
        fetch(`/api/requests/${requestId}/successful-payments`, {
            credentials: "same-origin" // ensure cookies/session are sent
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}`);
                }
                return response.json();
            })
            .then(payments => {
                // payments is an array: [{ transactionId, payerName, amount, timestamp }, …]
                payments.forEach(payment => {
                    const txId = payment.transactionId;
                    if (!seenTxIds.has(txId)) {
                        // New payment: show a notification
                        showNotification(payment.payerName, payment.amount);
                        seenTxIds.add(txId);

                        if (!isMultiUse) {
                            setTimeout(() => {
                                window.location.href = "/admin/createPaymentRequest";
                            }, 7000);
                            return;
                        }
                    }
                });
            })
            .catch(err => {
                console.error("qr.js polling error:", err);
            });
    }

    // Poll for every 5 seconds
    document.addEventListener("DOMContentLoaded", () => {
        pollPayments();                       // populate seenTxIds on load
        setInterval(pollPayments, 5000);
    });
})();
