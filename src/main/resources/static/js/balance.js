// topup.js
'use strict';

/**
 * Called by your input’s oninput="clearSelection()"
 */
function clearSelection() {
    document.querySelectorAll('.btn').forEach(btn => btn.classList.remove('selected'));
}

function updateTopupAmount(amount, fromCustom = false) {
    const topupField  = document.getElementById('topupAmount');
    const customInput = document.getElementById('customAmount');

    // Clear selection of all buttons
    clearSelection();

    if (fromCustom) {
        // Normalize commas → dots before storing
        topupField.value = customInput.value.replace(/,/g, '.');
        customInput.placeholder = '€';
    } else {
        // Mark the clicked button
        event.target.classList.add('selected');

        // Clear any typed value, then set placeholder to show the clicked amount
        if (customInput) {
            customInput.value       = '';
            customInput.placeholder = '€' + amount;
        }

        // Also update the hidden field for submission
        topupField.value = amount;
    }

    validateTopup();
}

let precisionWarningShown = false;
let maxAmountWarningShown = false;
let minAmountWarningShown = false;

function validateTopup() {
    const balanceContainer = document.querySelector('.balance-container');
    const currentBalance   = parseFloat(balanceContainer.dataset.currentBalance);
    const maxBalance       = typeof MAX_BALANCE !== 'undefined' ? MAX_BALANCE : 35;
    const minTopUp         = typeof MIN_TOPUP    !== 'undefined' ? MIN_TOPUP    : 1;

    // Read raw input, normalize commas → dots immediately
    const topupRaw      = document.getElementById('topupAmount').value;
    const normalizedRaw = topupRaw.replace(/,/g, '.');

    const topup         = parseFloat(normalizedRaw);
    const payBtn        = document.getElementById('payButton');

    const decimalPart         = normalizedRaw.includes('.')
        ? normalizedRaw.split('.')[1]
        : '';
    const hasTooMuchPrecision = decimalPart.length > 2;

    // deploy the notification only on the transition to “invalid”
    if (hasTooMuchPrecision && !precisionWarningShown) {
        triggerNotificationFromData({
            type: 'message',
            message: 'Please use at most two decimal digits.',
        });
        precisionWarningShown = true;
    }

    if (!isNaN(topup) && topup + currentBalance > maxBalance && !maxAmountWarningShown) {
        triggerNotificationFromData({
            type: 'message',
            message: 'Your balance cannot exceed ' +  maxBalance + " euros.",
        });
        maxAmountWarningShown = true;
    }

    if (!isNaN(topup) && topup < minTopUp && !minAmountWarningShown) {
        triggerNotificationFromData({
            type: 'message',
            message: 'Minimum top-up amount is ' +  minTopUp + " euros.",
        });
        minAmountWarningShown = true;
    }

    const isValid = !isNaN(topup)
        && topup > 0
        && !hasTooMuchPrecision
        && (topup + currentBalance <= maxBalance)
        && topup >= minTopUp;

    payBtn.disabled = !isValid;
    payBtn.classList.toggle('disabled', !isValid);
}

document.addEventListener('DOMContentLoaded', function () {
    const form        = document.getElementById('topup-form');
    const customInput = document.getElementById('customAmount');
    const topupField  = document.getElementById('topupAmount');

    if (form) {
        form.addEventListener('submit', function (e) {
            validateTopup();
            if (document.getElementById('payButton').disabled) e.preventDefault();
        });
    }

    if (customInput) {
        // when typing in custom input, normalize on update
        customInput.addEventListener('input', () => updateTopupAmount(null, true));
    }

    if (topupField) {
        topupField.addEventListener('input', validateTopup);
    }

    // Run once on page load to ensure initial button state is correct
    validateTopup();
});
