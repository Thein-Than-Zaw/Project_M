import test from 'node:test';
import assert from 'node:assert/strict';
import {createNotificationSounds} from '../../main/resources/static/notification-sounds.js';
function fixture(shared=new Map()){
 const notes=[],gains=[],timers=new Set();let resumes=0;
 const storage={getItem:k=>shared.get(k)??null,setItem:(k,v)=>shared.set(k,v)};
 const context={state:'suspended',currentTime:1,destination:{},async resume(){this.state='running';resumes++;},async close(){this.state='closed';},createOscillator(){const n={frequency:{},connect(){},disconnect(){},start(){},stops:0,stop(){this.stops++;}};notes.push(n);return n;},createGain(){const node={connect(){},disconnect(){},values:[],gain:{setValueAtTime(v){node.values.push(v);},linearRampToValueAtTime(v){node.values.push(v);},exponentialRampToValueAtTime(v){node.values.push(v);}}};gains.push(node);return node;}};
 const engine=createNotificationSounds({storage,createContext:()=>context,setInterval:f=>{timers.add(f);return f;},clearInterval:f=>timers.delete(f)});
 return {engine,notes,gains,timers,context,shared};
}
test('sound requires gesture; muted preference persists without replaying old messages',async()=>{
 const f=fixture();f.engine.message('before-gesture');assert.equal(f.notes.length,0);await f.engine.enable();f.engine.message('before-gesture');assert.equal(f.notes.length,0);
 f.engine.mute(true);f.engine.message('muted');assert.equal(f.notes.length,0);f.engine.mute(false);f.engine.message('muted');assert.equal(f.notes.length,0);f.engine.message('fresh');assert.equal(f.notes.length,2);
 assert.equal(f.shared.get('couple.sounds.muted'),'false');
});
test('mute immediately stops scheduled ringtone notes; visual call remains independent',async()=>{
 const f=fixture();await f.engine.enable();f.engine.ring('call-1');assert.equal(f.notes.length,3);assert.equal(f.timers.size,1);f.engine.ring('call-1');assert.equal(f.notes.length,3);
 f.engine.mute(true);assert.ok(f.notes.every(n=>n.stops===2));for(const repeat of f.timers)repeat();assert.equal(f.notes.length,3);f.engine.ring(null);assert.equal(f.timers.size,0);
});
test('message IDs deduplicate across sockets/tabs, and calls take precedence over chimes',async()=>{
 const shared=new Map(),a=fixture(shared),b=fixture(shared);await a.engine.enable();await b.engine.enable();a.engine.message('m1');a.engine.message('m1');b.engine.message('m1');assert.equal(a.notes.length,2);assert.equal(b.notes.length,0);
 a.engine.ring('c1');a.engine.message('m2');assert.equal(a.notes.length,5);a.engine.ring(null);a.engine.message('m3');assert.equal(a.notes.length,7);
});
test('saved mute and cross-tab changes silence current ringtone and dispose closes context',async()=>{
 const f=fixture(new Map([['couple.sounds.muted','true']]));await f.engine.enable();f.engine.ring('c1');assert.equal(f.notes.length,0);f.engine.mute(false);for(const repeat of f.timers)repeat();assert.equal(f.notes.length,3);f.engine.mute(true,false);assert.ok(f.notes.every(n=>n.stops===2));f.engine.dispose();assert.equal(f.timers.size,0);assert.equal(f.context.state,'closed');
});

