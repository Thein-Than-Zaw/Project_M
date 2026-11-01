import {chatSocket} from '/chat-socket.js';
const $=s=>document.querySelector(s), $$=s=>document.querySelectorAll(s);
const records=new Map(),pending=new Map();
let started=false,epoch=0,selfId,partner,timezone='Asia/Yangon',storageKey,live,cursor=0,hasOlder=false,loading=false,resync=false,olderBusy=false,initial=true,renderIdentity='',poll,ackTimer,ackDebounce,syncDebounce,lastAck=0;
const log=$('#messages'),empty=$('#empty');
function showError(text){$('#error').textContent=text;$('#error').hidden=false;$('#retry-load').hidden=false;}
function clearError(){$('#error').hidden=true;$('#retry-load').hidden=true;}
function expire(login=true){clearOutbox();live?.stop();location.replace(login?'/?expired=1':'/space.html');}
async function get(path){
  const response=await fetch(path,{cache:'no-store',signal:AbortSignal.timeout(10000)});
  if(response.status===401){expire();throw new Error('Please sign in again.');}
  if(response.status===403&&!path.startsWith('/api/chat/receipts')){expire(false);throw new Error('Connect with your partner first.');}
  if(!response.ok)throw new Error('We could not load your saved messages. Check your connection and try again.');return response.json();
}
function persist(){
  if(!storageKey)return;
  try{sessionStorage.setItem(storageKey,JSON.stringify([...pending.values()]));}
  catch{$('#storage-note').textContent='Unsent messages are kept only in memory here. Keep this tab open until they are sent.';$('#storage-note').hidden=false;}
}
function clearOutbox(){try{if(storageKey)sessionStorage.removeItem(storageKey);}catch{}pending.clear();}
function restore(){
  try{
    const values=JSON.parse(sessionStorage.getItem(storageKey)||'[]');
    if(!Array.isArray(values))return;
    for(const p of values.slice(0,50))if(typeof p.clientMessageId==='string'&&/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(p.clientMessageId)&&typeof p.body==='string'&&p.body.trim()!==''&&Array.from(p.body).length<=4000){pending.set(p.clientMessageId,{...p,state:'Queued',retryable:true});}
  }catch{$('#storage-note').textContent='Local pending messages could not be restored. Keep this tab open for new unsent messages.';$('#storage-note').hidden=false;}
}
function metadata(page){
  selfId=page.selfId;partner=page.partner;timezone=page.timezone;
  for(const node of $$('[data-partner]'))node.textContent=partner.displayName;
  $('#partner-avatar').textContent=Array.from(partner.displayName)[0];$('#about-avatar').textContent=Array.from(partner.displayName)[0];
  $('#couple-names').textContent=`${page.selfName} & ${partner.displayName}`;
  $('#couple-initials').replaceChildren(...[page.selfName,partner.displayName].map(name=>{const span=document.createElement('span');span.textContent=Array.from(name)[0];return span;}));
  for(const badge of $$('[data-unread]')){badge.textContent=page.unreadCount>99?'99+':String(page.unreadCount);badge.hidden=!page.unreadCount;}
}
function merge(message){
  if(!message?.id)return false;
  const previous=records.get(message.id),next={...message,deliveredAt:previous?.deliveredAt??message.deliveredAt,readAt:previous?.readAt??message.readAt};
  const changed=JSON.stringify(previous)!==JSON.stringify(next);records.set(message.id,next);
  if(message.senderId===selfId&&pending.get(message.clientMessageId)?.body===message.body){pending.delete(message.clientMessageId);persist();}
  return changed;
}
function receipt(value){const old=records.get(value.id);if(!old)return false;return merge({...old,deliveredAt:old.deliveredAt??value.deliveredAt,readAt:old.readAt??value.readAt});}
function time(instant){return new Intl.DateTimeFormat('en-GB',{timeZone:timezone,hour:'2-digit',minute:'2-digit'}).format(new Date(instant));}
function date(instant){return new Intl.DateTimeFormat('en-GB',{timeZone:timezone,day:'numeric',month:'short',year:'numeric'}).format(new Date(instant));}
function nearBottom(){return log.scrollHeight-log.scrollTop-log.clientHeight<80;}
function render(mode='normal'){
  const messages=[...records.values()].sort((a,b)=>a.sequence-b.sequence),outbox=[...pending.values()];
  const identity=JSON.stringify([messages,outbox.map(({lastAttempt,...p})=>p)]);
  if(identity===renderIdentity){queueAcks();return;}renderIdentity=identity;
  const bottom=nearBottom(),height=log.scrollHeight,top=log.scrollTop,fragment=document.createDocumentFragment();let lastDay='';
  for(const item of messages){
    const day=date(item.createdAt);
    if(day!==lastDay){const divider=document.createElement('div');divider.className='day-divider';const span=document.createElement('span');span.textContent=day;divider.append(span);fragment.append(divider);lastDay=day;}
    const row=document.createElement('article');row.className=`message${item.senderId===selfId?' mine':''}`;row.dataset.id=item.id;
    const bubble=document.createElement('p');bubble.className='bubble';bubble.textContent=item.body;
    const meta=document.createElement('div');meta.className='message-meta';const stamp=document.createElement('time');stamp.dateTime=item.createdAt;stamp.textContent=time(item.createdAt);meta.append(stamp);
    if(item.senderId===selfId){const status=document.createElement('span');status.textContent=item.readAt?'Read':item.deliveredAt?'Delivered':'Sent';meta.append(status);}
    row.append(bubble,meta);fragment.append(row);
  }
  for(const item of outbox){
    const row=document.createElement('article');row.className=`message mine${item.state==='Not confirmed'?' failed':''}`;
    const bubble=document.createElement('p');bubble.className='bubble';bubble.textContent=item.body;
    const meta=document.createElement('div');meta.className='message-meta';const status=document.createElement('span');status.textContent=item.error||item.state;meta.append(status);
    if(item.state==='Not confirmed'){const retry=document.createElement('button');retry.type='button';retry.className='retry-message';retry.textContent='Retry';retry.setAttribute('aria-label',`Retry message ${item.body.slice(0,40)}`);retry.addEventListener('click',()=>transmit(item,true));meta.append(retry);}
    row.append(bubble,meta);fragment.append(row);
  }
  if(!messages.length&&!outbox.length){empty.hidden=false;fragment.append(empty);}
  log.replaceChildren(fragment);
  if(mode==='older')log.scrollTop=top+log.scrollHeight-height;
  else if(initial||mode==='bottom'||bottom){log.scrollTop=log.scrollHeight;$('#new-messages').hidden=true;}
  else{log.scrollTop=top;$('#new-messages').hidden=false;}
  initial=false;$('#older').hidden=!hasOlder;queueAcks();
}
function visibleIncoming(){
  if(document.visibilityState!=='visible'||!document.hasFocus())return [];
  const bounds=log.getBoundingClientRect(),top=Math.max(0,bounds.top),bottom=Math.min(innerHeight,bounds.bottom);
  if(bottom<=top)return [];
  return [...log.querySelectorAll('.message[data-id]')].filter(node=>{const message=records.get(node.dataset.id),r=node.getBoundingClientRect();return message?.senderId!==selfId&&!message?.readAt&&r.bottom>top&&r.top<bottom;}).map(node=>node.dataset.id);
}
function flushAcks(){
  if(!started||!live?.connected()||Date.now()-lastAck<450)return;lastAck=Date.now();
  const delivered=[...records.values()].filter(m=>m.senderId!==selfId&&!m.deliveredAt).slice(0,50).map(m=>m.id),read=visibleIncoming().slice(0,50);
  if(delivered.length)live.send('chat.delivered',{messageIds:delivered});
  if(read.length)live.send('chat.read',{messageIds:read});
}
function queueAcks(){clearTimeout(ackDebounce);ackDebounce=setTimeout(flushAcks,250);}
function requestSync(){clearTimeout(syncDebounce);syncDebounce=setTimeout(()=>sync(),180);}
async function refreshReceipts(){
  const ids=[...records.values()].filter(m=>m.senderId===selfId&&!m.readAt).map(m=>m.id);
  let changed=false;
  for(let i=0;i<ids.length&&started;i+=50){const rows=await get(`/api/chat/receipts?ids=${ids.slice(i,i+50).join(',')}`);if(!started)return;for(const r of rows)changed=receipt(r)||changed;}
  if(changed)render();
}
async function sync(){
  if(!started||!selfId)return;if(loading){resync=true;return;}loading=true;const generation=epoch;
  try{
    let more=false;
    do{const page=await get(`/api/chat?after=${cursor}`);if(generation!==epoch)return;metadata(page);for(const m of page.items)merge(m);if(page.items.length)cursor=Math.max(cursor,page.items.at(-1).sequence);more=page.hasMore;if(!more)cursor=Math.max(cursor,page.latestSequence);render();}while(more&&started);
    await refreshReceipts();if(generation!==epoch)return;clearError();queueAcks();
  }catch(error){if(generation===epoch)showError(error.message);}
  finally{loading=false;if(resync&&started){resync=false;requestSync();}}
}
function transmit(item,manual=false){
  if(!started)return;
  if(!live?.connected()){item.state='Queued';item.error='Queued · waiting for connection';persist();render();return;}
  if(item.retryable===false&&!manual)return;
  item.state='Sending…';item.error='';item.lastAttempt=Date.now();
  if(!live.send('chat.send',{clientMessageId:item.clientMessageId,body:item.body})){item.state='Queued';item.error='Queued · waiting for connection';}
  persist();render();
}
function flushPending(){for(const p of pending.values())transmit(p);}
function onEvent(event){
  if(!started)return;
  let changed=false;
  if(event.message)changed=merge(event.message);
  for(const value of event.receipts||[])changed=receipt(value)||changed;
  if(event.type==='chat.error'){
    const item=pending.get(event.clientMessageId);
    if(item){
      const friendly={MESSAGE_NOT_SAVED:'We could not confirm this message was saved. Your text is still here. Retry.',MESSAGE_CONFLICT:'This message changed in another attempt. Reload your conversation before trying again.',INVALID_MESSAGE_ID:'Reload your conversation before trying again.'};
      item.state='Not confirmed';item.error=friendly[event.code]||event.detail||'Message not confirmed. Retry.';item.retryable=!['MESSAGE_CONFLICT','EMPTY_MESSAGE','MESSAGE_TOO_LONG','INVALID_MESSAGE_ID'].includes(event.code);persist();changed=true;
    }
    else if(event.code!=='ACK_NOT_SAVED')$('#connection').textContent=event.detail||'Receipt update will retry.';
  }
  if(changed)render();
  if(event.type!=='chat.error')requestSync();
}
async function start(){
  if(started)return;started=true;const generation=++epoch;clearError();$('#connection').textContent='Loading your conversation…';
  try{
    const page=await get('/api/chat');if(generation!==epoch)return;metadata(page);storageKey=`couple.chat.outbox.${selfId}`;restore();cursor=page.latestSequence;hasOlder=page.hasMore;for(const item of page.items)merge(item);$('#chat-content').hidden=false;$('#message-body').disabled=false;$('#send').disabled=false;initial=true;render();
    live=chatSocket({onEvent,onStatus:text=>{if(started)$('#connection').textContent=text;},onExpired:expire,onReady:()=>{sync().then(()=>{if(started)flushPending();});}});
    poll=setInterval(()=>{if(!document.hidden)sync();for(const item of pending.values())if(item.state==='Sending…'&&Date.now()-item.lastAttempt>15000){item.state='Not confirmed';item.error='Save not confirmed. Retry with the same message.';persist();render();}},15000);
    ackTimer=setInterval(flushAcks,3000);
  }catch(error){if(generation===epoch){showError(error.message);started=false;}}
}
$('#composer').addEventListener('submit',event=>{
  event.preventDefault();$('#compose-error').textContent='';$('#message-body').removeAttribute('aria-invalid');
  const body=$('#message-body').value;
  if(!body.trim()||Array.from(body).length>4000){$('#compose-error').textContent=body.trim()?'Use up to 4,000 characters.':'Write a message first.';$('#message-body').setAttribute('aria-invalid','true');$('#message-body').focus();return;}
  if(pending.size>=50){$('#compose-error').textContent='Your tab has 50 unsent messages. Reconnect and send them before adding more.';return;}
  const item={clientMessageId:crypto.randomUUID(),body,state:'Queued',retryable:true};pending.set(item.clientMessageId,item);persist();$('#message-body').value='';updateCount();transmit(item);render('bottom');$('#message-body').focus();
});
function updateCount(){$('#character-count').textContent=`${Array.from($('#message-body').value).length.toLocaleString()} / 4,000`;}
$('#message-body').addEventListener('input',updateCount);
$('#message-body').addEventListener('keydown',event=>{if(event.key==='Enter'&&!event.shiftKey&&!event.isComposing){event.preventDefault();$('#composer').requestSubmit();}});
$('#emoji-toggle').addEventListener('click',()=>{const show=$('#emoji-picker').hidden;$('#emoji-picker').hidden=!show;$('#emoji-toggle').setAttribute('aria-expanded',String(show));});
for(const button of $$('#emoji-picker button'))button.addEventListener('click',()=>{const input=$('#message-body');input.setRangeText(button.textContent,input.selectionStart,input.selectionEnd,'end');input.focus();updateCount();$('#emoji-picker').hidden=true;$('#emoji-toggle').setAttribute('aria-expanded','false');});
$('#older').addEventListener('click',async()=>{
  if(olderBusy||!records.size)return;olderBusy=true;$('#older').disabled=true;$('#older-status').textContent='Loading earlier messages…';
  try{const min=Math.min(...[...records.values()].map(m=>m.sequence)),page=await get(`/api/chat?before=${min}`);if(!started)return;hasOlder=page.hasMore;for(const m of page.items)merge(m);render('older');$('#older-status').textContent=hasOlder?'':'You are at the beginning of your story.';}
  catch(error){showError(error.message);$('#older-status').textContent='Earlier messages could not be loaded. Try again.';}
  finally{olderBusy=false;$('#older').disabled=false;}
});
$('#new-messages').addEventListener('click',()=>{log.scrollTop=log.scrollHeight;$('#new-messages').hidden=true;queueAcks();});
log.addEventListener('scroll',()=>{if(nearBottom())$('#new-messages').hidden=true;queueAcks();},{passive:true});
$('#retry-load').addEventListener('click',()=>started?sync():start());
window.addEventListener('focus',()=>{if(started){sync();queueAcks();}});window.addEventListener('resize',queueAcks);
document.addEventListener('visibilitychange',()=>{if(!document.hidden&&started){sync();queueAcks();}});
$('#logout').addEventListener('click',async()=>{
  $('#logout').disabled=true;
  try{const c=await(await fetch('/api/csrf',{cache:'no-store',signal:AbortSignal.timeout(10000)})).json(),r=await fetch('/api/auth/logout',{method:'POST',headers:{[c.headerName]:c.token},signal:AbortSignal.timeout(10000)});if(!r.ok&&r.status!==401)throw new Error('logout');clearOutbox();live?.stop();location.replace('/?signedout=1');}
  catch{showError('Sign out could not be confirmed. Check your connection and try again.');$('#logout').disabled=false;}
});
window.addEventListener('pageshow',start);
window.addEventListener('pagehide',()=>{started=false;epoch++;live?.stop();clearInterval(poll);clearInterval(ackTimer);clearTimeout(ackDebounce);clearTimeout(syncDebounce);$('#chat-content').hidden=true;records.clear();pending.clear();selfId=null;renderIdentity='';});
start();
