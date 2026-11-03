import {mountChatMedia} from '/chat-media.js';
import {avatar,activityText} from '/profile-ui.js';
import {refreshNotifications} from '/notification-inbox.js';
import {notificationSounds} from '/notification-sounds.js';
import {chatSocket} from '/chat-socket.js';
const $=s=>document.querySelector(s), $$=s=>document.querySelectorAll(s);
const records=new Map(),pending=new Map(),attempted=new Set(),hiddenMessages=new Set(),cancelledSends=new Set();
let chatMedia=null,cancelRetryBusy=false;
const messageSignatures=new WeakMap();
const SEND_WAIT_MS=300000;
let sendTimer;
let replyTargetId=null,actionMessageId=null,actionMode=null,actionBusy=false,changeVersion=-1;
let partnerActivity=null;
let editTarget=null,editVersion=null,editBusy=false;
let reactionMessageId=null,reactionAnchor=null,reactionHideTimer,contextPoint=null,reactionPersistent=false,reactionSignature=null;
const reactionChoices=['❤️','😂','😮','😢','👍','🥰'];
let selectedGift=null,giftOffset=0,giftBefore=null,giftDirection='browse',giftGeneration=0,giftBusy=false,giftHasMore=false,giftSearchTimer;
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
  try{sessionStorage.setItem(storageKey,JSON.stringify([...pending.values()].map(({localPreview,...item})=>item)));sessionStorage.setItem(storageKey+".cancelled",JSON.stringify([...cancelledSends]));}
  catch{$('#storage-note').textContent='Unsent messages are kept only in memory here. Keep this tab open until they are sent.';$('#storage-note').hidden=false;}
}
function clearOutbox(){try{if(storageKey){sessionStorage.removeItem(storageKey);sessionStorage.removeItem(storageKey+".cancelled");}}catch{}pending.clear();attempted.clear();cancelledSends.clear();}
function restore(){
  try{
    for(const id of JSON.parse(sessionStorage.getItem(storageKey+'.cancelled')||'[]'))if(typeof id==='string'&&/^[a-f0-9-]{36}$/i.test(id))cancelledSends.add(id);
    const values=JSON.parse(sessionStorage.getItem(storageKey)||'[]');
    if(!Array.isArray(values))return;
    for(const p of values.slice(0,50)){
      const gift=p.type==='GIFT',file=p.type==='FILE';
      if(typeof p.clientMessageId==='string'&&/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(p.clientMessageId)&&(!p.replyToId||/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(p.replyToId))&&typeof p.body==='string'&&Array.from(p.body).length<=(gift?500:4000)&&(file?(!p.attachmentId||safeMedia(p.attachment?.url)&&typeof p.attachmentId==='string'):gift?typeof p.giftCode==='string'&&typeof p.giftName==='string'&&safeAsset(p.giftAssetPath):p.body.trim()!==''))!cancelledSends.has(p.clientMessageId)&&pending.set(p.clientMessageId,{...p,uploading:false,state:Date.now()>=(p.deadlineAt||0)?'Failed':'Sending…',retryable:true});
    }
  }catch{$('#storage-note').textContent='Local pending messages could not be restored. Keep this tab open for new unsent messages.';$('#storage-note').hidden=false;}
}
function metadata(page){
  selfId=page.selfId;partner=page.partner;timezone=page.timezone;
  const stats=page.activity;$('#chat-activity-messages').textContent=Number.isSafeInteger(stats?.messageCount)?stats.messageCount.toLocaleString():'—';$('#chat-activity-calls').textContent=Number.isSafeInteger(stats?.videoCallCount)?stats.videoCallCount.toLocaleString():'—';$('#chat-activity-time').textContent=videoDuration(stats?.videoCallSeconds);

  for(const node of $$('[data-partner]'))node.textContent=partner.displayName;
  avatar($('#partner-avatar'),partner);avatar($('#about-avatar'),partner);
  $('#couple-names').textContent=`${page.selfName} & ${partner.displayName}`;
  $('#couple-initials').replaceChildren(...[{displayName:page.selfName,avatarUrl:page.selfAvatarUrl},partner].map(person=>{const span=document.createElement('span');avatar(span,person);return span;}));
  for(const badge of $$('[data-unread]')){badge.textContent=page.unreadCount>99?'99+':String(page.unreadCount);badge.hidden=!page.unreadCount;}
}
function merge(message){
  if(!message?.id)return false;
  if(message.hidden||hiddenMessages.has(message.id)){hiddenMessages.add(message.id);records.delete(message.id);if(message.senderId===selfId){pending.delete(message.clientMessageId);attempted.delete(message.clientMessageId);chatMedia?.confirmed(message.clientMessageId);persist();}redactReferences(message.id);return true;}
  const previous=records.get(message.id);if((previous?.version??0)>(message.version??0))return false;
  const next={...message,deliveredAt:previous?.deliveredAt??message.deliveredAt,readAt:previous?.readAt??message.readAt};
  const changed=JSON.stringify(previous)!==JSON.stringify(next);records.set(message.id,next);
  const queued=pending.get(message.clientMessageId);
  if(message.senderId===selfId&&message.deletedAt&&queued){pending.delete(message.clientMessageId);attempted.delete(message.clientMessageId);chatMedia?.confirmed(message.clientMessageId);persist();}
  if(message.senderId===selfId&&(queued?.body===message.body||queued&&message.editedAt)&&(queued.type||'TEXT')===message.type&&(queued.giftCode||null)===message.giftCode&&(queued.replyToId||null)===(message.replyToId||null)&&(queued.attachmentId||null)===(message.attachmentId||null)){if(attempted.delete(message.clientMessageId))notificationSounds.sent(message.id);pending.delete(message.clientMessageId);chatMedia?.confirmed(message.clientMessageId);persist();if(message.type==='GIFT')$('#gift-notice').textContent='Your sticker was sent.';}
  if(message.deletedAt)redactReferences(message.id);
  return changed;
}
function receipt(value){const old=records.get(value.id);if(!old)return false;return merge({...old,deliveredAt:old.deliveredAt??value.deliveredAt,readAt:old.readAt??value.readAt});}
function time(instant){return new Intl.DateTimeFormat('en-GB',{timeZone:timezone,hour:'2-digit',minute:'2-digit'}).format(new Date(instant));}
function date(instant){return new Intl.DateTimeFormat('en-GB',{timeZone:timezone,day:'numeric',month:'short',year:'numeric'}).format(new Date(instant));}
function nearBottom(){return log.scrollHeight-log.scrollTop-log.clientHeight<80;}
function safeAsset(path){return typeof path==='string'&&/^\/stickers\/[a-z0-9_-]+\/[a-zA-Z0-9_-]+\.png$/.test(path);}
function messageBubble(item){
  const bubble=document.createElement('div');bubble.className='bubble';
  if(cancelledSends.has(item.clientMessageId)&&!item.deletedAt){const text=document.createElement('p');text.textContent='Cancelling send…';bubble.append(text);return bubble;}
  if(item.deletedAt){bubble.classList.add('deleted-message');const text=document.createElement('p');text.textContent='Message deleted';bubble.append(text);return bubble;}
  if(item.type==='CALL'){bubble.classList.add('call-history-bubble');const title=document.createElement('strong');title.textContent=(item.senderId===selfId?'Outgoing':'Incoming')+' video call';const detail=document.createElement('p');const state=item.call?.state;detail.textContent=({ENDED:'Ended',MISSED:'Missed',DECLINED:'Declined',CANCELLED:'Cancelled',FAILED:'Connection interrupted'})[state]||'Video call';if(item.call?.connectedAt)detail.textContent+=' · '+callDuration(item.call.durationSeconds);bubble.append(title,detail);return bubble;}
  const quoted=item.reply||((item.replyToId&&records.has(item.replyToId))?quoteOf(records.get(item.replyToId)):null);if(quoted)bubble.append(quoteElement(quoted));
  if(item.type==='FILE'){bubble.classList.add('attachment-bubble');if(item.localPreview)appendLocalMedia(bubble,item);else if(item.attachment)appendMedia(bubble,item.attachment);else{const text=document.createElement('p');text.textContent='Attachment unavailable locally';bubble.append(text);}}
  if(item.type==='GIFT'){
    bubble.classList.add('gift-bubble');
    if(safeAsset(item.giftAssetPath)){
      const img=document.createElement('img');img.src=item.giftAssetPath;img.alt=item.giftName;img.width=144;img.height=144;img.loading='lazy';img.addEventListener('error',()=>{img.hidden=true;});bubble.append(img);
    }
    const label=document.createElement('strong');label.textContent=item.giftName||'A little gift';bubble.append(label);
  }
  if(item.body){const text=document.createElement('p');text.textContent=item.body;bubble.append(text);}
  return bubble;
}
function messageMetadata(item){
  const meta=document.createElement('div');meta.className='message-meta';const stamp=document.createElement('time');stamp.dateTime=item.createdAt;stamp.textContent=time(item.createdAt);meta.append(stamp);
  if(item.editedAt&&!item.deletedAt){const edited=document.createElement('span');edited.className='message-edited';edited.textContent='Edited';edited.title='Edited '+date(item.editedAt)+' '+time(item.editedAt);meta.append(edited);}
  if(item.senderId===selfId&&item.type!=='CALL'){const status=document.createElement('span');status.textContent=item.deletedAt?'Deleted':item.readAt?'Read':item.deliveredAt?'Delivered':'Sent';meta.append(status);}
  return meta;
}
function render(mode='normal'){
  const messages=[...records.values()].sort((a,b)=>a.sequence-b.sequence),outbox=[...pending.values()];
  const identity=JSON.stringify([messages,outbox.map(({lastAttempt,...p})=>p)]);
  // CHAT-08: transmit() may already render the same outbox before send requests
  // bottom alignment. An unchanged DOM must still honor that explicit action.
  if(identity===renderIdentity){if(mode==='bottom'){log.scrollTop=log.scrollHeight;$('#new-messages').hidden=true;}queueAcks();return;}renderIdentity=identity;
  const bottom=nearBottom(),height=log.scrollHeight,top=log.scrollTop,fragment=document.createDocumentFragment();let lastDay='';
  for(const item of messages){
    const day=date(item.createdAt);
    if(day!==lastDay){const divider=document.createElement('div');divider.className='day-divider';const span=document.createElement('span');span.textContent=day;divider.dataset.day=day;divider.append(span);fragment.append(divider);lastDay=day;}
    const row=document.createElement('article');row.className=`message${item.senderId===selfId?' mine':''}`;row.dataset.id=item.id;messageSignatures.set(row,JSON.stringify([item.body,item.type,item.giftCode,item.replyToId,item.reply,item.deletedAt,item.editedAt,item.call,item.attachmentId,item.attachment,cancelledSends.has(item.clientMessageId)]));
    const bubble=messageBubble(item);
    const meta=messageMetadata(item);
    const content=document.createElement('div');content.className='message-content';content.tabIndex=0;content.setAttribute('aria-label','Message. Shift+F10 opens message options.');content.append(bubble);bindMessageInteractions(content,{onReactions:options=>showHoverReactions(item.id,content,options),onLeave:scheduleHideReactions,onContext:point=>openMessageActions(item.id,point)});if(!item.deletedAt&&item.reactions?.length){content.append(reactionBar(item));content.classList.add('has-reactions');}row.append(content,meta);fragment.append(row);
  }
  for(const item of outbox){
    const row=document.createElement('article');row.className=`message mine${item.state==='Failed'?' failed':''}`;row.dataset.pendingId=item.clientMessageId;
    messageSignatures.set(row,JSON.stringify([item.body,item.type,item.giftCode,item.replyToId,item.localPreview,item.attachment,cancelledSends.has(item.clientMessageId)]));
    const content=document.createElement('div');content.className='message-content';content.append(messageBubble(item));
    const meta=document.createElement('div');meta.className='message-meta pending-meta';const status=document.createElement('span');status.className=item.state==='Failed'?'send-status failed-status':'send-status is-sending';status.textContent=item.state==='Failed'?'Failed':'Sending…';status.title=item.error||'Waiting for server confirmation';status.setAttribute('role','status');meta.append(status);
    if(item.state==='Failed'){const retry=document.createElement('button');retry.type='button';retry.className='retry-message';retry.textContent='Retry';retry.addEventListener('click',()=>transmit(item,true));meta.append(retry);if(item.code==='REPLY_UNAVAILABLE'&&item.replyToId){const detach=document.createElement('button');detach.type='button';detach.className='retry-message';detach.textContent='Send without reply';detach.addEventListener('click',()=>resendWithoutReply(item.clientMessageId));meta.append(detach);}}
    const cancel=document.createElement('button');cancel.type='button';cancel.className='cancel-send';cancel.textContent='×';cancel.title='Cancel sending';cancel.setAttribute('aria-label','Cancel sending');cancel.addEventListener('click',()=>cancelFileIntent(item.clientMessageId));meta.append(cancel);row.append(content,meta);fragment.append(row);
  }
  if(!messages.length&&!outbox.length){empty.hidden=false;fragment.append(empty);}
  reconcileMessageRows(fragment);
  if(mode==='older')log.scrollTop=top+log.scrollHeight-height;
  else if(initial||mode==='bottom'||bottom){log.scrollTop=log.scrollHeight;$('#new-messages').hidden=true;}
  else{log.scrollTop=top;$('#new-messages').hidden=false;}
  if(reactionMessageId){const id=reactionMessageId,anchor=log.querySelector('.message[data-id="'+id+'"] .message-content');if(anchor&&!records.get(id)?.deletedAt)showHoverReactions(id,anchor);else hideHoverReactions();}
  initial=false;renderReplyDraft();renderMessageActions();queueAcks();
}
function visibleIncoming(){
  if(document.visibilityState!=='visible'||!document.hasFocus()||$('#gifts-dialog').open||$('#message-actions').open||$('#message-editor').open||$('#notification-inbox').open||document.getElementById('call-panel')?.hidden===false)return [];
  const bounds=log.getBoundingClientRect(),top=Math.max(0,bounds.top),bottom=Math.min(innerHeight,bounds.bottom);
  if(bottom<=top)return [];
  return [...log.querySelectorAll('.message[data-id]')].filter(node=>{const message=records.get(node.dataset.id),r=node.getBoundingClientRect();return message?.senderId!==selfId&&!message?.deletedAt&&!message?.readAt&&r.bottom>top&&r.top<bottom;}).map(node=>node.dataset.id);
}
function flushAcks(){
  if(!started||!live?.connected()||Date.now()-lastAck<450)return;lastAck=Date.now();
  const delivered=[...records.values()].filter(m=>m.senderId!==selfId&&!m.deletedAt&&!m.deliveredAt).slice(0,50).map(m=>m.id),read=visibleIncoming().slice(0,50);
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
    let more=false,serverChange=changeVersion;
    do{const page=await get(`/api/chat?after=${cursor}`);if(generation!==epoch)return;metadata(page);serverChange=page.changeVersion??0;for(const m of page.items)merge(m);if(page.items.length)cursor=Math.max(cursor,page.items.at(-1).sequence);more=page.hasMore;if(!more)cursor=Math.max(cursor,page.latestSequence);render();}while(more&&started);
    if(serverChange!==changeVersion){await refreshLoadedMessages(generation);if(generation!==epoch)return;changeVersion=serverChange;}
    await refreshReceipts();if(generation!==epoch)return;clearError();queueAcks();
  }catch(error){if(generation===epoch)showError(error.message);}
  finally{loading=false;if(resync&&started){resync=false;requestSync();}}
}
function transmit(item,manual=false){
  if(!started||cancelledSends.has(item.clientMessageId)||item.state==='Failed'&&!manual)return;
  if(manual){chatMedia?.abort(item.clientMessageId);item.deadlineAt=Date.now()+SEND_WAIT_MS;item.uploading=false;}
  item.deadlineAt??=Date.now()+SEND_WAIT_MS;
  if(Date.now()>=item.deadlineAt){failSend(item,'Send timed out after 5 minutes. Retry or cancel.');return;}
  item.state='Sending…';item.error='';item.code=null;
  if(item.type==='FILE'&&!item.attachmentId){
    if(item.uploading)return;item.uploading=true;const deadline=item.deadlineAt;
    chatMedia.upload(item).then(attachment=>{if(pending.get(item.clientMessageId)!==item||cancelledSends.has(item.clientMessageId)||item.deadlineAt!==deadline||item.state==='Failed')return;item.uploading=false;item.attachmentId=attachment.id;item.attachment=attachment;persist();transmit(item);}).catch(error=>{if(pending.get(item.clientMessageId)===item&&item.deadlineAt===deadline&&item.state!=='Failed'&&error.name!=='AbortError')failSend(item,error.message);});persist();render();return;
  }
  if(!live?.connected()){persist();render();return;}
  if(item.retryable===false&&!manual)return;
  item.lastAttempt=Date.now();
  if(live.send('chat.send',{clientMessageId:item.clientMessageId,body:item.body,...(item.replyToId?{replyToId:item.replyToId}:{}),...(item.type==='FILE'?{type:'FILE',attachmentId:item.attachmentId}:item.type==='GIFT'?{type:'GIFT',giftCode:item.giftCode}:{})}))attempted.add(item.clientMessageId);
  persist();render();
}
function failSend(item,error){item.state='Failed';item.error=error;item.uploading=false;chatMedia?.failed(item.clientMessageId);persist();render();}
function checkSendTimeouts(){for(const item of pending.values())if(item.state!=='Failed'&&Date.now()>=(item.deadlineAt||0))failSend(item,'Send timed out after 5 minutes. Retry or cancel.');}
function flushPending(){retryCancelledSends();for(const p of pending.values())transmit(p);}
function onEvent(event){
  refreshNotifications();
  if(!started)return;
  if(event.type==='memory.published'&&event.notification){$('#memory-notice').textContent=event.notification;$('#memory-notice').hidden=false;return;}
  let changed=false;
  if(event.message&&event.message.senderId!==selfId&&!event.message.deletedAt&&!event.message.hidden&&!records.has(event.message.id))notificationSounds.message(event.message.id);
  if(event.notification&&event.message&&!records.has(event.message.id))$('#gift-notice').textContent=event.notification;
  if(event.message)changed=merge(event.message);
  for(const value of event.receipts||[])changed=receipt(value)||changed;
  if(event.type==='chat.error'){
    const item=pending.get(event.clientMessageId);
    if(item){
      const friendly={MESSAGE_NOT_SAVED:item.type==='GIFT'?'Your gift was not sent. Retry.':'We could not confirm this message was saved. Your text is still here. Retry.',MESSAGE_CONFLICT:'This message changed in another attempt. Reload your conversation before trying again.',INVALID_MESSAGE_ID:'Reload your conversation before trying again.'};
      item.state='Failed';chatMedia?.failed(item.clientMessageId);item.code=event.code;item.error=friendly[event.code]||event.detail||'Message not confirmed. Retry.';item.retryable=!['MESSAGE_CONFLICT','EMPTY_MESSAGE','MESSAGE_TOO_LONG','INVALID_MESSAGE_ID','INVALID_GIFT','GIFT_NOTE_TOO_LONG'].includes(event.code);persist();changed=true;
    }
    else if(event.code!=='ACK_NOT_SAVED')$('#connection').textContent=event.detail||'Receipt update will retry.';
  }
  if(changed)render();
  if(event.type!=='chat.error')requestSync();
}
async function start(){
  if(started)return;started=true;const generation=++epoch;clearError();$('#connection').textContent='Loading your conversation…';
  try{
    const page=await get('/api/chat');if(generation!==epoch)return;metadata(page);storageKey=`couple.chat.outbox.${selfId}`;restore();changeVersion=page.changeVersion??0;cursor=page.latestSequence;hasOlder=page.hasMore;for(const item of page.items)merge(item);$('#chat-content').hidden=false;composeControls();initial=true;render();
    get('/api/presence').then(state=>{if(generation===epoch){partnerActivity=state;$('#connection').textContent=activityText(state);}}).catch(()=>{if(generation===epoch)$('#connection').textContent='Status unavailable';});
    live=chatSocket({onEvent,onStatus:text=>{if(started)$('#connection').title=text;},onExpired:expire,onReady:()=>{sync().then(()=>{if(started)flushPending();});}});
    poll=setInterval(()=>{if(!document.hidden){sync();retryCancelledSends();}},15000);
    sendTimer=setInterval(checkSendTimeouts,1000);
    ackTimer=setInterval(flushAcks,3000);
    if(location.hash==='#gifts')openGifts();
  }catch(error){if(generation===epoch){showError(error.message);started=false;}}
}
$('#composer').addEventListener('submit',event=>{
  event.preventDefault();$('#compose-error').textContent='';$('#message-body').removeAttribute('aria-invalid');
  const body=$('#message-body').value;
  if(chatMedia?.hasDraft()){if(pending.size>=50){$('#compose-error').textContent='Send or cancel pending messages first.';return;}chatMedia.send(body,replyTargetId);return;}
  if(!body.trim()||Array.from(body).length>4000){$('#compose-error').textContent=body.trim()?'Use up to 4,000 characters.':'Write a message first.';$('#message-body').setAttribute('aria-invalid','true');$('#message-body').focus();return;}
  if(pending.size>=50){$('#compose-error').textContent='Your tab has 50 unsent messages. Reconnect and send them before adding more.';return;}
  const item={clientMessageId:crypto.randomUUID(),body,replyToId:replyTargetId,state:'Queued',retryable:true};pending.set(item.clientMessageId,item);persist();replyTargetId=null;renderReplyDraft();$('#message-body').value='';updateCount();transmit(item);render('bottom');$('#message-body').focus({preventScroll:true});
});
function updateCount(){resizeComposer();composeControls();$('#character-count').hidden=Array.from($('#message-body').value).length<3800;$('#character-count').textContent=`${Array.from($('#message-body').value).length.toLocaleString()} / 4,000`;}
$('#message-body').addEventListener('input',updateCount);
$('#message-body').addEventListener('keydown',event=>{if(event.key==='Enter'&&!event.shiftKey&&!event.isComposing){event.preventDefault();$('#composer').requestSubmit();}});
$('#emoji-toggle').addEventListener('click',()=>openGifts('emoji'));
for(const button of $$('#emoji-picker button'))button.addEventListener('click',()=>{const input=$('#message-body');input.setRangeText(button.textContent,input.selectionStart,input.selectionEnd,'end');$('#gifts-dialog').close();input.focus();updateCount();});
async function loadEarlier(){
  if(olderBusy||!hasOlder||!records.size||!started)return;olderBusy=true;const generation=epoch;$('#older').hidden=true;$('#older-status').textContent='Loading earlier messages…';$('#older-status').classList.add('is-loading');
  try{const min=Math.min(...[...records.values()].map(m=>m.sequence)),page=await get(`/api/chat?before=${min}`);if(!started||generation!==epoch)return;hasOlder=page.hasMore;for(const m of page.items)merge(m);render('older');$('#older-status').textContent='';}
  catch(error){$('#older-status').textContent='Could not load earlier messages.';$('#older').hidden=false;}
  finally{olderBusy=false;$('#older-status').classList.remove('is-loading');}
}
$('#older').addEventListener('click',loadEarlier);
$('#new-messages').addEventListener('click',()=>{log.scrollTop=log.scrollHeight;$('#new-messages').hidden=true;queueAcks();});
log.addEventListener('scroll',()=>{if(log.scrollTop<48)loadEarlier();if(nearBottom())$('#new-messages').hidden=true;queueAcks();},{passive:true});
$('#retry-load').addEventListener('click',()=>started?sync():start());
window.addEventListener('focus',()=>{if(started){checkSendTimeouts();sync();queueAcks();}});window.addEventListener('resize',queueAcks);
document.addEventListener('visibilitychange',()=>{if(!document.hidden&&started){checkSendTimeouts();sync();queueAcks();}});
$('#logout').addEventListener('click',async()=>{
  $('#logout').disabled=true;
  try{const c=await(await fetch('/api/csrf',{cache:'no-store',signal:AbortSignal.timeout(10000)})).json(),r=await fetch('/api/auth/logout',{method:'POST',headers:{[c.headerName]:c.token},signal:AbortSignal.timeout(10000)});if(!r.ok&&r.status!==401)throw new Error('logout');clearOutbox();live?.stop();location.replace('/?signedout=1');}
  catch{showError('Sign out could not be confirmed. Check your connection and try again.');$('#logout').disabled=false;}
});
// DEV-06 / F05: explicit preview/confirm, never create a message when choosing or cancelling.
function resetGiftPreview(){selectedGift=null;$('#gift-preview').hidden=true;$('#gift-browse').hidden=false;$('#gift-note').value='';$('#gift-note-count').textContent='0 / 500';$('#gift-error').textContent='';}
function openGifts(direction='browse'){if(!started||!selfId)return;giftDirection=direction;resetGiftPreview();for(const tab of $$('[data-gift-direction]'))tab.setAttribute('aria-pressed',String(tab.dataset.giftDirection===direction));$('#gifts-dialog').showModal();$('#emoji-toggle').setAttribute('aria-expanded','true');loadGifts();}
function chooseGift(gift){selectedGift=gift;$('#gift-browse').hidden=true;$('#gift-preview').hidden=false;$('#gift-preview-name').textContent=gift.name;$('#gift-preview-partner').textContent=`A little surprise for ${partner.displayName}`;$('#gift-preview-image').src=gift.assetPath;$('#gift-preview-image').alt=gift.name;$('#gift-preview-image').hidden=false;$('#gift-note').focus();}
async function loadGifts(more=false){
  const emoji=giftDirection==='emoji';$('#emoji-picker').hidden=!emoji;$('#gift-grid').hidden=emoji;$('#gift-browse .gift-tools').hidden=emoji;$('#gift-browse .gift-actions').hidden=emoji;$('#gift-browse .gift-credit').hidden=emoji;$('#gift-search-fields').hidden=emoji;if(emoji){giftGeneration++;giftBusy=false;giftHasMore=false;$('#gift-status').textContent='';return;}
  const generation=++giftGeneration;giftBusy=true;$('#gift-more').disabled=true;$('#gift-error').textContent='';$('#gift-status').textContent='Loading stickers…';
  if(!more){giftHasMore=false;giftOffset=0;giftBefore=null;$('#gift-grid').replaceChildren();}
  $('#gift-search-fields').hidden=giftDirection!=='browse';$('#gift-more').hidden=true;
  try{
    const path=giftDirection==='browse'?`/api/gifts?pack=${encodeURIComponent($('#gift-pack').value)}&q=${encodeURIComponent($('#gift-search').value)}&offset=${giftOffset}`:`/api/gifts/history?direction=${giftDirection}${giftBefore?`&before=${giftBefore}`:''}`;
    const page=await get(path);if(generation!==giftGeneration)return;
    if(page.packs){const select=$('#gift-pack'),previous=select.value;select.replaceChildren(new Option('All packs',''),...page.packs.map(p=>new Option(p==='blobfox'?'Blobfox · foxes':p==='bunhd'?'BunHD · bunnies':p,p)));select.value=previous;}
    const fragment=document.createDocumentFragment();
    for(const item of page.items){
      if(giftDirection==='browse'){
        const button=document.createElement('button');button.type='button';button.className='sticker-choice';button.setAttribute('aria-label',`Preview ${item.name}`);
        const img=document.createElement('img');img.src=item.assetPath;img.alt='';img.width=80;img.height=80;img.loading='lazy';img.addEventListener('error',()=>{img.hidden=true;});
        const name=document.createElement('span');name.textContent=item.name;button.append(img,name);button.addEventListener('click',()=>chooseGift(item));fragment.append(button);
      }else{
        const card=document.createElement('article');card.className='gift-history-card';card.append(messageBubble(item));const who=document.createElement('p');who.className='gift-history-meta';who.textContent=`${item.senderId===selfId?'You':partner.displayName} · ${date(item.createdAt)} · ${time(item.createdAt)}`;card.append(who);fragment.append(card);
      }
    }
    $('#gift-grid').append(fragment);giftOffset+=page.items.length;if(page.items.length)giftBefore=page.items.at(-1).sequence;
    giftHasMore=page.hasMore;$('#gift-more').hidden=true;$('#gift-status').textContent=giftDirection==='browse'?`${page.total} stickers · choose one to preview`:(page.items.length||more?'Your saved stickers.':'No stickers here yet. Choose one from Browse.');
  }catch(error){if(generation===giftGeneration){$('#gift-error').textContent=error.message;giftHasMore=false;$('#gift-more').hidden=false;$('#gift-status').textContent='Stickers could not be loaded.';}}
  finally{if(generation===giftGeneration){giftBusy=false;$('#gift-more').disabled=false;requestAnimationFrame(autoLoadStickers);}}
}
$('#gift-sidebar').addEventListener('click',event=>{event.preventDefault();openGifts();});
$('#gifts-close').addEventListener('click',()=>$('#gifts-dialog').close());$('#gift-cancel').addEventListener('click',()=>{resetGiftPreview();$('#gift-search').focus();});
$('#gifts-dialog').addEventListener('close',()=>{giftGeneration++;resetGiftPreview();$('#emoji-toggle').setAttribute('aria-expanded','false');});
$('#gift-preview-image').addEventListener('error',()=>{$('#gift-preview-image').hidden=true;});
$('#gift-note').addEventListener('input',()=>{$('#gift-note-count').textContent=`${Array.from($('#gift-note').value).length} / 500`;});
$('#gift-confirm').addEventListener('click',()=>{
  if(!selectedGift)return;const body=$('#gift-note').value;
  if(Array.from(body).length>500){$('#gift-error').textContent='Keep your note within 500 characters.';$('#gift-note').focus();return;}
  if(pending.size>=50){$('#gift-error').textContent='Reconnect and send your pending messages before adding more.';return;}
  const item={clientMessageId:crypto.randomUUID(),type:'GIFT',giftCode:selectedGift.code,giftName:selectedGift.name,giftAssetPath:selectedGift.assetPath,body,replyToId:replyTargetId,state:'Queued',retryable:true};
  pending.set(item.clientMessageId,item);persist();replyTargetId=null;renderReplyDraft();$('#gifts-dialog').close();$('#gift-notice').textContent='Your sticker is waiting for confirmation.';transmit(item);render('bottom');
});
for(const button of $$('[data-gift-direction]'))button.addEventListener('click',()=>{giftDirection=button.dataset.giftDirection;for(const tab of $$('[data-gift-direction]'))tab.setAttribute('aria-pressed',String(tab===button));resetGiftPreview();loadGifts();});
$('#gift-pack').addEventListener('change',()=>loadGifts());$('#gift-search').addEventListener('input',()=>{clearTimeout(giftSearchTimer);giftSearchTimer=setTimeout(()=>loadGifts(),250);});
$('#gift-reload').addEventListener('click',()=>loadGifts());$('#gift-more').addEventListener('click',()=>{if(!giftBusy)loadGifts(true);});
$('#animation-toggle').checked=!matchMedia('(prefers-reduced-motion: reduce)').matches;
function animationPreference(){document.body.classList.toggle('still-stickers',!$('#animation-toggle').checked);}
$('#animation-toggle').addEventListener('change',animationPreference);animationPreference();
window.addEventListener('pageshow',start);
window.addEventListener('pagehide',()=>{chatMedia?.dispose();started=false;epoch++;live?.stop();clearInterval(poll);clearInterval(sendTimer);clearInterval(ackTimer);clearTimeout(ackDebounce);clearTimeout(syncDebounce);$('#chat-content').hidden=true;hideHoverReactions();records.clear();pending.clear();attempted.clear();hiddenMessages.clear();replyTargetId=null;actionMessageId=null;$('#message-actions').close();$('#message-editor').close();editTarget=null;$('#message-edit-body').value='';$('#message-action-preview').textContent='';$('#message-action-error').textContent='';$('#reply-draft-text').textContent='';$('#reply-draft').hidden=true;selfId=null;renderIdentity='';});
start();

