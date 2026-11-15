/*
 * ClientRadar session controller and relay.
 * The authorisation expiry is entered in extension-local settings and is not
 * embedded in this public repository. Collection requires an explicit start,
 * an online connection, and the single selected Facebook search tab.
 */
const DEFAULT_SERVER="https://clientradar-fxrsau.v2.appdeploy.ai";
function isFacebookSearchUrl(value){try{const u=new URL(String(value||"").trim());const host=u.hostname.toLowerCase().replace(/^www\./,"");const path=decodeURIComponent(u.pathname);return u.protocol==="https:"&&(host==="facebook.com"||host.endsWith(".facebook.com"))&&/^\/search(?:\/|$)/i.test(path);}catch(_){return false;}}
const MAX_ACTIVE_MS=3*60*60*1000, BREAK_MS=90*60*1000, TICK="clientradar-session-tick";
async function getState(){return (await chrome.storage.local.get("session")).session||{active:false,phase:"stopped",activeMs:0,lastTickAt:0};}
async function setState(s){await chrome.storage.local.set({session:s});}
async function expiry(){const x=await chrome.storage.local.get("authorizationEndsAt");return Number(x.authorizationEndsAt)||0;}
async function log(e){const {activityLog=[]}=await chrome.storage.local.get("activityLog");const next=[{at:new Date().toISOString(),...e},...activityLog].slice(0,60);await chrome.storage.local.set({activityLog:next,lastActivity:next[0]});}
async function stop(reason){const s=await getState();s.active=false;s.phase="stopped";s.stopReason=reason;s.stoppedAt=Date.now();await setState(s);await chrome.alarms.clear(TICK);}
async function tick(){
 const s=await getState(),now=Date.now(),end=await expiry();
 if(!s.active&&s.phase!=="break")return;
 if(!end||now>=end)return stop("The configured authorisation window has expired or is missing.");
 if(s.phase==="break"){if(now<s.breakUntil)return;s.phase="running";s.active=true;s.blockActiveMs=0;s.lastTickAt=now;s.nextRefreshAt=now;}
 if(!navigator.onLine){s.lastTickAt=now;await setState(s);return;}
 if(s.lastTickAt){const d=Math.max(0,Math.min(90000,now-s.lastTickAt));s.activeMs=(s.activeMs||0)+d;s.blockActiveMs=(s.blockActiveMs||0)+d;}
 s.lastTickAt=now;
 if(s.blockActiveMs>=MAX_ACTIVE_MS){s.active=false;s.phase="break";s.breakUntil=now+BREAK_MS;await setState(s);return;}
 if(now>=(s.nextRefreshAt||0)){
  try{const tab=await chrome.tabs.get(s.tabId);if(!tab||!isFacebookSearchUrl(tab.url||""))return stop("The selected Facebook search tab is unavailable.");await chrome.tabs.reload(s.tabId);}
  catch(_){return stop("The selected Facebook search tab is unavailable.");}
  s.nextRefreshAt=now+(s.refreshMinutes||5)*60000;
 }
 await setState(s);
}
async function relay(m){
 const s=await getState(),end=await expiry();
 if(!s.active||s.phase!=="running"||!navigator.onLine||!end||Date.now()>=end)return;
 const url=String(m.postUrl||""),text=String(m.postText||"").trim().slice(0,12000);
 if(!text||!/^https:\/\/(www\.|m\.)?facebook\.com\//i.test(url))return;
 const key="sent:"+url,exists=await chrome.storage.local.get(key);if(exists[key])return;
 const cfg=await chrome.storage.local.get(["deviceToken","serverUrl"]);if(!cfg.deviceToken)throw Error("Add the paired companion token in Settings.");
 const response=await fetch((cfg.serverUrl||DEFAULT_SERVER).replace(/\/$/,"")+"/api/companion/share",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({deviceToken:cfg.deviceToken,text,sourceUrl:url})});
 if(!response.ok)throw Error("ClientRadar API returned HTTP "+response.status);
 const result=await response.json();await chrome.storage.local.set({[key]:Date.now()});
 await log({status:result.qualified?"qualified":"reviewed",title:result.title||(result.qualified?"Qualified request":"Reviewed post"),reason:result.reason||"",sourceUrl:url});
}
chrome.runtime.onMessage.addListener((m,_sender,reply)=>{
 if(m?.type==="CLIENTRADAR_GET_SESSION"){getState().then(reply);return true;}
 if(m?.type==="CLIENTRADAR_START_SESSION"){
  (async()=>{const end=await expiry();if(!end||Date.now()>=end)throw Error("Set a valid authorisation expiry in Settings before starting.");
   if(!navigator.onLine)throw Error("Connect to the internet before starting.");
   const tab=await chrome.tabs.get(m.tabId);const candidateUrl=String(m.tabUrl||tab?.url||"").trim();if(!tab||!isFacebookSearchUrl(candidateUrl))throw Error("ClientRadar did not recognise the active tab as a Facebook search page. Detected URL: "+(candidateUrl||"(browser did not provide the tab URL)")+". Keep the authorised search page active and try again.");
   const cfg=await chrome.storage.local.get(["deviceToken","refreshMinutes"]);if(!cfg.deviceToken)throw Error("Add the paired companion token in Settings first.");
   const now=Date.now();await setState({active:true,phase:"running",startedAt:now,lastTickAt:now,activeMs:0,blockActiveMs:0,tabId:tab.id,refreshMinutes:Math.max(1,Math.min(30,Number(cfg.refreshMinutes)||5)),nextRefreshAt:now+10000,expiresAt:end});
   await chrome.alarms.create(TICK,{periodInMinutes:1});return {ok:true};
  })().then(reply).catch(e=>reply({ok:false,error:e.message}));return true;
 }
 if(m?.type==="CLIENTRADAR_STOP_SESSION"){stop("Stopped by you.").then(()=>reply({ok:true}));return true;}
 if(m?.type==="CLIENTRADAR_AUTO_POST_FOUND"){relay(m).then(()=>reply({ok:true})).catch(async e=>{await log({status:"error",title:"Post submission failed",reason:e.message});reply({ok:false,error:e.message});});return true;}
 return false;
});
chrome.alarms.onAlarm.addListener(a=>{if(a.name===TICK)tick().catch(e=>log({status:"error",title:"Session error",reason:e.message}));});
chrome.runtime.onStartup.addListener(()=>tick());
chrome.runtime.onInstalled.addListener(async()=>{const x=await chrome.storage.local.get("serverUrl");if(!x.serverUrl)await chrome.storage.local.set({serverUrl:DEFAULT_SERVER});await tick();});