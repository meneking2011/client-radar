const serverInput = document.getElementById("serverUrl");
const tokenInput = document.getElementById("deviceToken");
const status = document.getElementById("status");
(async () => {
  const saved = await chrome.storage.local.get(["serverUrl", "deviceToken"]);
  serverInput.value = saved.serverUrl || "https://clientradar-fxrsau.v2.appdeploy.ai";
  tokenInput.value = saved.deviceToken || "";
})();
document.getElementById("save").addEventListener("click", async () => {
  const serverUrl = serverInput.value.trim().replace(/\/$/, "");
  const deviceToken = tokenInput.value.trim();
  if (!/^https:\/\//i.test(serverUrl)) { status.textContent = "Enter a valid HTTPS server URL."; return; }
  if (!deviceToken) { status.textContent = "Enter the paired companion token."; return; }
  await chrome.storage.local.set({ serverUrl, deviceToken });
  status.textContent = "Settings saved.";
});