// CHAT-02A: projections are server-derived; never cache another user's quote in the outbox.
function quoteOf(item){return {id:item.id,senderId:item.senderId,body:item.deletedAt?null:item.body,type:item.type,giftName:item.attachment?.name||item.giftName,unavailable:!!item.deletedAt};}
function quoteElement(quote){const box=document.createElement('blockquote');box.className='message-quote';const sender=document.createElement('strong');sender.textContent=quote.unavailable?'Original message':quote.senderId===selfId?'You':partner?.displayName||'Your person';const text=document.createElement('p');text.textContent=quote.unavailable?'Original message unavailable':quote.body||quote.giftName||(quote.type==='FILE'?'Attachment':'Sticker');box.append(sender,text);return box;}
function redactReferences(id){for(const item of records.values())if(item.replyToId===id)item.reply={id,unavailable:true};}
function renderReplyDraft(){const target=replyTargetId?records.get(replyTargetId):null;if(replyTargetId&&(!target||target.deletedAt)){replyTargetId=null;$('#compose-error').textContent='The original message is unavailable. Reply cleared; your text is kept.';}$('#reply-draft').hidden=!replyTargetId;if(replyTargetId){$('#reply-draft-title').textContent='Replying to '+(target.senderId===selfId?'yourself':partner?.displayName||'your person');$('#reply-draft-text').textContent=target.body||target.attachment?.name||target.giftName||'Attachment';}}
$('#reply-cancel').addEventListener('click',()=>{replyTargetId=null;renderReplyDraft();$('#message-body').focus();});
async function refreshLoadedMessages(generation){const ids=[...records.keys()];for(let i=0;i<ids.length;i+=50){const snapshot=await get('/api/chat/messages?ids='+ids.slice(i,i+50).join(','));if(generation!==epoch)return;for(const id of snapshot.removedIds){hiddenMessages.add(id);records.delete(id);redactReferences(id);}for(const item of snapshot.items)merge(item);}render();}
async function messageAction(path,body,method='POST'){
 const csrf=await get('/api/csrf');const response=await fetch(path,{method,headers:{[csrf.headerName]:csrf.token,'Content-Type':'application/json'},body:body===undefined?undefined:JSON.stringify(body),signal:AbortSignal.timeout(10000)});
 if(response.status===401){expire();throw new Error('Please sign in again.');}if(!response.ok){const failure=await response.json().catch(()=>({}));throw new Error(failure.message||'This action was not saved. Refresh and retry.');}return response.status===204?null:response.json();
}
function reactionBar(item){const bar=document.createElement('div');bar.className='message-reaction-bar';const own=item.reactions.find(r=>r.userId===selfId)?.emoji;for(const emoji of reactionChoices){const count=item.reactions.filter(r=>r.emoji===emoji).length;if(!count)continue;const button=document.createElement('button');button.type='button';button.textContent=emoji+' '+count;button.setAttribute('aria-pressed',String(own===emoji));button.setAttribute('aria-label',emoji+' reaction, '+count+' people. '+(own===emoji?'Remove your reaction':'Change your reaction'));button.addEventListener('click',()=>reactTo(item.id,own===emoji?null:emoji));bar.append(button);}return bar;}
function openMessageActions(id,point){if(actionBusy||!records.has(id))return;hideHoverReactions();actionMessageId=id;actionMode=null;contextPoint=point||{clientX:20,clientY:80};$('#message-action-error').hidden=true;renderMessageActions();if(!$('#message-actions').open)$('#message-actions').showModal();placeMessageMenu();($('#message-reply').disabled?$('#message-hide'):$('#message-reply')).focus({preventScroll:true});}
function renderMessageActions(){if(!actionMessageId)return;$('#message-actions').setAttribute('aria-busy',String(actionBusy));$('#message-actions-close').disabled=actionBusy;const item=records.get(actionMessageId);if(!item){$('#message-actions').close();actionMessageId=null;return;}$('#message-action-preview').textContent=item.deletedAt?'Message deleted':item.body||item.attachment?.name||item.giftName||'Attachment';$('#message-action-choices').hidden=!!actionMode;$('#message-confirmation').hidden=!actionMode;$('#message-reply').disabled=!!item.deletedAt||item.type==='CALL'||actionBusy;$('#message-delete').hidden=item.senderId!==selfId||!!item.deletedAt||item.type==='CALL';$('#message-edit').hidden=item.senderId!==selfId||!!item.deletedAt||item.type==='CALL';$('#message-edit').disabled=actionBusy;$('#message-hide').disabled=actionBusy;$('#message-delete').disabled=actionBusy;$('#message-confirm').disabled=actionBusy;$('#message-confirm-cancel').disabled=actionBusy;
}
async function reactTo(id,emoji){if(actionBusy)return;actionBusy=true;renderMessageActions();const generation=epoch;try{const item=await messageAction('/api/chat/messages/'+id+'/reaction',{emoji},'PUT');if(generation!==epoch)return;merge(item);render();if($('#message-actions').open)$('#message-actions').close();}catch(error){if(generation!==epoch)return;$('#compose-error').textContent=error.message;requestSync();}finally{actionBusy=false;renderMessageActions();}}
$('#message-actions-close').addEventListener('click',()=>{if(!actionBusy)$('#message-actions').close();});$('#message-actions').addEventListener('cancel',event=>{if(actionBusy)event.preventDefault();});
$('#message-reply').addEventListener('click',()=>{const item=records.get(actionMessageId);if(!item||item.deletedAt)return;replyTargetId=item.id;renderReplyDraft();$('#message-actions').close();$('#message-body').focus();});
function confirmMessageAction(mode){actionMode=mode;$('#message-confirmation-text').textContent=mode==='hide'?'Delete this message from your view? Your partner will still see it.':'Delete this message for both of you? Its content and reactions will be removed and replaced with “Message deleted”.';$('#message-confirm').textContent=mode==='hide'?'Delete for me':'Delete for everyone';renderMessageActions();placeMessageMenu();$('#message-confirm').focus();}
$('#message-hide').addEventListener('click',()=>confirmMessageAction('hide'));$('#message-delete').addEventListener('click',()=>confirmMessageAction('delete'));$('#message-confirm-cancel').addEventListener('click',()=>{actionMode=null;renderMessageActions();});
$('#message-confirm').addEventListener('click',async()=>{if(actionBusy||!actionMessageId||!actionMode)return;actionBusy=true;renderMessageActions();const id=actionMessageId,mode=actionMode,generation=epoch;try{const item=await messageAction('/api/chat/messages/'+id+'/'+mode);if(generation!==epoch)return;if(mode==='hide'){hiddenMessages.add(id);records.delete(id);redactReferences(id);}else merge(item);$('#message-actions').close();render();await sync();}catch(error){if(generation===epoch){$('#message-action-error').hidden=false;$('#message-action-error').textContent=error.message;placeMessageMenu();requestSync();}}finally{actionBusy=false;renderMessageActions();}});

