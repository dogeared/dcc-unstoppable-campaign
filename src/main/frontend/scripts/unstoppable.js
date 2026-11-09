/*
 * Browser-side helpers for the Unstoppable campaign sheet.
 * Java views call these through window.Unstoppable so no inline script
 * strings live in the UI code.
 */
window.Unstoppable = window.Unstoppable || {};

(function () {
  "use strict";

  function legacyCopy(text) {
    var area = document.createElement("textarea");
    area.value = text;
    area.setAttribute("readonly", "");
    area.className = "u-clipboard-proxy";
    document.body.appendChild(area);
    area.select();
    var copied = false;
    try {
      copied = document.execCommand("copy");
    } catch (error) {
      copied = false;
    }
    document.body.removeChild(area);
    return copied;
  }

  window.Unstoppable.copyText = function (text) {
    if (navigator.clipboard && window.isSecureContext) {
      return navigator.clipboard.writeText(text).catch(function () {
        legacyCopy(text);
      });
    }
    legacyCopy(text);
    return Promise.resolve();
  };
})();
