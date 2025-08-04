(function () {
    // If this script has already executed once (i.e. flag exists), abort.
    if (window.__CREATE_PAYMENT_REQUEST_LOADED__) return;
    // otherwise, mark it as loaded so a second <script> tag won't attach listeners twice.
    window.__CREATE_PAYMENT_REQUEST_LOADED__ = true;

    document.addEventListener('DOMContentLoaded', () => {

        // ───────────────────────────── Element handles ───────────────────────── (i love this  comment style)
        const form           = document.getElementById('descForm');
        const descriptionInp = document.getElementById('description');
        const amountInp      = document.getElementById('amount');
        const submitBtn      = document.querySelector('.btn');

        // ─────────────────────────── Validation constants/flags ───────────────
        const descriptionRegex    = /^[a-zA-Z0-9, ]+$/;
        let descriptionValid      = false;
        let amountValid           = false;
        let descriptionToastShown = false;
        let precisionToastShown   = false;

        // helper for submit button
        const updateSubmitState = () => {
            const ok = descriptionValid && amountValid; // form is valid if both fields are valid
            submitBtn.disabled = !ok;
            submitBtn.classList.toggle('disabled', !ok); // enable/disable greyed out css class
        };

        const validateDescription = (silent = false) => {
            const value = descriptionInp.value.trim();

            // check if it's valid & not empty
            descriptionValid = value !== '' && descriptionRegex.test(value);
            descriptionInp.style.borderColor = descriptionValid ? '' : 'red';

            // fire test on first time it's set to invalid & non-silent
            if (!descriptionValid && value !== '' && !descriptionToastShown && !silent) {
                triggerNotificationFromData({
                    type: 'error',
                    message: 'Description can only contain letters, numbers, spaces, and commas'
                });
                descriptionToastShown = true;
            }

            // reset flag when the field is OK *or* empty
            if (descriptionValid || value === '') descriptionToastShown = false;
            updateSubmitState();
            return descriptionValid;
        };

        // neutered balance.js
        const validateAmount = (silent = false) => {
            const raw     = amountInp.value;
            const amount  = parseFloat(raw);
            const dec     = raw.includes('.') ? raw.split('.')[1] : '';
            const tooMany = dec.length > 2;

            amountValid = !isNaN(amount) && amount > 0 && !tooMany;
            amountInp.style.borderColor = amountValid ? '' : 'red';

            if (tooMany && !precisionToastShown && !silent) {
                triggerNotificationFromData({
                    type: 'message',
                    message: 'Please use at most two decimal digits.'
                });
            }
            // reset flag when precision is OK *or* field is empty
            precisionToastShown = tooMany && raw !== '';
            updateSubmitState();
            return amountValid;
        };

        descriptionInp?.addEventListener('input', () => validateDescription());
        amountInp?.addEventListener('input', () => validateAmount());

        form?.addEventListener('submit', e => {
            if (!(validateDescription(true) && validateAmount(true))) {
                e.preventDefault();
                (!descriptionValid ? descriptionInp : amountInp).focus();
            }
        });

        // initial state
        validateDescription(true);
        validateAmount(true);
    });
})();