function resendWithoutReply(id){const item=pending.get(id);if(!item?.replyToId||item.code!=='REPLY_UNAVAILABLE')return;const replacement={...item,clientMessageId:item.type==='FILE'?id:crypto.randomUUID(),replyToId:null,code:null,state:'Queued',error:'',retryable:true,lastAttempt:0,deadlineAt:Date.now()+SEND_WAIT_MS};pending.delete(id);attempted.delete(id);pending.set(replacement.clientMessageId,replacement);persist();transmit(replacement);render('bottom');}

// CHAT-02B: hover reacts; context menu Reply/Delete; keyboard and touch equivalents.
function bindMessageInteractions(element,{onReactions,onLeave,onContext},timers={setTimeout:(callback,delay)=>setTimeout(callback,delay),clearTimeout:id=>clearTimeout(id)}){
 let hold=null,start=null,longPressed=false,pointerKind='mouse';
 const cancel=()=>{timers.clearTimeout(hold);hold=null;};
 const touch=()=>pointerKind==='touch'||pointerKind==='pen';
 const reveal=()=>{if(!longPressed){longPressed=true;onReactions({persistent:true});}};
 element.addEventListener('pointerenter',event=>{if(event.pointerType==='mouse'){pointerKind='mouse';onReactions({persistent:false});}});
 element.addEventListener('pointerleave',()=>{cancel();start=null;if(!touch())onLeave();});
 element.addEventListener('focusin',()=>{if(!touch())onReactions({persistent:false});});
 element.addEventListener('focusout',event=>{if(!touch()&&!element.contains(event.relatedTarget))onLeave();});
 element.addEventListener('contextmenu',event=>{event.preventDefault();event.stopPropagation?.();cancel();if(touch()||event.pointerType==='touch'||event.pointerType==='pen'){reveal();return;}onContext({clientX:event.clientX,clientY:event.clientY});});
 element.addEventListener('keydown',event=>{pointerKind='keyboard';if(event.key==='ContextMenu'||event.key==='F10'&&event.shiftKey){event.preventDefault();const rect=element.getBoundingClientRect();onContext({clientX:rect.left+16,clientY:rect.bottom});}});
 element.addEventListener('pointerdown',event=>{pointerKind=event.pointerType||'mouse';if(!touch())return;cancel();longPressed=false;start={x:event.clientX,y:event.clientY};hold=timers.setTimeout(()=>{hold=null;reveal();},500);});
 element.addEventListener('pointermove',event=>{if(start&&Math.hypot(event.clientX-start.x,event.clientY-start.y)>10){cancel();start=null;}});
 element.addEventListener('pointerup',()=>{cancel();start=null;});
 element.addEventListener('pointercancel',()=>{cancel();start=null;});
 element.addEventListener('click',event=>{if(longPressed){event.preventDefault();event.stopImmediatePropagation?.();longPressed=false;}},true);
}
function hideHoverReactions(){clearTimeout(reactionHideTimer);reactionMessageId=null;reactionAnchor=null;reactionPersistent=false;reactionSignature=null;$('#hover-reactions').hidden=true;$('#hover-reactions').replaceChildren();}
function scheduleHideReactions(){clearTimeout(reactionHideTimer);if(!reactionPersistent)reactionHideTimer=setTimeout(hideHoverReactions,180);}
function showHoverReactions(id,anchor,options={}){clearTimeout(reactionHideTimer);const item=records.get(id);if(!item||item.deletedAt||item.type==='CALL'||actionBusy||$('#message-editor').open||$('#message-actions').open){hideHoverReactions();return;}if(reactionMessageId!==id)reactionPersistent=false;if(options.persistent!==undefined)reactionPersistent=options.persistent;reactionMessageId=id;reactionAnchor=anchor;const picker=$('#hover-reactions');const signature=JSON.stringify([id,item.reactions||[]]);if(signature!==reactionSignature){reactionSignature=signature;picker.replaceChildren();const own=item.reactions?.find(r=>r.userId===selfId)?.emoji;
 for(const emoji of reactionChoices){const button=document.createElement('button');button.type='button';button.textContent=emoji;button.setAttribute('aria-label',(own===emoji?'Remove ':'React with ')+emoji);button.setAttribute('aria-pressed',String(own===emoji));button.addEventListener('click',()=>{hideHoverReactions();reactTo(id,own===emoji?null:emoji);});picker.append(button);}
 for(const [label,glyph] of [['Reply','↩'],['More message actions','⋯']]){const button=document.createElement('button');button.type='button';button.textContent=glyph;button.setAttribute('aria-label',label);button.addEventListener('click',()=>{const rect=anchor.getBoundingClientRect();if(label==='Reply'){replyTargetId=id;renderReplyDraft();hideHoverReactions();$('#message-body').focus();}else openMessageActions(id,{clientX:rect.left,clientY:rect.bottom});});picker.append(button);}}
 picker.hidden=false;const rect=anchor.getBoundingClientRect(),width=picker.offsetWidth,height=picker.offsetHeight;picker.style.left=Math.max(8,Math.min(innerWidth-width-8,(records.get(id)?.senderId===selfId?rect.right-width:rect.left)))+'px';picker.style.top=Math.max(8,Math.min(innerHeight-height-8,rect.top-height-4>=8?rect.top-height-4:rect.bottom+4))+'px';
}
function placeMessageMenu(){const panel=$('#message-actions');if(!panel.open)return;const rect=panel.getBoundingClientRect();panel.style.left=Math.max(8,Math.min(innerWidth-rect.width-8,contextPoint?.clientX??20))+'px';panel.style.top=Math.max(8,Math.min(innerHeight-rect.height-8,contextPoint?.clientY??80))+'px';}
$('#hover-reactions').addEventListener('pointerenter',()=>clearTimeout(reactionHideTimer));$('#hover-reactions').addEventListener('pointerleave',scheduleHideReactions);$('#hover-reactions').addEventListener('focusin',()=>clearTimeout(reactionHideTimer));$('#hover-reactions').addEventListener('focusout',event=>{if(!$('#hover-reactions').contains(event.relatedTarget))scheduleHideReactions();});
document.addEventListener('pointerdown',event=>{if(!$('#hover-reactions').contains(event.target)&&!reactionAnchor?.contains(event.target))hideHoverReactions();if($('#message-actions').open&&!insideMessageMenu(event)&&!actionBusy)$('#message-actions').close();});
document.addEventListener('keydown',event=>{if(event.key==='Escape'){hideHoverReactions();if($('#message-actions').open&&!actionBusy){event.preventDefault();$('#message-actions').close();log.querySelector('.message[data-id="'+actionMessageId+'"] .message-content')?.focus({preventScroll:true});}}});
log.addEventListener('scroll',()=>{if(reactionPersistent&&reactionAnchor?.isConnected){const r=reactionAnchor.getBoundingClientRect(),bounds=log.getBoundingClientRect();if(r.bottom<bounds.top||r.top>bounds.bottom)hideHoverReactions();else showHoverReactions(reactionMessageId,reactionAnchor);}else hideHoverReactions();},{passive:true});window.addEventListener('resize',()=>{hideHoverReactions();placeMessageMenu();});

