import {notificationStorage} from './notification-sounds.js';

// DEV-09B: opt-in browser/system alerts while an authenticated app page is open.
// No Push API, service worker, message previews or background server subscription.
export function createBrowserNotifications({storage,api,secure,isBackground,focus,onChange=()=>{},lock=async task=>task()}){
 let opted=storage.getItem('couple.browserNotifications.enabled')==='true',revision=0,lastError='',disposed=false;const active=new Map(),dismissed=new Set();
 const supported=()=>!!(secure&&api&&typeof api.requestPermission==='function');
 const enabled=()=>!disposed&&supported()&&opted&&storage.getItem('couple.browserNotifications.enabled')==='true'&&api.permission==='granted';
 function dismiss(id){dismissed.add(id);active.get(id)?.close();active.delete(id);}
 function disable(persist=true){revision++;opted=false;for(const id of active.keys())dismiss(id);if(persist)storage.setItem('couple.browserNotifications.enabled','false');onChange();}
 return {
  get supported(){return supported();},get enabled(){return enabled();},get permission(){return api?.permission||'unsupported';},get error(){return lastError;},
  async enable(){const version=++revision;if(!supported())throw new Error('Browser notifications are unavailable here. Try a supported desktop browser on HTTPS or localhost.');
   const permission=api.permission==='default'?await api.requestPermission():api.permission;
   if(version!==revision)return false;
   if(permission!=='granted'){disable();throw new Error(permission==='denied'?'Notifications are blocked. Allow them in your browser site settings, then try again.':'Permission was not granted. In-app alerts remain available.');}
   opted=true;lastError='';storage.setItem('couple.browserNotifications.enabled','true');onChange();return true;
  },
  disable,sync(){opted=storage.getItem('couple.browserNotifications.enabled')==='true';if(!enabled())for(const id of active.keys())dismiss(id);onChange();},dismiss,
  async show({id,title,body,onClick,test=false}){
   if(!id||dismissed.has(id)||!enabled()||(!test&&!isBackground()))return false;
   try{return await lock(()=>{
    if(dismissed.has(id)||!enabled()||(!test&&!isBackground()))return false;
    let seen=[];try{const saved=JSON.parse(storage.getItem('couple.browserNotifications.recent')||'[]');if(Array.isArray(saved))seen=saved;}catch{}
    if(seen.includes(id))return false;
    try{const notice=new api(title,{body,tag:'couple-'+id,silent:true});active.set(id,notice);
     notice.onclick=()=>{notice.close();focus();onClick?.();};notice.onclose=()=>active.delete(id);
     storage.setItem('couple.browserNotifications.recent',JSON.stringify([...seen.slice(-99),id]));lastError='';return true;
    }catch{lastError='This browser could not display the notification. In-app alerts are still available.';onChange();return false;}
   });}catch{lastError='Browser alerts are temporarily unavailable. In-app alerts remain available.';onChange();return false;}
  },
  dispose(){disposed=true;revision++;for(const id of active.keys())dismiss(id);}
 };
}

const inBrowser=typeof window!=='undefined',tabId=inBrowser?(globalThis.crypto?.randomUUID?.()||String(Math.random())):'';
const focusKey='couple.browserNotifications.focus';
function foreground(){try{const state=JSON.parse(notificationStorage.getItem(focusKey)||'null');return state&&state.until>Date.now();}catch{return false;}}
function reportFocus(){if(!inBrowser)return;let current;try{current=JSON.parse(notificationStorage.getItem(focusKey)||'null');}catch{}
 if(!document.hidden&&document.hasFocus())notificationStorage.setItem(focusKey,JSON.stringify({tab:tabId,until:Date.now()+5000}));
 else if(current?.tab===tabId)notificationStorage.setItem(focusKey,'null');
}
export const browserNotifications=inBrowser?createBrowserNotifications({storage:notificationStorage,api:window.Notification,secure:window.isSecureContext,isBackground:()=>{reportFocus();return (document.hidden||!document.hasFocus())&&!foreground();},focus:()=>window.focus(),onChange:()=>window.dispatchEvent(new Event('browser-notifications-change')),lock:task=>navigator.locks?navigator.locks.request('couple-browser-notifications',task):Promise.resolve(task())}):null;
if(inBrowser){
 const timer=setInterval(reportFocus,2000);reportFocus();
 window.addEventListener('focus',()=>{reportFocus();browserNotifications.sync();});window.addEventListener('blur',reportFocus);document.addEventListener('visibilitychange',reportFocus);
 window.addEventListener('storage',event=>{if(event.key==='couple.browserNotifications.enabled'||event.key===null)browserNotifications.sync();});
 window.addEventListener('pagehide',()=>{clearInterval(timer);browserNotifications.dispose();let current;try{current=JSON.parse(notificationStorage.getItem(focusKey)||'null');}catch{}if(current?.tab===tabId)notificationStorage.setItem(focusKey,'null');});
}
