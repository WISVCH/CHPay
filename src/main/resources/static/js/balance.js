// balance.js - FlyonUI Enhanced Balance Top-up with Radio Button Behavior
'use strict';

// Wrap everything in IIFE to prevent global scope pollution
(function() {
    'use strict';
    
    // Get configuration from global object
    const config = window.BALANCE_CONFIG || {};
    const MAX_BALANCE = config.MAX_BALANCE || 0;
    const MIN_TOPUP = config.MIN_TOPUP || 0;
    const TRANSACTION_FEE = config.TRANSACTION_FEE || 0;
    const CURRENT_BALANCE = config.CURRENT_BALANCE || 0;
    
    let currentErrorType = null;

/**
 * Handle radio button selection
 * @param {number} amount - The amount selected
 */
function handleRadioChange(amount) {
    const topupField = document.getElementById('topupAmount');
    const customInput = document.getElementById('customAmount');
    
    // Check if this radio button is disabled
    const selectedRadio = document.querySelector(`input[name="amountSelection"][value="${amount}"]`);
    if (selectedRadio && selectedRadio.disabled) {
        // Don't process the selection if the button is disabled
        selectedRadio.checked = false;
        return;
    }
    
    // Clear custom input when radio button is selected
    customInput.value = '';
    
    // Clear any error state when selecting a radio button
    clearErrorState();
    currentErrorType = null;
    
    // Update the hidden field for submission
    topupField.value = amount;
    
    updatePaymentSummary();
    validateTopup();
}

/**
 * Handle custom input changes
 */
function handleCustomInput() {
    const topupField = document.getElementById('topupAmount');
    const customInput = document.getElementById('customAmount');
    const radioButtons = document.querySelectorAll('input[name="amountSelection"]');
    
    // Clear all radio button selections when typing in custom input
    radioButtons.forEach(radio => {
        radio.checked = false;
    });
    
    // Normalize commas → dots before storing
    const normalizedValue = customInput.value.replace(/,/g, '.');
    topupField.value = normalizedValue;
    
    updatePaymentSummary();
    validateTopup();
}

/**
 * Handle custom input focus
 */
function handleCustomInputFocus() {
    const radioButtons = document.querySelectorAll('input[name="amountSelection"]');
    
    // Clear all radio button selections when focusing on custom input
    radioButtons.forEach(radio => {
        radio.checked = false;
    });
    
    // Clear error state when focusing on custom input
    clearErrorState();
    currentErrorType = null;
}

/**
 * Update the payment summary with current amount and fees
 */
function updatePaymentSummary() {
    const topupField = document.getElementById('topupAmount');
    const amountToAddElement = document.getElementById('amountToAdd');
    const totalToPayElement = document.getElementById('totalToPay');
    
    if (!topupField || !amountToAddElement || !totalToPayElement) {
        return; // Exit if elements not found
    }
    
    const amount = parseFloat(topupField.value) || 0;
    const transactionFee = TRANSACTION_FEE;
    
    // Update amount to add
    amountToAddElement.textContent = `€${amount.toFixed(2)}`;
    
    // Calculate and update total to pay
    // If amount is 0, total should be 0 (no transaction fee applied)
    const totalToPay = amount === 0 ? 0 : amount + transactionFee;
    if (!isNaN(totalToPay)) {
        totalToPayElement.textContent = `€${totalToPay.toFixed(2)}`;
    } else {
        totalToPayElement.textContent = '€0.00';
    }
}

/**
 * Show error state for the custom amount input
 * @param {string} message - Error message to display
 */
function showErrorState(message) {
    const container = document.getElementById('customAmountContainer');
    const errorElement = document.getElementById('customAmountError');
    
    if (container && errorElement) {
        container.classList.add('is-invalid');
        errorElement.textContent = message;
        errorElement.style.display = 'block';
    }
}

/**
 * Clear error state for the custom amount input
 */
function clearErrorState() {
    const container = document.getElementById('customAmountContainer');
    const errorElement = document.getElementById('customAmountError');
    
    if (container && errorElement) {
        container.classList.remove('is-invalid');
        errorElement.style.display = 'none';
        errorElement.textContent = '';
    }
}

/**
 * Update radio button states based on current balance and max balance
 */
function updateRadioButtonStates() {
    const currentBalance = CURRENT_BALANCE;
    const maxBalance = MAX_BALANCE;
    
    const radioButtons = document.querySelectorAll('input[name="amountSelection"]');
    
    radioButtons.forEach(radio => {
        const amount = parseFloat(radio.value);
        const wouldExceedMax = currentBalance + amount > maxBalance;
        
        radio.disabled = wouldExceedMax;
        
        // Update styling and tooltip based on disabled state
        if (wouldExceedMax) {
            radio.classList.add('btn-disabled');
            radio.title = `Cannot add €${amount} - would exceed maximum balance of €${maxBalance}`;
        } else {
            radio.classList.remove('btn-disabled');
            radio.title = `Add €${amount} to your balance`;
        }
    });
}

/**
 * Validate the top-up amount and update UI accordingly
 */
function validateTopup() {
    const currentBalance   = CURRENT_BALANCE;
    const maxBalance       = MAX_BALANCE;
    const minTopUp         = MIN_TOPUP;

    // Read raw input, normalize commas → dots immediately
    const topupRaw      = document.getElementById('topupAmount').value;
    const normalizedRaw = topupRaw.replace(/,/g, '.');

    const topup         = parseFloat(normalizedRaw);
    const payBtn        = document.getElementById('payButton');

    const decimalPart         = normalizedRaw.includes('.')
        ? normalizedRaw.split('.')[1]
        : '';
    const hasTooMuchPrecision = decimalPart && decimalPart.length > 2;

    // Determine error type and message
    let errorType = null;
    let errorMessage = '';

    if (hasTooMuchPrecision) {
        errorType = 'precision';
        errorMessage = 'Please use at most two decimal digits.';
    } else if (!isNaN(topup) && topup + currentBalance > maxBalance) {
        errorType = 'maxBalance';
        errorMessage = 'Your balance cannot exceed €' + maxBalance + '.';
    } else if (!isNaN(topup) && topup < minTopUp) {
        errorType = 'minAmount';
        errorMessage = 'Minimum top-up amount is €' + minTopUp + '.';
    }

    // Update error state only if error type changed
    if (errorType !== currentErrorType) {
        if (errorType) {
            showErrorState(errorMessage);
        } else {
            clearErrorState();
        }
        currentErrorType = errorType;
    }

    const isValid = !isNaN(topup)
        && topup > 0
        && !hasTooMuchPrecision
        && (topup + currentBalance <= maxBalance)
        && topup >= minTopUp;

    // Update button state with FlyonUI classes
    payBtn.disabled = !isValid;
    if (isValid) {
        payBtn.classList.remove('btn-disabled');
        payBtn.classList.add('btn-primary');
    } else {
        payBtn.classList.add('btn-disabled');
        payBtn.classList.remove('btn-primary');
    }
}

/**
 * Initialize the balance page functionality
 */
document.addEventListener('DOMContentLoaded', function () {
    const form        = document.getElementById('topup-form');
    const customInput = document.getElementById('customAmount');
    const topupField  = document.getElementById('topupAmount');

    if (form) {
        form.addEventListener('submit', function (e) {
            validateTopup();
            if (document.getElementById('payButton').disabled) {
                e.preventDefault();
                return false;
            }
            
            // Show loading state
            const payBtn = document.getElementById('payButton');
            const originalText = payBtn.innerHTML;
            payBtn.innerHTML = '<span class="loading loading-spinner loading-sm"></span> Processing...';
            payBtn.disabled = true;
            
            // Re-enable button after 3 seconds as fallback
            setTimeout(() => {
                payBtn.innerHTML = originalText;
                payBtn.disabled = false;
            }, 3000);
        });
    }

    if (customInput) {
        // When typing in custom input, normalize on update
        customInput.addEventListener('input', handleCustomInput);
    }

    if (topupField) {
        topupField.addEventListener('input', validateTopup);
    }

    // Run once on page load to ensure initial button state is correct
    updateRadioButtonStates();
    updatePaymentSummary();
    validateTopup();
});

    // Expose necessary functions to global scope for HTML event handlers
    window.handleRadioChange = handleRadioChange;
    window.handleCustomInput = handleCustomInput;
    window.handleCustomInputFocus = handleCustomInputFocus;

})(); // End of IIFE