// CHAT-03: compact composer follows the visual viewport when the mobile keyboard opens.
function resizeComposer(){const input=$('#message-body');input.style.height='auto';const cap=Math.min(100,(window.visualViewport?.height||innerHeight)*.22);input.style.height=Math.min(cap,Math.max(36,input.scrollHeight))+'px';input.style.overflowY=input.scrollHeight>cap?'auto':'hidden';}
function resizeChatViewport(){const view=window.visualViewport;document.documentElement.style.setProperty('--chat-height',(view?.height||innerHeight)+'px');document.documentElement.style.setProperty('--chat-top',(view?.offsetTop||0)+'px');resizeComposer();if(nearBottom())log.scrollTop=log.scrollHeight;}
window.visualViewport?.addEventListener('resize',resizeChatViewport);window.visualViewport?.addEventListener('scroll',resizeChatViewport);window.addEventListener('resize',resizeChatViewport);resizeChatViewport();updateCount();
function insideMessageMenu(event){const panel=$('#message-actions'),rect=panel.getBoundingClientRect();return panel.contains(event.target)&&event.clientX>=rect.left&&event.clientX<=rect.right&&event.clientY>=rect.top&&event.clientY<=rect.bottom;}
$('#message-actions').addEventListener('contextmenu',event=>event.preventDefault());
function autoLoadStickers(){const dialog=$('#gifts-dialog');if(dialog.open&&!selectedGift&&giftDirection!=='emoji'&&giftHasMore&&!giftBusy&&dialog.scrollHeight-dialog.scrollTop-dialog.clientHeight<140)loadGifts(true);}
$('#gifts-dialog').addEventListener('scroll',autoLoadStickers,{passive:true});
document.addEventListener('partner-activity',event=>{partnerActivity=event.detail;if(started)$('#connection').textContent=partnerActivity?activityText(partnerActivity):'Status unavailable';});
function renderSilence(){const muted=notificationSounds.muted,button=$('#chat-silence');button.setAttribute('aria-pressed',String(muted));button.setAttribute('aria-label',muted?'Unmute notification sounds':'Mute notification sounds');button.title=muted?'Unmute notification sounds':'Mute notification sounds';button.innerHTML='<svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">'+(muted?'<path d="m3 3 18 18M9 4a6 6 0 0 1 9 5v4M6 6a6 6 0 0 0 0 3c0 6-3 6-3 8h14M10 21h4"/>':'<path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4"/>')+'</svg>';}
$('#chat-silence').addEventListener('click',()=>{notificationSounds.mute(!notificationSounds.muted);renderSilence();window.dispatchEvent(new Event('notification-sounds-change'));});window.addEventListener('notification-sounds-change',renderSilence);renderSilence();

