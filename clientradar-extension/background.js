/*
 * ClientRadar Capture relay.
 * Uses the existing paired-device token and existing /api/companion/share contract.
 * No Facebook page collection runs in the service worker.
 */
const DEFAULT_SERVER = "https://clientradar-fxrsau.v2.appdeploy.ai";

async function addLog(entry) {
  const { activityLog = [] } = await chrome.storage.local.get("activityLog");
  const next = [{ at: new Date().toISOString(), ...entry }, ...activityLog].slice(0, 30);
  await chrome.storage.local.set({ activityLog: next, lastActivity: next[0] });
}

chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
  if (!message || message.type !== "CLIENTRADAR_SUBMIT_CAPTURE") return false;

  (async () => {
    const settings = await chrome.storage.local.get(["deviceToken", "serverUrl"]);
    const deviceToken = (settings.deviceToken || "").trim();
    const serverUrl = (settings.serverUrl || DEFAULT_SERVER).replace(/\\/$/, "");
    if (!deviceToken) throw new Error("Add your paired companion token in extension settings.");
    if (!/^https:\/\//i.test(serverUrl)) throw new Error("Server URL must use HTTPS.");

    const text = String(message.postText || "").trim().slice(0, 12000);
    const sourceUrl = String(message.sourceUrl || "");
    if (!text) throw new Error("No selected post text was provided.");
    if (!/^https:\/\/(www\\.|m\\.)?facebook\\.com\//i.test(sourceUrl)) {
      throw new Error("Capture is limited to Facebook search pages.");
    }

    const body = { deviceToken, text, sourceUrl };
    let response;
    let lastError;
    for (let attempt = 0; attempt < 2; attempt++) {
      try {
        response = await fetch(serverUrl + "/api/companion/share", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body)
        });
        if (!response.ok) throw new Error("Server returned HTTP " + response.status);
        break;
      } catch (error) {
        lastError = error;
        if (attempt === 1) throw lastError;
      }
    }

    const result = await response.json();
    await addLog({
      status: result.qualified ? "qualified" : "reviewed",
      title: result.title || (result.qualified ? "Request captured" : "Not qualified"),
      reason: result.reason || "",
      sourceUrl
    });
    return {
      ok: true,
      qualified: Boolean(result.qualified),
      title: result.title || "",
      reason: result.reason || "",
      message: result.message || "",
      result
    };
  })().then(sendResponse).catch(async (error) => {
    await addLog({ status: "error", title: "Submission failed", reason: error.message });
    sendResponse({ ok: false, error: error.message });
  });

  return true;
});

chrome.runtime.onInstalled.addListener(async () => {
  const current = await chrome.storage.local.get("serverUrl");
  if (!current.serverUrl) {
    await chrome.storage.local.set({ serverUrl: DEFAULT_SERVER });
  }
});