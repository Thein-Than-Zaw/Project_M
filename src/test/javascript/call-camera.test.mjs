import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';
import {canSwitchCallCamera,switchCallCamera} from '../../main/resources/static/call-media.js';
function track(kind='video',facingMode='user'){
 return {kind,enabled:true,readyState:'live',stops:0,stop(){this.stops++;this.readyState='ended';},getSettings:()=>({facingMode,deviceId:facingMode+'-id'})};
}
function stream(...tracks){return {tracks,getTracks(){return [...this.tracks];},getVideoTracks(){return this.tracks.filter(t=>t.kind==='video');},removeTrack(t){this.tracks=this.tracks.filter(x=>x!==t);},addTrack(t){this.tracks.push(t);}};}
function fixture(){const old=track(),audio=track('audio'),next=track('video','environment'),media=stream(old,audio),requests=[],replacements=[];
 const f={old,audio,next,media,requests,replacements,mediaDevices:{getUserMedia:async c=>{requests.push(c);return stream(next);}},sender:{replaceTrack:async t=>replacements.push(t)}};
 f.change=options=>switchCallCamera({mediaDevices:f.mediaDevices,stream:media,sender:f.sender,facingMode:'environment',...options});return f;
}
test('front/back switch replaces only video and disposes the old camera after replacement',async()=>{
 const f=fixture();const result=await f.change();assert.equal(result,f.next);assert.equal(f.requests[0].audio,false);assert.equal(f.requests[0].video.facingMode.exact,'environment');assert.equal(f.replacements[0],f.next);assert.equal(f.media.getVideoTracks()[0],f.next);assert.equal(f.old.stops,1);assert.equal(f.audio.stops,0);assert.ok(f.audio.enabled);
 f.mediaDevices.getUserMedia=async c=>{f.requests.push(c);return stream(track());};await f.change({facingMode:'user'});assert.equal(f.requests.at(-1).video.facingMode.exact,'user');assert.equal(f.next.stops,1);assert.equal(f.audio.stops,0);
});
test('switching while camera is off preserves disabled video and muted microphone',async()=>{
 const f=fixture();f.old.enabled=false;f.audio.enabled=false;await f.change();assert.equal(f.next.enabled,false);assert.equal(f.audio.enabled,false);assert.equal(f.audio.stops,0);
});
test('exclusive mobile camera releases video only and retries the requested rear camera',async()=>{
 const f=fixture();let attempts=0;f.mediaDevices.getUserMedia=async c=>{f.requests.push(c);if(++attempts===1)throw {name:'NotReadableError'};assert.equal(f.old.readyState,'ended');return stream(f.next);};await f.change();assert.equal(attempts,2);assert.equal(f.audio.stops,0);assert.equal(f.replacements[0],f.next);
});
test('a busy-device fallback restores the exact original camera after acquisition failure',async()=>{
 const f=fixture(),restored=track();let attempts=0;f.mediaDevices.getUserMedia=async c=>{f.requests.push(c);if(++attempts===1)throw {name:'NotReadableError'};if(attempts===2)throw {name:'NotFoundError'};return stream(restored);};
 await assert.rejects(f.change(),/previous camera is kept/);assert.equal(f.requests[2].video.deviceId.exact,'user-id');assert.equal(f.media.getVideoTracks()[0],restored);assert.equal(f.audio.stops,0);assert.equal(f.replacements.at(-1),restored);
});
test('permission denial or unsupported facing direction keeps the current camera without releasing it',async()=>{
 for(const name of ['NotAllowedError','OverconstrainedError']){const f=fixture();f.mediaDevices.getUserMedia=async()=>{throw {name};};await assert.rejects(f.change(),/previous camera is kept/);assert.equal(f.old.stops,0);assert.equal(f.media.getVideoTracks()[0],f.old);assert.equal(f.audio.stops,0);}
});
test('replaceTrack rejection stops candidate and preserves existing video',async()=>{
 const f=fixture();f.sender.replaceTrack=async t=>{if(t===f.next)throw new Error('negotiation required');f.replacements.push(t);};await assert.rejects(f.change(),/previous camera is kept/);assert.equal(f.old.stops,0);assert.equal(f.next.stops,1);assert.equal(f.replacements.at(-1),f.old);assert.equal(f.media.getVideoTracks()[0],f.old);
});
test('cancellation while camera permission is pending disposes late hardware and never replaces sender',async()=>{
 const f=fixture(),controller=new AbortController();let finish;f.mediaDevices.getUserMedia=()=>new Promise(resolve=>{finish=resolve;});const request=f.change({signal:controller.signal});controller.abort();await assert.rejects(request,e=>e.code==='MEDIA_CANCELLED');finish(stream(f.next));await new Promise(resolve=>setImmediate(resolve));assert.equal(f.next.stops,1);assert.equal(f.replacements.length,0);assert.equal(f.audio.stops,0);
});
test('call/peer changes while replacement resolves cannot attach a stale camera stream',async()=>{
 const f=fixture();let current=true;f.sender.replaceTrack=async()=>{current=false;};await assert.rejects(f.change({isCurrent:()=>current}),e=>e.code==='MEDIA_CANCELLED');assert.equal(f.next.stops,1);assert.equal(f.media.getVideoTracks()[0],f.old);
});
test('camera timeout releases any late returned video and keeps old camera live',async()=>{
 const f=fixture();let finish;f.mediaDevices.getUserMedia=()=>new Promise(resolve=>{finish=resolve;});await assert.rejects(f.change({timeoutMs:10}),/previous camera is kept/);finish(stream(f.next));await new Promise(resolve=>setImmediate(resolve));assert.equal(f.next.stops,1);assert.equal(f.old.stops,0);
});
test('failed camera restoration leaves microphone connected and gives a camera retry action',async()=>{
 const f=fixture();f.mediaDevices.getUserMedia=async()=>{throw {name:'NotReadableError'};};await assert.rejects(f.change(),/Audio is still connected.*camera button/);assert.equal(f.audio.stops,0);assert.equal(f.old.readyState,'ended');
});
test('single camera and failed device enumeration hide switch; multiple cameras/directions enable it',async()=>{
 assert.equal(await canSwitchCallCamera({enumerateDevices:async()=>[{kind:'videoinput'}]},track()),false);
 assert.equal(await canSwitchCallCamera({enumerateDevices:async()=>[{kind:'videoinput'},{kind:'videoinput'}]},track()),true);
 assert.equal(await canSwitchCallCamera({}, {getCapabilities:()=>({facingMode:['user','environment']})}),true);
 assert.equal(await canSwitchCallCamera({enumerateDevices:async()=>{throw new Error();}},track()),false);
});
test('ending during a pending sender replacement immediately releases candidate camera',async()=>{
 const f=fixture(),controller=new AbortController();let complete;f.sender.replaceTrack=()=>new Promise(resolve=>{complete=resolve;});const changing=f.change({signal:controller.signal});while(!complete)await new Promise(resolve=>setImmediate(resolve));controller.abort();assert.equal(f.next.readyState,'ended');complete();await assert.rejects(changing,e=>e.code==='MEDIA_CANCELLED');assert.equal(f.audio.stops,0);
});
const widget=readFileSync(new URL('../../main/resources/static/call-widget.js',import.meta.url),'utf8');
function widgetFixture(){
 const nodes=new Map(),streamObject=stream(track(),track('audio')),sender={track:streamObject.getVideoTracks()[0]},peer={getSenders:()=>[sender]};let operations=0,finish;
 const c={cameraController:null,busy:false,closed:false,call:{id:'same-call',mode:'VIDEO',state:'ACTIVE'},endingId:null,pc:peer,localStream:streamObject,mediaEpoch:1,cameraFacing:'user',AbortController,navigator:{mediaDevices:{}},active:()=>true,own:()=>true,closeCallReactions(){},render(){},refreshCameras(){},sendMedia(){},errors:[],failure:e=>c.errors.push(e.message),$:id=>{if(!nodes.has(id))nodes.set(id,{textContent:'',srcObject:null,play:()=>Promise.resolve()});return nodes.get(id);},switchCallCamera:()=>{operations++;return new Promise(resolve=>{finish=resolve;});}};
 vm.createContext(c);vm.runInContext(widget.slice(widget.indexOf('async function changeCamera('),widget.indexOf("$('call-switch-camera').addEventListener"))+';globalThis.change=changeCamera;',c);
 return {c,nodes,get operations(){return operations;},finish:t=>finish(t)};
}
test('widget serializes repeated switch taps and updates rear preview after confirmed replacement',async()=>{
 const f=widgetFixture(),one=f.c.change('environment');await f.c.change('environment');assert.equal(f.operations,1);const rear=track('video','environment');f.c.localStream.removeTrack(f.c.localStream.getVideoTracks()[0]);f.c.localStream.addTrack(rear);f.finish(rear);await one;assert.equal(f.c.cameraFacing,'environment');assert.equal(f.c.cameraController,null);assert.equal(f.nodes.get('call-local').srcObject,f.c.localStream);
});
test('widget ignores stale replacement result after call ownership/peer changes',async()=>{
 const f=widgetFixture(),changing=f.c.change('environment');f.c.pc=null;f.c.cameraController.abort();f.finish(track('video','environment'));await changing;assert.equal(f.nodes.has('call-local'),false);assert.equal(f.c.errors.length,0);assert.equal(f.c.cameraFacing,'user');
});