test('volume defaults louder, persists, clamps and updates currently scheduled sound gain',async()=>{
 const f=fixture();assert.equal(f.engine.volume,80);await f.engine.enable();f.engine.message('volume');assert.equal(f.gains[0].values[0],.8);assert.ok(f.gains[1].values.includes(.22));
 f.engine.setVolume(100);assert.equal(f.gains[0].values.at(-1),1);assert.equal(f.shared.get('couple.sounds.volume'),'100');f.engine.setVolume(300);assert.equal(f.engine.volume,100);f.engine.setVolume(-5);assert.equal(f.engine.volume,0);f.engine.message('zero');assert.equal(f.notes.length,2);
 f.engine.setVolume(35,false);assert.equal(f.engine.volume,35);assert.equal(f.shared.get('couple.sounds.volume'),'0');f.engine.setVolume(NaN);assert.equal(f.engine.volume,35);
});
test('sent acknowledgement chime deduplicates, respects mute and does not interrupt ringtone',async()=>{
 const f=fixture();await f.engine.enable();f.engine.sent('confirmed-1');f.engine.sent('confirmed-1');assert.equal(f.notes.length,2);f.engine.mute(true);f.engine.sent('muted');assert.equal(f.notes.length,2);f.engine.mute(false);f.engine.sent('muted');assert.equal(f.notes.length,2);
 f.engine.ring('call');f.engine.sent('during-call');assert.equal(f.notes.length,5);await f.engine.enable();assert.equal(f.notes.length,5);f.engine.ring(null);f.engine.sent('confirmed-2');assert.equal(f.notes.length,7);
});
test('preview honors mute/zero volume and never consumes a real message event',async()=>{
 const f=fixture();await f.engine.enable();f.engine.preview();assert.equal(f.notes.length,2);f.engine.mute(true);f.engine.preview();assert.equal(f.notes.length,2);f.engine.mute(false);f.engine.setVolume(0);f.engine.preview();assert.equal(f.notes.length,2);f.engine.setVolume(50);f.engine.message('real');assert.equal(f.notes.length,4);
});
test('outgoing ringback repeats once per call and stops when answered/cancelled',async()=>{
 const f=fixture();await f.engine.enable();f.engine.ringback('outgoing');assert.equal(f.notes.length,4);assert.deepEqual(f.notes.map(n=>n.frequency.value),[440,480,440,480]);assert.equal(f.timers.size,1);f.engine.ringback('outgoing');assert.equal(f.notes.length,4);for(const timer of f.timers)timer();assert.equal(f.notes.length,8);f.engine.ring(null);assert.equal(f.timers.size,0);assert.ok(f.notes.every(n=>n.stops===2));
});
test('ended/cancelled tone and unanswered/failed tone differ and deduplicate across tabs',async()=>{
 const shared=new Map(),a=fixture(shared),b=fixture(shared);await a.engine.enable();await b.engine.enable();
 a.engine.callEnded('ended','ENDED');assert.deepEqual(a.notes.map(n=>n.frequency.value),[659.25,440]);a.engine.callEnded('ended','ENDED');b.engine.callEnded('ended','ENDED');assert.equal(a.notes.length,2);assert.equal(b.notes.length,0);
 a.engine.callEnded('missed','MISSED');assert.deepEqual(a.notes.slice(2).map(n=>n.frequency.value),[330,330,262]);a.engine.callEnded('declined','DECLINED');assert.equal(a.notes.length,8);
});
test('outgoing/end sounds respect mute/zero volume and do not replay old terminal events',async()=>{
 const f=fixture();await f.engine.enable();f.engine.mute(true);f.engine.ringback('ring');assert.equal(f.notes.length,0);f.engine.ring(null);f.engine.callEnded('muted','CANCELLED');f.engine.mute(false);f.engine.callEnded('muted','CANCELLED');assert.equal(f.notes.length,0);
 f.engine.setVolume(0);f.engine.ringback('zero');for(const timer of f.timers)timer();assert.equal(f.notes.length,0);f.engine.ring(null);f.engine.callEnded('zero','FAILED');f.engine.setVolume(60);f.engine.callEnded('zero','FAILED');assert.equal(f.notes.length,0);f.engine.callEnded('fresh','ENDED');assert.equal(f.notes.length,2);assert.equal(f.gains[0].values.at(-1),.6);
});
test('terminal sound does not interrupt a newer incoming call and dispose stops ringback timer',async()=>{
 const f=fixture();await f.engine.enable();f.engine.ring('incoming');f.engine.callEnded('older','MISSED');assert.equal(f.notes.length,3);assert.equal(f.timers.size,1);f.engine.ringback('outgoing');assert.equal(f.timers.size,1);f.engine.dispose();assert.equal(f.timers.size,0);assert.equal(f.context.state,'closed');
});
test('gesture unlock starts pending outgoing ringback without replaying ended history',async()=>{
 const f=fixture();f.engine.ringback('pending');assert.equal(f.notes.length,0);await f.engine.enable();assert.equal(f.notes.length,4);await f.engine.enable();assert.equal(f.notes.length,4);f.engine.ring(null);f.engine.callEnded('once','ENDED');await f.engine.enable();assert.equal(f.notes.length,6);
});
