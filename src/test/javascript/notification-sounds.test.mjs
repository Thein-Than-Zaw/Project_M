import test from 'node:test';
import assert from 'node:assert/strict';
import {createNotificationSounds} from '../../main/resources/static/notification-sounds.js';
function fixture(shared=new Map()){
 const notes=[],timers=new Set();let resumes=0;
 const storage={getItem:k=>shared.get(k)??null,setItem:(k,v)=>shared.set(k,v)};
 const context={state:'suspended',currentTime:1,destination:{},async resume(){this.state='running';resumes++;},async close(){this.state='closed';},createOscillator(){const n={frequency:{},connect(){},disconnect(){},start(){},stops:0,stop(){this.stops++;}};notes.push(n);return n;},createGain(){return {connect(){},disconnect(){},gain:{setValueAtTime(){},linearRampToValueAtTime(){},exponentialRampToValueAtTime(){}}};}};
 const engine=createNotificationSounds({storage,createContext:()=>context,setInterval:f=>{timers.add(f);return f;},clearInterval:f=>timers.delete(f)});
 return {engine,notes,timers,context,shared};
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