// CHAT-05 media stays inside its own message; all names use textContent.
function safeMedia(url){return typeof url==='string'&&/^\/api\/chat\/assets\/[a-f0-9-]{36}(?:\?thumbnail=true)?$/i.test(url);}
function appendLocalMedia(bubble,item){
 if(typeof item.localPreview!=='string'||!item.localPreview.startsWith('blob:'))return;
 if(item.localKind==='DOCUMENT'){const label=document.createElement('strong');label.textContent=item.localName||'Document';bubble.append(label);return;}
 const player=document.createElement(item.localKind==='IMAGE'?'img':item.localKind==='VIDEO'?'video':'audio');player.src=item.localPreview;if(item.localKind==='IMAGE'){player.alt='Photo being sent';}else{player.controls=true;player.preload='metadata';if(item.localKind==='VIDEO')player.playsInline=true;}bubble.append(player);
}
function appendMedia(bubble,asset){
 if(!safeMedia(asset.url))return;
 if(asset.kind==='DOCUMENT'){const name=document.createElement('strong');name.textContent=asset.name;bubble.append(name);}
 if(asset.kind==='IMAGE'){const image=document.createElement('img');image.src=safeMedia(asset.thumbnail)?asset.thumbnail:asset.url;image.alt=asset.name;image.loading='lazy';image.addEventListener('click',()=>{image.src=asset.url;});bubble.append(image);}
 else if(asset.kind==='VIDEO'||asset.kind==='VOICE'){const player=document.createElement(asset.kind==='VIDEO'?'video':'audio');player.controls=true;player.preload='metadata';player.src=asset.url;if(asset.kind==='VIDEO'){player.playsInline=true;if(safeMedia(asset.thumbnail))player.poster=asset.thumbnail;}bubble.append(player);}
 const menu=document.createElement('details');menu.className='media-more';const toggle=document.createElement('summary');toggle.textContent='⋯';toggle.setAttribute('aria-label','Attachment options');toggle.title='Attachment options';menu.append(toggle);
 const panel=document.createElement('div');panel.className='media-options';const name=document.createElement('span');name.textContent=asset.name;const link=document.createElement('a');link.href=asset.url+'?download=true';link.download=asset.name;link.textContent='Download';panel.append(name,link);menu.append(panel);bubble.append(menu);
}
function composeControls(){const hasText=!!$('#message-body').value.trim(),draft=!!chatMedia?.hasDraft();$('#send').hidden=!hasText&&!draft;$('#voice-record').hidden=hasText||draft;$('#send').disabled=!started||!!chatMedia?.busy();$('#message-body').disabled=!started||!!chatMedia?.busy();$('#voice-record').disabled=!started||draft;$('#attach-open').disabled=!started||draft;}
async function cancelFileIntent(key){chatMedia?.abort(key);cancelledSends.add(key);pending.delete(key);attempted.delete(key);persist();render();await retryCancelledSends();}
async function retryCancelledSends(){if(cancelRetryBusy||!chatMedia||!cancelledSends.size||!started)return;cancelRetryBusy=true;try{for(const key of [...cancelledSends]){await chatMedia.cancelIntent(key);cancelledSends.delete(key);persist();}await sync();}catch(error){$('#compose-error').textContent='Cancellation pending. Reconnect to finish cancelling this send.';}finally{cancelRetryBusy=false;}}
chatMedia=mountChatMedia({csrf:()=>get('/api/csrf'),onError:text=>{$('#compose-error').textContent=text;},onControls:composeControls,queue:item=>{if(!started||pending.size>=50)throw new Error('Reconnect and send or cancel pending messages first.');pending.set(item.clientMessageId,{...item,state:'Sending…',deadlineAt:Date.now()+SEND_WAIT_MS,retryable:true});persist();replyTargetId=null;renderReplyDraft();$('#message-body').value='';updateCount();transmit(pending.get(item.clientMessageId));render('bottom');}});composeControls();

