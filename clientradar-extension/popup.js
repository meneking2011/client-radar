const el=document.getElementById("status");
async function show(){
 const s=await chrome.runtime.sendMessage({type:"CLIENTRADAR_GET_SESSION"});
 el.textContent=s.phase==="running"&&s.active?"Running. "+Math.max(0,Math.ceil((10800000-(s.blockActiveMs||0))/60000))+" active minutes left in this block.":s.phase==="break"?"Break until "+new Date(s.breakUntil).toLocaleTimeString():("Stopped"+(s.stopReason?": "+s.stopReason:"."));
}
function isFacebookSearchUrl(value){
 try{
  const u=new URL(String(value||"").trim());
  const host=u.hostname.toLowerCase().replace("www.","");
  const path=decodeURIComponent(u.pathname).toLowerCase();
  const validHost=host==="facebook.com"||host.endsWith(".facebook.com");
  const validPath=path==="/search"||path.startsWith("/search/");
  return u.protocol==="https:"&&validHost&&validPath;
 }catch(_){return false;}
}
document.getElementById("start").onclick=async()=>{
 el.textContent="Finding your authorised Facebook search tab…";
 try{
  // Kiwi may open the extension UI as a separate tab. Search all tabs
  // rather than assuming the currently active tab is Facebook.
  const tabs=await chrome.tabs.query({});
  const matches=tabs.filter(t=>t.id!==undefined&&isFacebookSearchUrl(t.url||""));
  if(!matches.length){
   el.textContent="No Facebook search tab found. Open https://m.facebook.com/search/ in another tab, then return here.";
   return;
  }
  const tab=matches.find(t=>t.active)||matches[matches.length-1];
  const r=await chrome.runtime.sendMessage({type:"CLIENTRADAR_START_SESSION",tabId:tab.id,tabUrl:tab.url});
  el.textContent=r?.ok?"Started on "+tab.url:r?.error||"Could not start.";
  if(r?.ok)show();
 }catch(e){el.textContent="Could not inspect browser tabs: "+(e?.message||String(e));}
};
document.getElementById("stop").onclick=async()=>{await chrome.runtime.sendMessage({type:"CLIENTRADAR_STOP_SESSION"});show();};
show();