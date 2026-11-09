(() => {
  const toastRegion = document.getElementById("toast-region");
  if (toastRegion) {
    document.querySelectorAll(".alert[data-toast]").forEach((alert) => {
      const tone = ["success", "danger", "warning", "info"]
        .find((kind) => alert.classList.contains(`alert-${kind}`)) || "info";
      const toast = document.createElement("div");
      toast.className = `ft-toast ft-toast-${tone}`;
      toast.setAttribute("role", tone === "danger" ? "alert" : "status");

      const message = document.createElement("span");
      message.className = "min-w-0 flex-1";
      message.textContent = alert.textContent.trim();
      toast.append(message);

      const dismiss = document.createElement("button");
      dismiss.className = "ft-toast-dismiss";
      dismiss.type = "button";
      dismiss.setAttribute("aria-label", "Dismiss notification");
      dismiss.innerHTML = '<svg aria-hidden="true" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m18 6-12 12M6 6l12 12"></path></svg>';
      dismiss.addEventListener("click", () => toast.remove());
      toast.append(dismiss);
      toastRegion.append(toast);
      alert.remove();

      window.setTimeout(() => {
        toast.classList.add("pointer-events-none", "opacity-0", "-translate-y-2");
        window.setTimeout(() => toast.remove(), 300);
      }, 5000);
    });
  }

  const loadingOverlay = document.getElementById("loading-overlay");
  const loadingMessage = document.getElementById("loading-message");
  document.querySelectorAll("form").forEach((form) => {
    form.addEventListener("submit", (event) => {
      if (event.defaultPrevented || !form.checkValidity() || !loadingOverlay) {
        return;
      }

      if (loadingMessage) {
        loadingMessage.textContent = form.dataset.loadingMessage || "Please wait while we save your changes.";
      }
      loadingOverlay.classList.remove("hidden");
      loadingOverlay.setAttribute("aria-hidden", "false");

      const submitButton = form.querySelector('button[type="submit"]:not(:disabled)');
      if (submitButton) {
        submitButton.disabled = true;
        submitButton.setAttribute("aria-busy", "true");
        const label = submitButton.dataset.loadingText || "Working…";
        submitButton.replaceChildren();
        const spinner = document.createElement("span");
        spinner.className = "button-spinner";
        spinner.setAttribute("aria-hidden", "true");
        submitButton.append(spinner, document.createTextNode(label));
      }
    });
  });
})();