// Keep focused bubbles and media elements connected while receipts/reactions update.
function reconcileMessageRows(fragment){
 const rowKey=row=>row.dataset.id||'pending:'+row.dataset.pendingId;const oldRows=new Map([...log.querySelectorAll('.message[data-id],.message[data-pending-id]')].map(row=>[rowKey(row),row])),days=new Map([...log.querySelectorAll('.day-divider')].map(row=>[row.dataset.day,row]));
 const desired=[...fragment.children].map(row=>{if(row.classList.contains('day-divider'))return days.get(row.dataset.day)||row;const old=oldRows.get(rowKey(row));if(!old||messageSignatures.get(old)!==messageSignatures.get(row))return row;
  old.className=row.className;old.querySelector('.message-meta')?.replaceWith(row.querySelector('.message-meta'));const content=old.querySelector('.message-content'),fresh=row.querySelector('.message-content');content.querySelector('.message-reaction-bar')?.remove();const reactions=fresh.querySelector('.message-reaction-bar');if(reactions)content.append(reactions);content.classList.toggle('has-reactions',!!reactions);return old;});
 const kept=new Set(desired);for(const row of [...log.children])if(!kept.has(row)){for(const player of row.querySelectorAll('audio,video')){player.pause();player.removeAttribute('src');player.load();}row.remove();}
 let cursor=log.firstChild;for(const row of desired){if(row===cursor)cursor=cursor.nextSibling;else log.insertBefore(row,cursor);}
}

