/**
 * OpenDebt — Accessibility helpers (a11y.js)
 *
 * Minimal focus-management script for HTMX-driven content updates.
 * After HTMX swaps new content into the DOM, focus is moved to the
 * swapped container so that screen readers announce the updated content.
 *
 * No framework dependencies. Loaded with "defer" in the layout template.
 */
(function () {
  "use strict";

  document.addEventListener("htmx:afterSwap", function (event) {
    var target = event.detail.target;
    if (!target) {
      return;
    }
    // Make the target programmatically focusable if it isn't already
    if (!target.hasAttribute("tabindex")) {
      target.setAttribute("tabindex", "-1");
    }
    target.focus({ preventScroll: false });
  });
})();
