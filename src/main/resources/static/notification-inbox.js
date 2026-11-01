import {browserNotifications} from '/browser-notifications.js';
// DEV-09A: durable inbox uses existing page sockets for invalidation + REST for recovery.
const template=document.createElement('template');template.innerHTML=`
<dialog id="notification-inbox" class="notification-inbox" aria-labelledby="notification-title">
 <header class="notification-heading"><div><span>YOUR LITTLE WORLD</span><h2 id="notification-title">Notifications</h2></div><button id="notification-close" type="button" aria-label="Close notifications">×</button></header>
 <div class="notification-filters" role="group" aria-label="Filter notifications"><span>Unread notifications</span><button id="notification-refresh" type="button">Refresh</button></div>
 <p id="notification-status" class="notification-status" role="status"></p><p id="notification-error" class="notification-error" role="alert" hidden></p>
 <div id="notification-list" class="notification-list"></div><button id="notification-older" class="notification-older" type="button" hidden>Load older</button>
</dialog>`;
document.body.append(template.content.cloneNode(true));
const $=id=>document.getElementById(id),entries=[],items=new Map();
const labels={MESSAGE:'Your person sent a message.',GIFT:'You received a little gift.',MEMORY:'A new memory was added.',MISSED_CALL:'You missed a call from your person.',PARTNER_JOINED:'Your partner joined your space.'};
let latestSequence=null;
let disposed=false,epoch=0,loading=false,needsReset=false,cursor=null,refreshTimer,previousFocus;
// CHAT-04: keep the sound toggle in Chat; the inbox entry belongs outside Chat.
for(const nav of notificationEntryRoots(document)){
 const link=document.createElement('a');link.href='#notification-inbox';link.className='notification-entry';
 const icon=document.createElement('span');icon.innerHTML='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4"/></svg>';icon.setAttribute('aria-hidden','true');
 const text=document.createElement('span');text.className='notification-entry-label';text.textContent='Notifications';const count=document.createElement('span');count.className='notification-badge';count.hidden=true;
 link.append(icon,text,count);link.addEventListener('click',event=>{event.preventDefault();previousFocus=link;if(!$('notification-inbox').open)$('notification-inbox').showModal();load(true);});const logout=nav.querySelector('#logout');if(logout)logout.before(link);else nav.append(link);entries.push({link,count});
}
function badge(count){for(const entry of entries){entry.count.hidden=count===0;entry.count.textContent=count>99?'99+':String(count);entry.link.setAttribute('aria-label',`Notifications, ${count} unread`);}}
function error(text){$('notification-error').hidden=!text;$('notification-error').textContent=text;}
async function request(path,method='GET',body){
 const headers={};if(method!=='GET'){const response=await fetch('/api/csrf',{cache:'no-store',signal:AbortSignal.timeout(10000)});if(response.status===401)return expired();if(!response.ok)throw new Error('Could not verify your session. Refresh and retry.');const csrf=await response.json();headers[csrf.headerName]=csrf.token;headers['Content-Type']='application/json';}
 const response=await fetch(path,{method,headers,body:body===undefined?undefined:JSON.stringify(body),cache:'no-store',signal:AbortSignal.timeout(10000)});
 if(response.status===401)return expired();if(!response.ok){const failure=await response.json().catch(()=>({}));throw new Error(failure.message||'Notifications could not be updated. Try Refresh.');}return response.json();
}
function expired(){dispose();location.replace('/?expired=1');throw new Error('Please sign in again.');}
function render(){
 const fragment=document.createDocumentFragment();
 for(const item of unreadItems(items.values())){
  const row=document.createElement('article');row.className='notification-item'+(item.readAt?'':' is-unread');
  const label=document.createElement('h3');label.textContent=labels[item.type]||'An update from your space.';
  const time=document.createElement('time');time.dateTime=item.createdAt;time.textContent=new Intl.DateTimeFormat('en-GB',{dateStyle:'medium',timeStyle:'short',timeZone:'Asia/Yangon'}).format(new Date(item.createdAt));
  const state=document.createElement('span');state.className='notification-read-state';state.textContent=item.readAt?'Read':'Unread';
  const actions=document.createElement('div');actions.className='notification-item-actions';
  const open=document.createElement('button');open.type='button';open.textContent='Open';
  const toggle=document.createElement('button');toggle.type='button';toggle.textContent=item.readAt?'Mark unread':'Mark read';
  toggle.addEventListener('click',async()=>{const version=epoch;toggle.disabled=true;error('');try{await request(`/api/notifications/${item.id}`,'PATCH',{read:!item.readAt,version:item.version});if(!disposed&&version===epoch)await load(true);}catch(failure){if(!disposed&&version===epoch){error(failure.message);refreshNotifications();}}finally{toggle.disabled=false;}});
  open.addEventListener('click',async()=>{const version=epoch;open.disabled=true;error('');try{const target=await request(`/api/notifications/${item.id}/target`);if(disposed||version!==epoch)return;if(!target.available){$('notification-status').textContent='This item is no longer available.';return;}if(!item.readAt)await request(`/api/notifications/${item.id}`,'PATCH',{read:true,version:item.version});if(disposed||version!==epoch)return;$('notification-inbox').close();if(target.url==='/home.html#calls'&&location.pathname==='/home.html')window.dispatchEvent(new Event('open-call-history'));else location.assign(target.url);}catch(failure){if(!disposed&&version===epoch){error(failure.message);refreshNotifications();}}finally{open.disabled=false;}});
  actions.append(open,toggle);row.append(label,time,state,actions);fragment.append(row);
 }
 $('notification-list').replaceChildren(fragment);$('notification-older').hidden=cursor===null;
 $('notification-status').textContent=unreadItems(items.values()).length?'Times shown in Myanmar Standard Time.':'You’re all caught up. No unread notifications.';
}
async function load(reset){
 if(disposed)return;if(loading){needsReset||=reset;return;}loading=true;const version=epoch;
 $('notification-refresh').disabled=true;$('notification-older').disabled=true;
 if(!items.size)$('notification-status').textContent='Loading notifications…';
 try{const query=new URLSearchParams({unreadOnly:'true'});if(!reset&&cursor!==null)query.set('before',cursor);const page=await request('/api/notifications?'+query);
  if(disposed||version!==epoch)return;if(reset){const latest=page.items.filter(item=>latestSequence!==null&&item.sequence>latestSequence&&!item.readAt);if(latest.length)browserNotifications.show({id:'inbox:'+latest[0].id,title:'us, always',body:labels[latest[0].type]||'A new update is waiting in your private space.',onClick:()=>{const callPanel=document.getElementById('call-panel');if(callPanel&&!callPanel.hidden)return;if(!$('notification-inbox').open)$('notification-inbox').showModal();load(true);}});latestSequence=Math.max(latestSequence??0,...page.items.map(item=>item.sequence));items.clear();}for(const item of page.items)items.set(item.id,item);cursor=page.nextCursor;badge(page.unreadCount);error('');render();
 }catch(failure){if(!disposed&&version===epoch){error(failure.message);$('notification-status').textContent='Could not refresh. Your saved notifications are unchanged.';}}
 finally{loading=false;if(!disposed){$('notification-refresh').disabled=false;$('notification-older').disabled=false;if(needsReset){needsReset=false;load(true);}}}
}
export function refreshNotifications(){if(disposed)return;clearTimeout(refreshTimer);refreshTimer=setTimeout(()=>load(true),250);}
$('notification-refresh').addEventListener('click',()=>load(true));$('notification-older').addEventListener('click',()=>load(false));$('notification-close').addEventListener('click',()=>$('notification-inbox').close());
$('notification-inbox').addEventListener('close',()=>{if(!disposed)previousFocus?.focus({preventScroll:true});});
const poll=setInterval(()=>{if(!document.hidden)refreshNotifications();},15000);
document.addEventListener('visibilitychange',()=>{if(!document.hidden)refreshNotifications();});
function dispose(){disposed=true;epoch++;clearTimeout(refreshTimer);clearInterval(poll);$('notification-inbox').close();items.clear();$('notification-list').replaceChildren();error('');$('notification-status').textContent='';badge(0);}
window.addEventListener('pagehide',dispose);refreshNotifications();

// Explicit helpers allow regression checks against actual mount/filter behavior.
function notificationEntryRoots(page){return page.querySelector('.chat-world')?[]:page.querySelectorAll('.world-header,.memory-heading,.mobile-nav');}
function unreadItems(values){return [...values].filter(item=>!item.readAt);}
