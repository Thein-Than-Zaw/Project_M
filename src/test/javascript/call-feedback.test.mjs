import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';
const source=readFileSync(new URL('../../main/resources/static/call-widget.js',import.meta.url),'utf8');
test('call outcome sound follows current live participant transitions, never historical or duplicate terminal events',()=>{
 const played=[];const c={live:new Set(['RINGING','CONNECTING','ACTIVE','RECONNECTING']),closed:false,isCaller:v=>v.caller,own:v=>v.selected,notificationSounds:{callEnded:(...args)=>played.push(args)}};
 vm.createContext(c);vm.runInContext(source.slice(source.indexOf('function playCallOutcome('),source.indexOf('function renderPeerMute('))+';globalThis.outcome=playCallOutcome;',c);
 const end={id:'call',state:'ENDED'};c.outcome(undefined,end);c.outcome({id:'call',state:'ENDED',selected:true},end);c.outcome({id:'other',state:'ACTIVE',selected:true},end);c.outcome({id:'call',state:'ACTIVE',selected:false},end);assert.equal(played.length,0);
 for(const state of ['ENDED','MISSED','CANCELLED','DECLINED','FAILED'])c.outcome({id:'call',state:'RINGING',selected:true,caller:true},{id:'call',state});assert.equal(played.length,5);
 c.outcome({id:'call',state:'RINGING',selected:false,caller:false},{id:'call',state:'MISSED'});assert.equal(played.length,6);c.closed=true;c.outcome({id:'call',state:'ACTIVE',selected:true},end);assert.equal(played.length,6);
});
test('partner mic badge appears over live video/voice and clears on unmute/end/unselected tab',()=>{
 const badge={hidden:true};const c={$:()=>badge,active:()=>true,own:()=>true,call:{state:'ACTIVE'},peerMic:false};vm.createContext(c);vm.runInContext(source.slice(source.indexOf('function renderPeerMute('),source.indexOf('function ring('))+';globalThis.draw=renderPeerMute;',c);
 c.draw();assert.equal(badge.hidden,false);assert.match(badge.title,/microphone is muted/);c.peerMic=true;c.draw();assert.equal(badge.hidden,true);c.peerMic=false;c.own=()=>false;c.draw();assert.equal(badge.hidden,true);c.own=()=>true;c.call.state='RINGING';c.draw();assert.equal(badge.hidden,true);c.call.state='ACTIVE';c.active=()=>false;c.draw();assert.equal(badge.hidden,true);
});
test('remote media signal validates call/negotiation before rendering partner mute state',async()=>{
 let renders=0;const c={call:{id:'call'},active:()=>true,own:()=>true,seenSignals:new Set(),negotiationId:'current',peerCamera:true,peerMic:true,render:()=>renders++,apply(){}};
 vm.createContext(c);vm.runInContext(source.slice(source.indexOf('async function receive('),source.indexOf('function sendMedia('))+';globalThis.receiveSignal=receive;',c);
 const event=(id,callId,negotiation,mic)=>({eventId:id,callId,negotiationId:negotiation,type:'call.media',payload:{micEnabled:mic,cameraEnabled:true}});
 await c.receiveSignal(event('other','other','current',false));await c.receiveSignal(event('old','call','old',false));assert.equal(c.peerMic,true);assert.equal(renders,0);
 await c.receiveSignal(event('mute','call','current',false));assert.equal(c.peerMic,false);assert.equal(renders,1);await c.receiveSignal(event('mute','call','current',false));assert.equal(renders,1);await c.receiveSignal(event('unmute','call','current',true));assert.equal(c.peerMic,true);assert.equal(renders,2);
});
test('lost terminal socket event is recovered through scoped final-call metadata',async()=>{
 const applied=[],requests=[];const c={home:{},closed:false,call:{id:'call-123'},active:()=>true,api:async path=>{requests.push(path);return path.endsWith('/current')?{call:null}:{id:'call-123',state:'MISSED'};},apply:view=>applied.push(view)};
 vm.createContext(c);vm.runInContext(source.slice(source.indexOf('async function sync('),source.indexOf('async function acquire('))+';globalThis.syncCall=sync;',c);await c.syncCall();assert.deepEqual(requests,['/api/calls/current','/api/calls/call-123']);assert.equal(applied[0].state,'MISSED');
});
