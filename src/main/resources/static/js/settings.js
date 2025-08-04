/* settings.js – v2
 * Enables submit when *either* a valid balance is entered OR the
 * freeze-toggle changes from its initial state.
 */
(function () {
    if (window.__SETTINGS_JS_LOADED__) return;
    window.__SETTINGS_JS_LOADED__ = true;

    document.addEventListener('DOMContentLoaded', () => {

        const form        = document.querySelector('form');
        const balanceInp  = document.getElementById('maximumBalance');
        const toggleInp   = document.getElementById('isFrozen');
        const submitBtn   = document.querySelector('.submit-btn');


        let balanceValid        = false;
        let toggleChanged       = false;
        let precisionToastShown = false;

        const initialToggleState = toggleInp?.checked ?? false;

        // Enable/disable button
        const updateSubmitState = () => {
            const ok = balanceValid || toggleChanged;
            submitBtn.disabled = !ok;
            submitBtn.classList.toggle('disabled', !ok);
        };

        // Validate number field (positive, ≤2 decimals)
        const validateBalance = (silent = false) => {
            const raw      = balanceInp.value.trim();
            const amount   = parseFloat(raw);
            const decPart  = raw.includes('.') ? raw.split('.')[1] : '';
            const tooMany  = decPart.length > 2;

            balanceValid = raw !== '' && !isNaN(amount) && amount > 0 && !tooMany;

            // Only colour red when user typed something
            balanceInp.style.borderColor = (!balanceValid && raw !== '') ? 'red' : '';

            if (tooMany && !precisionToastShown && !silent) {
                triggerNotificationFromData({
                    type: 'message',
                    message: 'Please use at most two decimal digits.'
                });
            }
            precisionToastShown = tooMany && raw !== '';

            updateSubmitState();
            return balanceValid;
        };

        //  toggle interaction
        const handleToggleChange = () => {
            toggleChanged = (toggleInp.checked !== initialToggleState);
            updateSubmitState();
        };


        balanceInp?.addEventListener('input', () => validateBalance());
        toggleInp?.addEventListener('change', handleToggleChange);

        form?.addEventListener('submit', e => {

            const balanceOK = validateBalance(true);
            if (!(balanceOK || toggleChanged)) {
                e.preventDefault();
                // Focus on whichever control still needs attention
                (balanceOK ? toggleInp : balanceInp).focus();
            }
        });

        // Initial button state
        validateBalance(true);
        handleToggleChange();
    });
})();
