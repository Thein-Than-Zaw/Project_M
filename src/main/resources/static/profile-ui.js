// CHAT-03: shared private avatars and foreground activity (not WebSocket connectivity).
export function avatar(node,person){
 if(!node||!person)return;const key=person.avatarUrl||person.displayName;if(node.dataset.avatarKey===key)return;node.dataset.avatarKey=key;node.textContent=Array.from(person.displayName||'?')[0];
 if(typeof person.avatarUrl==='string'&&/^\/api\/profiles\/[\da-f-]+\/photo\?v=\d+$/.test(person.avatarUrl)){
  const img=document.createElement('img');img.src=person.avatarUrl;img.alt='';img.addEventListener('error',()=>{img.remove();node.textContent=Array.from(person.displayName||'?')[0];});node.replaceChildren(img);
 }
}
export function activityText(value){
 if(value.active)return 'Active';if(!value.lastActiveAt)return 'Offline';
 const minutes=Math.max(1,Math.floor((Date.parse(value.serverTime)-Date.parse(value.lastActiveAt))/60000));
 if(minutes<60)return `Active ${minutes} min ago`;if(minutes<1440)return `Active ${Math.floor(minutes/60)} hr ago`;return `Active ${Math.floor(minutes/1440)} day ago`;
}
let stopped=false,busy=false;
async function tick(){
 if(stopped||busy||document.hidden)return;busy=true;
 try{
  const csrfResponse=await fetch('/api/csrf',{cache:'no-store',signal:AbortSignal.timeout(8000)});if(!csrfResponse.ok)return;const csrf=await csrfResponse.json();
  const beat=await fetch('/api/presence',{method:'POST',headers:{[csrf.headerName]:csrf.token},signal:AbortSignal.timeout(8000)});if(!beat.ok)return;
  const response=await fetch('/api/presence',{cache:'no-store',signal:AbortSignal.timeout(8000)});if(!response.ok)return;const state=await response.json();
  document.dispatchEvent(new CustomEvent('partner-activity',{detail:state}));
 }catch{document.dispatchEvent(new CustomEvent('partner-activity',{detail:null}));}finally{busy=false;}
}
let timer=setInterval(tick,20000);document.addEventListener('visibilitychange',tick);window.addEventListener('focus',tick);window.addEventListener('pageshow',()=>{stopped=false;clearInterval(timer);timer=setInterval(tick,20000);tick();});window.addEventListener('pagehide',()=>{stopped=true;clearInterval(timer);});tick();
