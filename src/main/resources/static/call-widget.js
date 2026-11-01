import {notificationSounds} from '/notification-sounds.js';
import {callSocket} from '/call-socket.js';
import {requestCallMedia} from '/call-media.js';

// Shared Home/Chat/Memories widget. Static template only; all user content uses textContent.
const template=document.createElement('template');template.innerHTML=`
<button id="notification-mute" type="button" class="notification-mute" aria-pressed="false">Sounds</button>
<button id="call-launcher" type="button" class="call-launcher" aria-controls="call-panel" aria-expanded="false">Calls</button>
<section id="call-panel" class="call-panel" aria-labelledby="call-title" role="region" tabindex="-1" hidden>
 <header class="call-heading"><div><span class="eyebrow">A LITTLE CLOSER</span><h2 id="call-title">Time for just us.</h2></div><button id="call-close" class="secondary" type="button" aria-label="Return to chat while keeping the call connected">← Back to chat</button></header>
 <p id="call-status" class="call-status" role="status">Connecting calls…</p><p id="call-error" class="field-error" role="alert" tabindex="-1"></p>
 <div id="call-stage" class="call-stage" hidden><video id="call-remote" autoplay playsinline aria-label="Your partner's call"></video><div id="call-person" class="call-person"><span>♡</span><strong id="call-partner"></strong><p id="call-peer-state"></p></div><video id="call-local" autoplay muted playsinline aria-label="Your camera preview"></video><span id="call-duration" class="call-duration"></span></div>
 <div id="call-idle-actions" class="call-actions"><button id="call-voice" type="button" class="secondary" disabled>☎ Voice call</button><button id="call-video" type="button" class="primary" disabled>▣ Video call</button></div>
 <div class="call-actions call-controls"><button id="call-answer" class="primary" type="button" hidden>Answer</button><button id="call-stop-setup" class="secondary" type="button" hidden>Cancel device setup</button><button id="call-decline" class="call-danger" type="button" hidden>Decline</button><button id="call-cancel" class="call-danger" type="button" hidden>Cancel call</button><button id="call-resume" class="secondary" type="button" hidden>Reconnect call</button><button id="call-mic" class="secondary" type="button" hidden>Mute microphone</button><button id="call-camera" class="secondary" type="button" hidden>Turn camera off</button><button id="call-end" class="call-danger" type="button" hidden>End call</button><button id="call-play" class="secondary" type="button" hidden>Play partner audio</button></div>
 <p id="call-connectivity" class="fine-print"></p><p class="fine-print">Keep this page open during your call. Leaving the page ends this call.</p>
 <div class="call-footer"><button id="call-sound" type="button" class="text-button">Enable notification sounds</button><button id="call-fullscreen" type="button" class="text-button">Full screen</button><button id="call-history-toggle" type="button" class="text-button">Recent calls</button><button id="call-refresh" type="button" class="text-button">Check call state</button></div><div id="call-history" class="call-history" hidden></div>
</section>`;document.body.append(template.content.cloneNode(true));
const $=id=>document.getElementById(id),live=new Set(['RINGING','CONNECTING','ACTIVE','RECONNECTING']);
const secure=window.isSecureContext&&typeof crypto?.randomUUID==='function'&&crypto.subtle;
const clientId=secure?crypto.randomUUID():null;let clientHash;
let home,call,socket,csrf,rtc,localStream,remoteStream,pc,negotiationId,announced=false,outgoingIce=[],incomingIce=[],seenSignals=new Set(),mediaEpoch=0,offering=false,busy=false,poll,beat,clock,closed=false,clockOffset=0,lastRestart='',eventChain=Promise.resolve(),reported=false,peerCamera=true,peerMic=true,endingId=null,mediaController=null,setupSequence=0;
const own=(v=call)=>v&&home&&(v.callerId===home.self.id?v.callerClient:v.calleeClient)===clientHash;
const isCaller=(v=call)=>v&&home&&v.callerId===home.self.id;
const active=()=>call&&live.has(call.state);
function message(text){$('call-status').textContent=text;}
function failure(error,reveal=false){const box=$('call-error');box.textContent=typeof error==='string'?error:error.message||'Could not complete this call action.';if(reveal&&!$('call-panel').hidden){box.focus({preventScroll:true});box.scrollIntoView({block:'nearest',behavior:'smooth'});}}
let previousFocus;const inerted=[];
function show(){if(closed||!$('call-panel').hidden)return;previousFocus=document.activeElement;for(const element of document.body.children){if(element!==$('call-panel')&&!element.inert){element.inert=true;inerted.push(element);}}$('call-panel').hidden=false;document.body.classList.add('call-view-open');$('call-launcher').setAttribute('aria-expanded','true');$('call-close').focus({preventScroll:true});}
function hideView(){if($('call-panel').hidden)return;$('call-panel').hidden=true;for(const element of inerted.splice(0))element.inert=false;document.body.classList.remove('call-view-open');$('call-launcher').setAttribute('aria-expanded','false');if(document.fullscreenElement===$('call-panel'))document.exitFullscreen?.().catch(()=>{});if(!closed)previousFocus?.focus({preventScroll:true});}
function ring(on){notificationSounds.ring(on?call?.id:null);}
const icons={mic:'<rect x="9" y="2" width="6" height="12" rx="3"/><path d="M5 10v2a7 7 0 0014 0v-2M12 19v3M8 22h8"/>',camera:'<rect x="2" y="5" width="13" height="14" rx="3"/><path d="m15 10 7-4v12l-7-4z"/>',end:'<path d="M3 15v-4c5-5 13-5 18 0v4l-5-1v-3a13 13 0 00-8 0v3z"/>'};
function control(id,icon,label,off=false){const button=$(id);button.innerHTML='<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">'+icons[icon]+(off?'<path d="M2 2 22 22"/>':'')+'</svg><span></span>';button.querySelector('span').textContent=label;button.setAttribute('aria-label',label);button.title=label;}
control('call-end','end','End call');
function soundLabel(){const button=$('call-sound');button.textContent=notificationSounds.muted?'Unmute notification sounds':notificationSounds.enabled?'Mute notification sounds':'Enable notification sounds';button.setAttribute('aria-pressed',String(notificationSounds.muted));const quick=$('notification-mute');quick.textContent=notificationSounds.muted?'Sounds off':notificationSounds.enabled?'Sounds on':'Enable sounds';quick.setAttribute('aria-pressed',String(notificationSounds.muted));}
window.addEventListener('notification-sounds-change',soundLabel);$('notification-mute').addEventListener('click',()=>$('call-sound').click());soundLabel();
function render(){
  const running=active(),selected=running&&own(),incoming=running&&call.state==='RINGING'&&!isCaller(),media=selected&&localStream;
  $('call-title').textContent=running?(call.mode==='VOICE'?'A little voice, a little closer.':'A little face-to-face time.'):'Time for just us.';
  $('call-launcher').textContent=running?(incoming?'Incoming call':selected?'Your call':'Call in another tab'):'Calls';
  $('call-launcher').hidden=!running&&!!document.querySelector('.chat-world');
  $('call-idle-actions').hidden=running;$('call-stage').hidden=!running;$('call-partner').textContent=home?.members.find(m=>m.id!==home.self.id)?.displayName||'Your person';
  $('call-answer').hidden=!incoming;$('call-decline').hidden=!incoming;$('call-cancel').hidden=!(selected&&isCaller()&&call.state==='RINGING');$('call-end').hidden=!(selected&&call.state!=='RINGING');$('call-resume').hidden=!(selected&&call.state!=='RINGING'&&!localStream);
  $('call-stop-setup').hidden=!mediaController;$('call-answer').textContent=mediaController?(call?.mode==='VIDEO'?'Opening camera/microphone…':'Opening microphone…'):'Answer';
  $('call-mic').hidden=!media||call.state==='RINGING';$('call-camera').hidden=!media||call.mode==='VOICE'||call.state==='RINGING';
  const audio=localStream?.getAudioTracks()[0],video=localStream?.getVideoTracks()[0];control('call-mic','mic',audio?.enabled?'Mute microphone':'Unmute microphone',!audio?.enabled);$('call-mic').setAttribute('aria-pressed',String(audio?!audio.enabled:false));control('call-camera','camera',video?.enabled?'Turn camera off':'Turn camera on',!video?.enabled);$('call-camera').setAttribute('aria-pressed',String(video?!video.enabled:false));
  $('call-local').hidden=!running||call.mode==='VOICE'||!video?.enabled;$('call-person').hidden=!!(running&&call.mode==='VIDEO'&&peerCamera&&remoteStream?.getVideoTracks().length);$('call-peer-state').textContent=running?(call.mode==='VOICE'?'Voice call':!peerCamera?'Your partner’s camera is off':'Waiting for your person…')+(peerMic?'':' · Microphone muted'):'';
  for(const id of ['call-voice','call-video'])$(id).disabled=busy||!home||!socket?.connected();for(const id of ['call-answer','call-decline','call-resume'])$(id).disabled=busy||!socket?.connected();for(const id of ['call-cancel','call-end'])$(id).disabled=busy;
  if(running&&!busy){const states={RINGING:incoming?'Your person is calling…':selected?'Calling your person…':'An outgoing call is active in another tab.',CONNECTING:selected?'Connecting your call…':'Answered in another tab.',ACTIVE:selected?'Connected · Just the two of you':'Call active in another tab.',RECONNECTING:selected?'Connection interrupted. Trying again for up to 15 seconds…':'Call reconnecting in another tab.'};message(states[call.state]);}
  if(!running&&call)message(({MISSED:'Call was not answered.',DECLINED:'Call declined.',CANCELLED:'Call cancelled.',ENDED:'Call ended.',FAILED:call.endReason==='SESSION_ENDED'?'Call ended because a session closed.':'The call could not stay connected. Try again.'})[call.state]||'Call ended.');
  if(running&&endingId===call.id){message('Ending call… Your microphone and camera are off.');$('call-resume').hidden=true;}
}
function stopPeer(){
  if(pc){pc.ontrack=null;pc.onicecandidate=null;pc.onconnectionstatechange=null;pc.oniceconnectionstatechange=null;pc.close();pc=null;}
  if(remoteStream)for(const track of remoteStream.getTracks())track.stop();remoteStream=null;$('call-remote').srcObject=null;negotiationId=null;announced=false;outgoingIce=[];incomingIce=[];reported=false;offering=false;
}
function stopMedia(){mediaEpoch++;mediaController?.abort();mediaController=null;stopPeer();if(localStream)for(const track of localStream.getTracks())track.stop();localStream=null;$('call-local').srcObject=null;$('call-play').hidden=true;ring(false);}
async function api(path,method='GET',body){
  const headers={};if(method!=='GET'){if(!csrf)throw new Error('Session token unavailable. Refresh and retry.');headers[csrf.headerName]=csrf.token;headers['Content-Type']='application/json';}
  const response=await fetch(path,{method,headers,body:body===undefined?undefined:JSON.stringify(body),cache:'no-store',signal:AbortSignal.timeout(10000)});
  if(response.status===401){expire();throw new Error('Please sign in again.');}if(!response.ok){const data=await response.json().catch(()=>({}));throw Object.assign(new Error(data.message||'Call state is unavailable. Retry.'),{code:data.code});}return response.json();
}
async function config(){if(!rtc||Date.parse(rtc.expiresAt)<Date.now()+60000){rtc=await api('/api/rtc/config');$('call-connectivity').textContent=rtc.relayConfigured?'Private call connection is configured.':'Local direct calls are available. Calls across different networks may need a configured relay.';}return {iceServers:rtc.iceServers.map(s=>s.username?{urls:s.urls,username:s.username,credential:s.credential}:{urls:s.urls}),iceTransportPolicy:new URLSearchParams(location.search).get('relay')==='1'?'relay':'all'};}
async function send(op,extra={}){return socket.send(op,{eventId:crypto.randomUUID(),clientId,callId:call?.id,...extra});}
function apply(view){
  if(closed||!view)return;if(call?.id===view.id&&view.version<call.version)return;if(call?.id!==view.id&&active()&&!live.has(view.state))return;
  const previous=call;if(previous?.id!==view.id){if(active())stopMedia();endingId=null;lastRestart='';peerCamera=true;peerMic=true;seenSignals.clear();}call=view;clockOffset=Date.parse(view.serverTime)-Date.now();
  if(!live.has(view.state)){endingId=null;setupSequence++;stopMedia();busy=false;render();return;}
  const incoming=view.state==='RINGING'&&!isCaller();if(incoming&&(!previous||previous.id!==view.id)){show();ring(true);}
  if(own()){if(previous?.state==='RINGING'&&view.state!=='RINGING')ring(false);}
  else if(!incoming){stopMedia();if(!$('call-panel').hidden)hideView();}
  render();
  if(!endingId&&own()&&isCaller()&&localStream){if(view.state==='CONNECTING'&&!pc)offer(false).catch(failure);else if(view.state==='RECONNECTING'&&view.recoveryDeadline!==lastRestart){lastRestart=view.recoveryDeadline;offer(true).catch(failure);}}
}
async function sync(){if(!home||closed)return;const current=await api('/api/calls/current');if(current.call)apply(current.call);else if(active()){stopMedia();call=null;endingId=null;busy=false;message('No call is in progress.');render();}}
async function acquire(mode){
  if(!window.isSecureContext||!navigator.mediaDevices?.getUserMedia)throw new Error('Microphone/camera access needs localhost or trusted HTTPS in a supported browser.');
  const epoch=mediaEpoch,controller=new AbortController();mediaController=controller;render();let stream;
  const remaining=call?.state==='RINGING'&&!isCaller()?Date.parse(call.ringDeadline)-(Date.now()+clockOffset):30000;
  try{stream=await requestCallMedia({mediaDevices:navigator.mediaDevices,mode,signal:controller.signal,timeoutMs:Math.min(30000,remaining),onWaiting:()=>{if(mediaController===controller)message('Still opening the device. Check the browser permission icon. You can cancel device setup and retry.');}});}
  finally{if(mediaController===controller)mediaController=null;render();}
  if(epoch!==mediaEpoch||closed){stream.getTracks().forEach(t=>t.stop());throw new Error('Call setup was cancelled.');}localStream=stream;$('call-local').srcObject=stream;$('call-local').play().catch(()=>{});return stream;
}
async function start(mode){
  show();if(!secure)return failure('Microphone/camera calls need localhost or trusted HTTPS in a supported browser.');if(busy||!socket?.connected())return failure('Calls are connecting. Try again shortly.');if(active())return render();
  busy=true;const setup=++setupSequence;$('call-error').textContent='';message('Allow your '+(mode==='VIDEO'?'camera and microphone':'microphone')+' to start.');render();
  try{await config();if(setup!==setupSequence||closed)return;if(new URLSearchParams(location.search).get('relay')==='1'&&!rtc.relayConfigured)throw new Error('Relay-only testing needs a configured TURN relay.');await acquire(mode);if(setup!==setupSequence||closed)return;const view=await send('invite',{callId:null,mode});apply(view);show();}
  catch(error){if(setup!==setupSequence)return;try{await sync();}catch{}if(setup!==setupSequence)return;if(!active()||!own())stopMedia();failure(error,true);}finally{if(setup===setupSequence){busy=false;render();}}
}
async function answer(){
  if(busy||!active())return;busy=true;const setup=++setupSequence,id=call.id;$('call-error').textContent='';message('Opening your microphone'+(call.mode==='VIDEO'?' and camera':'')+'…');render();
  try{await config();if(setup!==setupSequence||call?.id!==id||call.state!=='RINGING'||closed)return;await acquire(call.mode);if(setup!==setupSequence||call?.id!==id||call.state!=='RINGING')throw new Error('This call is no longer ringing.');apply(await send('accept'));ring(false);}
  catch(error){if(setup!==setupSequence)return;stopMedia();failure(error,true);try{await sync();}catch{}}finally{if(setup===setupSequence){busy=false;render();}}
}
async function makePeer(){
  const epoch=mediaEpoch,id=call?.id,settings=await config();if(epoch!==mediaEpoch||id!==call?.id||endingId===id||!localStream)throw new Error('Call setup was cancelled. Reconnect to open your devices again.');stopPeer();negotiationId=null;const peer=new RTCPeerConnection(settings);pc=peer;remoteStream=new MediaStream();$('call-remote').srcObject=remoteStream;for(const track of localStream.getTracks())peer.addTrack(track,localStream);
  peer.ontrack=event=>{if(pc!==peer||epoch!==mediaEpoch)return;if(!remoteStream.getTracks().some(t=>t.id===event.track.id))remoteStream.addTrack(event.track);$('call-remote').play().catch(()=>{$('call-play').hidden=false;});render();};
  peer.onicecandidate=event=>{if(pc!==peer||epoch!==mediaEpoch||!event.candidate)return;const data=event.candidate.toJSON();if(announced&&negotiationId)send('ice',{negotiationId,...data}).catch(failure);else outgoingIce.push(data);};
  const state=()=>{if(pc!==peer||epoch!==mediaEpoch||!active()||!own())return;const value=peer.connectionState;if(value==='connected'&&negotiationId&&!reported){reported=true;send('connected',{negotiationId}).then(apply).catch(failure);sendMedia();}else if(['failed','disconnected'].includes(value)){reported=false;send('disconnected').then(apply).catch(failure);}};peer.onconnectionstatechange=state;peer.oniceconnectionstatechange=state;return peer;
}
async function flushIce(){announced=true;for(const item of outgoingIce.splice(0))await send('ice',{negotiationId,...item});}
async function offer(restart){
  if(offering||!active()||!own()||!isCaller()||!localStream)return;offering=true;const epoch=mediaEpoch;
  try{
    let peer=pc;if(!peer||peer.connectionState==='closed')peer=await makePeer();if(epoch!==mediaEpoch||pc!==peer)return;
    const id=crypto.randomUUID();negotiationId=id;announced=false;outgoingIce=[];incomingIce=[];reported=false;const description=await peer.createOffer({iceRestart:restart});if(epoch!==mediaEpoch||pc!==peer)return;await peer.setLocalDescription(description);await send('offer',{negotiationId:id,sdp:peer.localDescription.sdp});if(epoch!==mediaEpoch||pc!==peer)return;await flushIce();
  }finally{if(epoch===mediaEpoch)offering=false;}
}
async function receive(event){
  if(event.call){apply(event.call);return;}if(!event.payload||!active()||event.callId!==call.id||!own()||seenSignals.has(event.eventId))return;seenSignals.add(event.eventId);if(seenSignals.size>1000)seenSignals=new Set([event.eventId]);
  if(event.type==='call.offer'){
    if(isCaller()||!localStream||endingId)return;const queued=incomingIce.filter(item=>item.negotiation===event.negotiationId);const peer=await makePeer();negotiationId=event.negotiationId;await peer.setRemoteDescription({type:'offer',sdp:event.payload.sdp});if(pc!==peer||call?.id!==event.callId||endingId)return;for(const item of queued)await peer.addIceCandidate(item.payload);const answer=await peer.createAnswer();if(pc!==peer||call?.id!==event.callId||endingId)return;await peer.setLocalDescription(answer);if(pc!==peer||call?.id!==event.callId||endingId)return;await send('answer',{negotiationId,sdp:peer.localDescription.sdp});if(pc===peer&&call?.id===event.callId&&!endingId)await flushIce();
  }else if(event.type==='call.answer'){
    if(!isCaller()||!pc||event.negotiationId!==negotiationId)return;await pc.setRemoteDescription({type:'answer',sdp:event.payload.sdp});for(const item of incomingIce.splice(0))if(item.negotiation===negotiationId)await pc.addIceCandidate(item.payload);
  }else if(event.type==='call.ice'){
    if(pc?.remoteDescription&&event.negotiationId===negotiationId)await pc.addIceCandidate(event.payload);else if(incomingIce.length<100)incomingIce.push({negotiation:event.negotiationId,payload:event.payload});
  }else if(event.type==='call.media'&&event.negotiationId===negotiationId){peerCamera=event.payload.cameraEnabled;peerMic=event.payload.micEnabled;render();}
}
function sendMedia(){if(negotiationId&&active()&&own())send('media',{negotiationId,micEnabled:!!localStream?.getAudioTracks()[0]?.enabled,cameraEnabled:!!localStream?.getVideoTracks()[0]?.enabled}).catch(failure);}
async function action(op){if(busy||!active())return;const ending=op==='end'||op==='cancel';if(ending){endingId=call.id;stopMedia();}busy=true;render();try{if(ending&&!socket?.connected())apply(await api(`/api/calls/${call.id}/leave`,'POST',{clientId,eventId:crypto.randomUUID()}));else apply(await send(op));}catch(error){failure(error);try{await sync();}catch{}}finally{busy=false;render();}}
function leave(){
  if(active()&&own()&&csrf){fetch(`/api/calls/${call.id}/leave`,{method:'POST',headers:{[csrf.headerName]:csrf.token,'Content-Type':'application/json'},body:JSON.stringify({clientId,eventId:crypto.randomUUID()}),keepalive:true}).catch(()=>{});}
  stopMedia();
}
function expire(){closed=true;stopMedia();socket?.stop();clearInterval(poll);clearInterval(beat);clearInterval(clock);hideView();$('call-history').replaceChildren();home=null;call=null;location.replace('/?expired=1');}
$('call-launcher').addEventListener('click',show);$('call-close').addEventListener('click',()=>hideView());document.addEventListener('keydown',event=>{if(event.key==='Escape'&&!$('call-panel').hidden)hideView();});$('call-voice').addEventListener('click',()=>start('VOICE'));$('call-video').addEventListener('click',()=>start('VIDEO'));$('call-answer').addEventListener('click',answer);$('call-decline').addEventListener('click',()=>action('decline'));$('call-cancel').addEventListener('click',()=>action('cancel'));$('call-end').addEventListener('click',()=>action('end'));
$('call-stop-setup').addEventListener('click',()=>{setupSequence++;stopMedia();busy=false;failure('Device setup cancelled. You can retry or decline the incoming call.');render();});
$('call-resume').addEventListener('click',async()=>{if(busy)return;busy=true;render();try{await acquire(call.mode);apply(await send('resume'));if(isCaller())await offer(true);}catch(error){stopMedia();failure(error);}finally{busy=false;render();}});
$('call-mic').addEventListener('click',()=>{const track=localStream?.getAudioTracks()[0];if(track){track.enabled=!track.enabled;sendMedia();render();}});$('call-camera').addEventListener('click',()=>{const track=localStream?.getVideoTracks()[0];if(track){track.enabled=!track.enabled;sendMedia();render();}});$('call-play').addEventListener('click',()=>$('call-remote').play().then(()=>{$('call-play').hidden=true;}).catch(()=>failure('Audio playback is blocked. Check browser sound permissions.')));
$('call-refresh').addEventListener('click',()=>sync().catch(failure));$('call-sound').addEventListener('click',async()=>{try{if(notificationSounds.muted){notificationSounds.mute(false);await notificationSounds.enable();}else if(!notificationSounds.enabled)await notificationSounds.enable();else notificationSounds.mute(true);soundLabel();}catch{failure('Sound playback is unavailable. Visual alerts remain enabled.');}});
$('call-fullscreen').hidden=!document.fullscreenEnabled;
$('call-fullscreen').addEventListener('click',async()=>{try{if(document.fullscreenElement)await document.exitFullscreen();else await $('call-panel').requestFullscreen();}catch{failure('Browser full screen is unavailable. The full-page call remains open.');}});
document.addEventListener('fullscreenchange',()=>{$('call-fullscreen').textContent=document.fullscreenElement?'Exit full screen':'Full screen';});
$('call-history-toggle').addEventListener('click',async()=>{const box=$('call-history');box.hidden=!box.hidden;if(box.hidden)return;box.textContent='Loading calls…';try{const recent=await api('/api/calls/recent');box.replaceChildren();if(!recent.length)box.textContent='No calls yet.';for(const item of recent){const row=document.createElement('p');row.textContent=`${item.callerId===home.self.id?'Outgoing':'Incoming'} ${item.mode.toLowerCase()} · ${item.state.toLowerCase()} · ${new Intl.DateTimeFormat('en-GB',{dateStyle:'medium',timeStyle:'short',timeZone:'Asia/Yangon'}).format(new Date(item.createdAt))}`;box.append(row);}}catch(error){box.textContent=error.message;}});
for(const button of document.querySelectorAll('[data-call-mode]'))button.addEventListener('click',()=>start(button.dataset.callMode));document.getElementById('logout')?.addEventListener('click',leave,{capture:true});
window.addEventListener('pagehide',()=>{closed=true;leave();socket?.stop();clearInterval(poll);clearInterval(beat);clearInterval(clock);hideView();$('call-history').replaceChildren();$('call-partner').textContent='';home=null;call=null;});window.addEventListener('pageshow',event=>{if(event.persisted)location.reload();});
async function init(){
  try{if(!secure)throw new Error('Microphone/camera calls need localhost or trusted HTTPS in a supported browser.');clientHash=Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(clientId))),b=>b.toString(16).padStart(2,'0')).join('');home=await api('/api/home');await config();if(closed)return;socket=callSocket({onCsrf:value=>{csrf=value;},onExpired:expire,onStatus:text=>{if(!busy)message(text);render();},onEvent:event=>{eventChain=eventChain.then(()=>receive(event)).catch(failure);return eventChain;},onReady:async()=>{await sync();if(active()&&own()&&localStream){apply(await send('resume'));if(isCaller())offer(true).catch(failure);}render();}});
    poll=setInterval(()=>{if(!document.hidden||active())sync().catch(failure);},5000);beat=setInterval(()=>{if(active()&&own()&&socket?.connected())send('heartbeat').catch(()=>{});},5000);clock=setInterval(()=>{const seconds=active()&&call.connectedAt?Math.max(0,Math.floor((Date.now()+clockOffset-Date.parse(call.connectedAt))/1000)):0;$('call-duration').textContent=active()&&call.connectedAt?`${Math.floor(seconds/60)}:${String(seconds%60).padStart(2,'0')}`:'';},1000);render();
  }catch(error){failure(error);message('Calls unavailable. Refresh this page to retry.');}
}
init();