// CHAT-07: plain text, ownership checked server-side; preserve draft on conflicts/errors.
function videoDuration(seconds){if(!Number.isFinite(seconds)||seconds<0)return '—';if(seconds===0)return '0 min';if(seconds<60)return '<1 min';const minutes=Math.floor(seconds/60);return minutes<60?minutes+' min':Math.floor(minutes/60)+'h '+minutes%60+'m';}
function callDuration(seconds){seconds=Math.max(0,Math.floor(seconds||0));return seconds>=3600?Math.floor(seconds/3600)+':'+String(Math.floor(seconds%3600/60)).padStart(2,'0')+':'+String(seconds%60).padStart(2,'0'):Math.floor(seconds/60)+':'+String(seconds%60).padStart(2,'0');}
function editCount(){const max=records.get(editTarget)?.type==='GIFT'?500:4000;$('#message-edit-count').textContent=Array.from($('#message-edit-body').value).length.toLocaleString()+' / '+max.toLocaleString();}
function editControls(){for(const id of ['message-edit-save','message-edit-cancel','message-edit-reload','message-edit-body'])$('#'+id).disabled=editBusy;$('#message-editor').setAttribute('aria-busy',String(editBusy));}
$('#message-edit').addEventListener('click',()=>{const item=records.get(actionMessageId);if(!item||item.senderId!==selfId||item.deletedAt||item.type==='CALL')return;editTarget=item.id;editVersion=item.version;$('#message-edit-body').value=item.body||'';$('#message-edit-error').textContent='';$('#message-edit-reload').hidden=true;editCount();$('#message-actions').close();$('#message-editor').showModal();$('#message-edit-body').focus();});
$('#message-edit-body').addEventListener('input',editCount);
$('#message-edit-cancel').addEventListener('click',()=>{if(!editBusy){$('#message-editor').close();$('#message-edit-body').value='';editTarget=null;}});
$('#message-editor').addEventListener('cancel',event=>{if(editBusy)event.preventDefault();else{$('#message-edit-body').value='';editTarget=null;}});
$('#message-edit-reload').addEventListener('click',async()=>{if(editBusy||!editTarget)return;editBusy=true;editControls();const generation=epoch,id=editTarget;try{const snapshot=await get('/api/chat/messages?ids='+id);if(generation!==epoch)return;const item=snapshot.items.find(m=>m.id===id);if(!item||item.deletedAt)throw new Error('This message is no longer available. Cancel to close this draft.');merge(item);editVersion=item.version;$('#message-edit-body').value=item.body||'';$('#message-edit-error').textContent='Latest text loaded. Review before saving.';$('#message-edit-reload').hidden=true;editCount();render();}catch(error){if(generation===epoch)$('#message-edit-error').textContent=error.message;}finally{editBusy=false;editControls();}});
$('#message-edit-form').addEventListener('submit',async event=>{event.preventDefault();if(editBusy||!editTarget)return;editBusy=true;editControls();const generation=epoch,id=editTarget;try{const item=await messageAction('/api/chat/messages/'+id,{body:$('#message-edit-body').value,version:editVersion},'PATCH');if(generation!==epoch)return;merge(item);$('#message-editor').close();$('#message-edit-body').value='';editTarget=null;render();requestSync();}catch(error){if(generation===epoch){$('#message-edit-error').textContent=error.message;$('#message-edit-reload').hidden=false;requestSync();}}finally{editBusy=false;editControls();}});
