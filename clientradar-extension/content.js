/*
 * ClientRadar Capture — user-initiated page selection only.
 * No MutationObserver, polling, automatic refresh, navigation, or Facebook interaction.
 */
(() => {
  if (window.__clientRadarCaptureInstalled) return;
  window.__clientRadarCaptureInstalled = true;

  const cleanText = (value) => (value || "").replace(/\s+/g, " ").trim().slice(0, 12000);

  function captureSelection() {
    const selection = window.getSelection();
    const postText = cleanText(selection ? selection.toString() : "");
    if (!postText) {
      return { ok: false, error: "Select the post text first, then open ClientRadar Capture." };
    }

    // Use a permalink only if the selected text is inside a recognizable post container.
    let node = selection && selection.anchorNode;
    let element = node && (node.nodeType === Node.ELEMENT_NODE ? node : node.parentElement);
    const post = element && element.closest('[role="article"]');
    let postUrl = "";
    if (post) {
      const links = Array.from(post.querySelectorAll('a[href]'));
      const candidate = links.find((a) => /\/(posts|permalink|photo|videos)\//i.test(a.getAttribute("href") || ""));
      if (candidate) {
        try {
          const url = new URL(candidate.href, location.href);
          if (url.hostname === "www.facebook.com" || url.hostname === "m.facebook.com") {
            postUrl = url.href;
          }
        } catch (_) { /* Keep the search URL as source fallback. */ }
      }
    }

    return {
      ok: true,
      postText,
      postUrl: postUrl || location.href,
      sourceUrl: location.href,
      capturedAt: new Date().toISOString(),
      keyword: new URL(location.href).searchParams.get("q") || ""
    };
  }

  chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
    if (message && message.type === "CLIENTRADAR_CAPTURE_SELECTED") {
      sendResponse(captureSelection());
    }
    return true;
  });
})();