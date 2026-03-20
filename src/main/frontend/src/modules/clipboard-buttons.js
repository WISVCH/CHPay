function getClipboardText(trigger) {
  const clipboardText = trigger.dataset.clipboardText;
  if (clipboardText) {
    return clipboardText;
  }

  const clipboardTarget = trigger.dataset.clipboardTarget;
  if (!clipboardTarget) {
    return "";
  }

  const targetElement = document.querySelector(clipboardTarget);
  if (!targetElement) {
    return "";
  }

  if (
    targetElement instanceof HTMLInputElement ||
    targetElement instanceof HTMLTextAreaElement ||
    targetElement instanceof HTMLSelectElement
  ) {
    return targetElement.value;
  }

  return targetElement.textContent?.trim() ?? "";
}

function showClipboardSuccess(trigger) {
  const defaultIcon = trigger.querySelector(".js-clipboard-default");
  const successIcon = trigger.querySelector(".js-clipboard-success");

  if (defaultIcon) {
    defaultIcon.classList.add("hidden");
  }
  if (successIcon) {
    successIcon.classList.remove("hidden");
  }

  window.setTimeout(() => {
    if (defaultIcon) {
      defaultIcon.classList.remove("hidden");
    }
    if (successIcon) {
      successIcon.classList.add("hidden");
    }
  }, 1800);
}

let clipboardInstance = null;

export function initClipboardButtons(selector = ".js-clipboard") {
  if (typeof window.ClipboardJS !== "function") {
    return;
  }

  if (clipboardInstance) {
    return;
  }

  try {
    clipboardInstance = new window.ClipboardJS(selector, {
      text: (trigger) => getClipboardText(trigger),
    });
  } catch {
    return;
  }

  clipboardInstance.on("success", (event) => {
    showClipboardSuccess(event.trigger);
  });
}
