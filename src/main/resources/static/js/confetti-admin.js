document.addEventListener('DOMContentLoaded', function () {
    const forms = document.querySelectorAll('[data-confetti-form]');

    const PATH_PLACEHOLDER = 'SVG path data (e.g. M10 10H90V90H10Z)';
    const TEXT_PLACEHOLDER = 'Text for confetti shape';
    const DEFAULT_PLACEHOLDER = 'Value';

    const parseColors = (value) => value.split(',')
        .map(item => item.trim())
        .filter(item => item.length > 0);

    const isValidColor = (value) => /^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/.test(value);

    const getShapeWrapper = (form) => form.querySelector('[data-confetti-shape-list]');

    const getShapeRows = (form) => {
        const wrapper = getShapeWrapper(form);
        if (!wrapper) return [];
        return Array.from(wrapper.querySelectorAll('[data-confetti-shape-row]'));
    };

    const updateShapeRow = (row) => {
        const select = row.querySelector('[data-confetti-shape-type]');
        const input = row.querySelector('[data-confetti-shape-value]');
        if (!select || !input) return;

        const type = (select.value || '').toUpperCase();
        if (type === 'PATH') {
            input.classList.remove('hidden');
            input.placeholder = PATH_PLACEHOLDER;
        } else if (type === 'TEXT') {
            input.classList.remove('hidden');
            input.placeholder = TEXT_PLACEHOLDER;
        } else {
            input.classList.add('hidden');
            input.placeholder = DEFAULT_PLACEHOLDER;
            input.value = '';
        }
    };

    const initializeShapeRows = (form) => {
        const wrapper = getShapeWrapper(form);
        if (!wrapper) return;

        wrapper.querySelectorAll('[data-confetti-shape-row]').forEach(row => {
            if (row.hasAttribute('data-confetti-template')) {
                row.removeAttribute('data-confetti-template');
                row.removeAttribute('id');
                row.classList.remove('hidden');
                row.querySelectorAll('[disabled]').forEach(input => {
                    input.disabled = false;
                });
            }
            updateShapeRow(row);
        });
    };

    const validateShapes = (form) => {
        const shapeSelection = form.querySelector('#shapeSelection');
        if (!shapeSelection) return true;

        const rows = getShapeRows(form);
        const hasRows = rows.length > 0;
        const hasMissingValues = rows.some(row => {
            const select = row.querySelector('[data-confetti-shape-type]');
            const input = row.querySelector('[data-confetti-shape-value]');
            if (!select) return false;
            const type = (select.value || '').toUpperCase();
            if (type === 'PATH' || type === 'TEXT') {
                return !input || input.value.trim().length === 0;
            }
            return false;
        });

        const isValid = hasRows && !hasMissingValues;
        shapeSelection.setCustomValidity(isValid ? '' : 'Add at least one shape and provide values for Path/Text shapes');
        shapeSelection.value = isValid ? 'ok' : '';
        return isValid;
    };

    forms.forEach(form => {
        const nameInput = form.querySelector('#name');
        const colorsInput = form.querySelector('#colors');
        const scalarInput = form.querySelector('#scalar');
        const minTransactionsInput = form.querySelector('#minTransactions');

        const validateName = () => {
            if (!nameInput) return true;
            const isValid = nameInput.value.trim().length > 0;
            nameInput.setCustomValidity(isValid ? '' : 'Name is required');
            return isValid;
        };

        const validateColors = () => {
            if (!colorsInput) return true;
            const colors = parseColors(colorsInput.value);
            const isValid = colors.length > 0 && colors.every(isValidColor);
            colorsInput.setCustomValidity(isValid ? '' : 'Enter at least one valid hex color');
            return isValid;
        };

        const validateScalar = () => {
            if (!scalarInput) return true;
            const value = Number.parseFloat(scalarInput.value);
            const isValid = Number.isFinite(value) && value > 0;
            scalarInput.setCustomValidity(isValid ? '' : 'Enter a scalar greater than 0');
            return isValid;
        };

        const validateMinTransactions = () => {
            if (!minTransactionsInput) return true;
            const rawValue = minTransactionsInput.value.trim();
            const isValid = /^[0-9]+$/.test(rawValue);
            minTransactionsInput.setCustomValidity(isValid ? '' : 'Enter 0 or a positive whole number');
            return isValid;
        };

        initializeShapeRows(form);
        validateShapes(form);
        validateScalar();
        validateMinTransactions();

        nameInput?.addEventListener('input', validateName);
        colorsInput?.addEventListener('input', validateColors);
        scalarInput?.addEventListener('input', validateScalar);
        minTransactionsInput?.addEventListener('input', validateMinTransactions);

        form.addEventListener('change', (event) => {
            const target = event.target;
            if (!(target instanceof HTMLElement)) return;
            if (target.matches('[data-confetti-shape-type]')) {
                const row = target.closest('[data-confetti-shape-row]');
                if (row) {
                    updateShapeRow(row);
                }
                validateShapes(form);
            }
        });

        form.addEventListener('input', (event) => {
            const target = event.target;
            if (!(target instanceof HTMLElement)) return;
            if (target.matches('[data-confetti-shape-value]')) {
                validateShapes(form);
            }
        });

        const copyTrigger = form.querySelector('[data-copy-markup]');
        copyTrigger?.addEventListener('click', () => {
            setTimeout(() => {
                initializeShapeRows(form);
                validateShapes(form);
            }, 0);
        });

        form.addEventListener('click', (event) => {
            const target = event.target;
            if (!(target instanceof HTMLElement)) return;
            if (target.closest('[data-copy-markup-delete-item]')) {
                setTimeout(() => {
                    validateShapes(form);
                }, 0);
            }
        });

        form.addEventListener('submit', (event) => {
            const nameValid = validateName();
            const colorsValid = validateColors();
            const scalarValid = validateScalar();
            const minTransactionsValid = validateMinTransactions();
            const shapesValid = validateShapes(form);

            if (!nameValid || !colorsValid || !scalarValid || !minTransactionsValid || !shapesValid) {
                event.preventDefault();
                event.stopPropagation();
                if (!nameValid && nameInput) {
                    nameInput.focus();
                } else if (!colorsValid && colorsInput) {
                    colorsInput.focus();
                } else if (!scalarValid && scalarInput) {
                    scalarInput.focus();
                } else if (!minTransactionsValid && minTransactionsInput) {
                    minTransactionsInput.focus();
                } else {
                    const wrapper = getShapeWrapper(form);
                    const firstRow = wrapper?.querySelector('[data-confetti-shape-row]');
                    const select = firstRow?.querySelector('[data-confetti-shape-type]');
                    const input = firstRow?.querySelector('[data-confetti-shape-value]');
                    const type = select?.value?.toUpperCase();
                    if ((type === 'PATH' || type === 'TEXT') && input && input.value.trim().length === 0) {
                        input.focus();
                    } else {
                        select?.focus();
                    }
                }
            }
            form.classList.add('validate');
        });
    });
});
