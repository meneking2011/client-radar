/*
 * ClientRadar authorised discovery content agent.
 * Runs only during an explicitly started, time-limited session.
 */
(() => {
  if (window.__clientRadarAutoDiscoveryInstalled) return;
  window.__clientRadarAutoDiscoveryInstalled = true;
  const seenOnPage = new Set();
  const clean = s => (s || "").replace(/\s+/g, " ").trim().slice(0, 12000);
  function permalinkFor(article) {
    for (const a of Array.from(article.querySelectorAll('a[href]'))) {
      try {
        const u = new URL(a.href, location.href);
        if (!/(^|\.)facebook\.com$/i.test(u.hostname)) continue;
        if (/\/(posts|permalink|photo|videos|reel)\//i.test(u.pathname) || /story_fbid=|permalink.php/i.test(u.search)) return u.href;
      } catch (_) {}
    }
    return "";
  }
  async function scan() {
    try {
      const s = await chrome.runtime.sendMessage({type:"CLIENTRADAR_GET_SESSION"});
      if (!s?.active || s.phase!=="running" || !navigator.onLine) return;
      for (const article of Array.from(document.querySelectorAll('[role="article"]'))) {
        const postText=clean(article.innerText||article.textContent||"");
        const postUrl=permalinkFor(article);
        if (!postUrl || postText.length<25 || seenOnPage.has(postUrl)) continue;
        seenOnPage.add(postUrl);
        chrome.runtime.sendMessage({type:"CLIENTRADAR_AUTO_POST_FOUND",postText,postUrl,sourceUrl:location.href}).catch(()=>{});
      }
    } catch (_) {}
  }
  scan();
  setInterval(scan,30000);
  new MutationObserver(scan).observe(document.documentElement,{childList:true,subtree:true});
})();