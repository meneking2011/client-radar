const statusEl = document.getElementById("status");
document.getElementById("capture").addEventListener("click", async () => {
  statusEl.textContent = "Reading your selection…";
  try {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    if (!tab || !tab.id) throw new Error("No active tab.");
    const captured = await chrome.tabs.sendMessage(tab.id, { type: "CLIENTRADAR_CAPTURE_SELECTED" });
    if (!captured || !captured.ok) throw new Error(captured?.error || "Could not capture the selection.");
    statusEl.textContent = "Sending to ClientRadar…";
    const result = await chrome.runtime.sendMessage({
      type: "CLIENTRADAR_SUBMIT_CAPTURE",
      postText: captured.postText,
      sourceUrl: captured.postUrl || captured.sourceUrl
    });
    if (!result || !result.ok) throw new Error(result?.error || "Submission failed.");
    statusEl.textContent = result.qualified
      ? "Qualified request saved.\\n" + (result.title || "") + (result.message ? "\\n\\nDraft: " + result.message : "")
      : "Reviewed, not qualified.\\n" + (result.reason || "No qualifying request found.");
  } catch (error) {
    statusEl.textContent = error.message + "\\n\\nCheck that you are on a Facebook search page and have selected post text.";
  }
});