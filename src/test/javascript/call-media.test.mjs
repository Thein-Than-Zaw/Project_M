import test from 'node:test';
import assert from 'node:assert/strict';
import {requestCallMedia} from '../../main/resources/static/call-media.js';

const tick=()=>new Promise(resolve=>setImmediate(resolve));
const stream=()=>{const tracks=[{stops:0,stop(){this.stops++;}},{stops:0,stop(){this.stops++;}}];return {getTracks:()=>tracks};};
test('video requests real audio/video and voice requests no camera',async()=>{
 for(const mode of ['VOICE','VIDEO']){let constraints;const media=stream();assert.equal(await requestCallMedia({mode,mediaDevices:{getUserMedia:value=>{constraints=value;return Promise.resolve(media);}}}),media);assert.ok(constraints.audio);assert.equal(!!constraints.video,mode==='VIDEO');assert.ok(media.getTracks().every(t=>t.stops===0));}
});
test('pending permission times out, and a late camera stream is stopped',async()=>{
 let complete;const request=requestCallMedia({mode:'VIDEO',timeoutMs:15,mediaDevices:{getUserMedia:()=>new Promise(resolve=>{complete=resolve;})}});
 await assert.rejects(request,e=>e.code==='MEDIA_TIMEOUT');const media=stream();complete(media);await tick();assert.ok(media.getTracks().every(t=>t.stops===1));
});
test('cancel immediately rejects pending setup and stops all late tracks',async()=>{
 let complete;const controller=new AbortController();const request=requestCallMedia({mode:'VIDEO',signal:controller.signal,mediaDevices:{getUserMedia:()=>new Promise(resolve=>{complete=resolve;})}});controller.abort();await assert.rejects(request,e=>e.code==='MEDIA_CANCELLED');const media=stream();complete(media);await tick();assert.ok(media.getTracks().every(t=>t.stops===1));
});
test('already cancelled setup never opens a device',async()=>{
 const controller=new AbortController();controller.abort();let opened=false;await assert.rejects(requestCallMedia({signal:controller.signal,mediaDevices:{getUserMedia:()=>{opened=true;}}}),e=>e.code==='MEDIA_CANCELLED');assert.equal(opened,false);
});
test('busy hardware gives actionable feedback without retrying as voice',async()=>{
 let calls=0;await assert.rejects(requestCallMedia({mode:'VIDEO',mediaDevices:{getUserMedia:()=>{calls++;return Promise.reject({name:'NotReadableError'});}}}),e=>e.code==='NotReadableError'&&e.message.includes('Another browser')&&e.message.includes('second camera'));assert.equal(calls,1);
});
test('permission refusal preserves its reason and does not claim hardware failure',async()=>{
 await assert.rejects(requestCallMedia({mode:'VIDEO',mediaDevices:{getUserMedia:()=>Promise.reject({name:'NotAllowedError'})}}),e=>e.code==='NotAllowedError'&&e.message.includes('address bar'));
});
test('slow setup signals progress and late rejection after timeout is handled',async()=>{
 let refuse,waiting=0;const request=requestCallMedia({timeoutMs:20,waitingMs:1,onWaiting:()=>waiting++,mediaDevices:{getUserMedia:()=>new Promise((resolve,reject)=>{refuse=reject;})}});await assert.rejects(request,e=>e.code==='MEDIA_TIMEOUT');assert.equal(waiting,1);refuse({name:'NotReadableError'});await tick();
});
