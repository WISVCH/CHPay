(() => {
    const randomInRange = (min, max) => Math.random() * (max - min) + min;

    const parseScalar = (meta) => {
        if (!meta) {
            return undefined;
        }
        const raw = meta.dataset.confettiScalar;
        const value = Number.parseFloat(raw);
        return Number.isFinite(value) && value > 0 ? value : undefined;
    };

    const buildShapes = (meta, scalar) => {
        if (!meta || typeof confetti !== 'function') {
            return [];
        }

        const shapeElements = Array.from(meta.querySelectorAll('[data-confetti-shape]'));
        const shapes = [];

        shapeElements.forEach(element => {
            const type = element.dataset.confettiShapeType;
            const value = element.dataset.confettiShapeValue || '';

            if (type === 'SQUARE') {
                shapes.push('square');
                return;
            }
            if (type === 'CIRCLE') {
                shapes.push('circle');
                return;
            }
            if (type === 'STAR') {
                shapes.push('star');
                return;
            }
            if (type === 'PATH' && value.trim().length > 0) {
                shapes.push(confetti.shapeFromPath({ path: value }));
                return;
            }
            if (type === 'TEXT' && value.trim().length > 0) {
                const options = scalar ? { text: value, scalar } : { text: value };
                shapes.push(confetti.shapeFromText(options));
            }
        });

        return shapes;
    };

    const getColors = (meta) => {
        if (!meta) {
            return [];
        }
        return Array.from(meta.querySelectorAll('[data-confetti-color]'))
            .map(element => element.dataset.confettiColor)
            .filter(color => color && color.length > 0);
    };

    const getDefaultsFromMeta = (meta) => {
        if (!meta || typeof confetti !== 'function') {
            return {};
        }

        const scalar = parseScalar(meta);
        const shapes = buildShapes(meta, scalar);
        const colors = getColors(meta);
        const defaults = {};

        if (colors.length > 0) {
            defaults.colors = colors;
        }
        if (shapes.length > 0) {
            defaults.shapes = shapes;
        }
        if (scalar) {
            defaults.scalar = scalar;
        }

        return defaults;
    };

    const launchAtEvent = (event, defaults, ignoreSelector) => {
        if (ignoreSelector && event.target.closest(ignoreSelector)) {
            return;
        }
        if (typeof confetti !== 'function') {
            return;
        }

        const originX = event.clientX / window.innerWidth;
        const originY = event.clientY / window.innerHeight;

        confetti({
            ...defaults,
            angle: randomInRange(55, 125),
            spread: randomInRange(50, 70),
            particleCount: randomInRange(50, 100),
            origin: { x: originX, y: originY }
        });
    };

    const bindCards = (options = {}) => {
        const cardSelector = options.cardSelector || '[data-confetti-card="true"]';
        const metaSelector = options.metaSelector || '[data-confetti-meta]';
        const ignoreSelector = options.ignoreSelector || 'button, a, form';

        document.querySelectorAll(cardSelector).forEach(card => {
            card.addEventListener('click', event => {
                if (ignoreSelector && event.target.closest(ignoreSelector)) {
                    return;
                }
                const meta = card.querySelector(metaSelector);
                if (!meta) {
                    return;
                }
                const defaults = getDefaultsFromMeta(meta);
                launchAtEvent(event, defaults);
            });
        });
    };

    const bindPreviewButtons = (options = {}) => {
        const buttonSelector = options.buttonSelector || '[data-confetti-preview]';
        const rowSelector = options.rowSelector || 'tr';
        const metaSelector = options.metaSelector || '[data-confetti-meta]';

        document.addEventListener('click', event => {
            const button = event.target.closest(buttonSelector);
            if (!button) {
                return;
            }
            const row = button.closest(rowSelector);
            const meta = row ? row.querySelector(metaSelector) : null;
            if (!meta) {
                return;
            }
            const defaults = getDefaultsFromMeta(meta);
            launchAtEvent(event, defaults);
        });
    };

    const autoBind = () => {
        if (document.querySelector('[data-confetti-card="true"]')) {
            bindCards();
        }
        if (document.querySelector('[data-confetti-preview]')) {
            bindPreviewButtons();
        }
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', autoBind);
    } else {
        autoBind();
    }

    window.confettiUtils = {
        randomInRange,
        getDefaultsFromMeta,
        launchAtEvent,
        bindCards,
        bindPreviewButtons
    };
})();
