import test from 'node:test';
import assert from 'node:assert/strict';
import {createVoiceCapture,validateChatFile,MEDIA_LIMITS,mountChatMedia} from '../../main/resources/static/chat-media.js';

function fixture(permission){
 let tick,time=0,stopped=0,recorder;const states=[],errors=[];
 const stream={getTracks:()=>[{stop(){stopped++;}}]};
 class Recorder{static isTypeSupported(type){return type==='audio/webm;codecs=opus';}constructor(){recorder=this;this.state='inactive';this.mimeType='audio/webm';}start(){this.state='recording';}stop(){this.state='inactive';}data(size=3){this.ondataavailable?.({data:new Blob([new Uint8Array(size)],{type:this.mimeType})});}finish(){this.onstop?.();}}
 const capture=createVoiceCapture({getUserMedia:permission||(()=>Promise.resolve(stream)),Recorder,now:()=>time,setInterval:fn=>(tick=fn,1),clearInterval:()=>{tick=null;},onState:value=>states.push(value),onError:text=>errors.push(text)});
 return {capture,stream,states,errors,get recorder(){return recorder;},get stopped(){return stopped;},tick(seconds){time=seconds*1000;tick?.();}};
}
test('cancel during recording releases microphone immediately and never publishes a preview',async()=>{
 const f=fixture();await f.capture.start();f.recorder.data();const recorder=f.recorder;f.capture.cancel();assert.equal(f.stopped,1);assert.equal(recorder.state,'inactive');recorder.finish();assert.equal(f.states.at(-1).state,'idle');assert.equal(f.states.some(s=>s.state==='preview'),false);
});
test('cancel while microphone permission is pending stops the late stream without recording',async()=>{
 let grant;const f=fixture(()=>new Promise(resolve=>grant=resolve));const pending=f.capture.start();f.capture.cancel();grant(f.stream);await pending;assert.equal(f.stopped,1);assert.equal(f.recorder,undefined);assert.equal(f.states.at(-1).state,'idle');
});
test('stop releases microphone before delayed recorder completion and creates a local audio preview',async()=>{
 const f=fixture();await f.capture.start();f.recorder.data(5);f.tick(2);f.capture.stop();assert.equal(f.stopped,1);assert.equal(f.states.some(s=>s.state==='preview'),false);f.recorder.finish();const preview=f.states.at(-1);assert.equal(preview.state,'preview');assert.equal(preview.blob.size,5);assert.equal(preview.seconds,2);
});
test('five minute limit stops capture and over-size recording cancels instead of sending truncated audio',async()=>{
 const f=fixture();await f.capture.start();f.recorder.data();f.tick(300);assert.equal(f.recorder.state,'inactive');assert.equal(f.stopped,1);f.recorder.finish();assert.equal(f.states.at(-1).state,'preview');
 const big=fixture();await big.capture.start();big.recorder.data(MEDIA_LIMITS.voice+1);assert.equal(big.stopped,1);assert.equal(big.states.at(-1).state,'idle');assert.match(big.errors[0],/10 MB/);
});
test('permission denial gives actionable feedback and leaves recording idle',async()=>{
 const f=fixture(async()=>{throw Object.assign(new Error(),{name:'NotAllowedError'});});await f.capture.start();assert.equal(f.states.at(-1).state,'idle');assert.match(f.errors[0],/permission/);
});
test('client file limits reject unsupported and oversized files before upload',()=>{
 for(const [name,limit,kind] of [['a.PNG',10000000,'IMAGE'],['clip.mp4',100000000,'VIDEO'],['doc.pdf',20000000,'DOCUMENT']]){assert.equal(validateChatFile({name,size:limit}),kind);assert.throws(()=>validateChatFile({name,size:limit+1}));}
 assert.throws(()=>validateChatFile({name:'script.html',size:2}));assert.throws(()=>validateChatFile({name:'empty.png',size:0}));
});

function mounted(t){
 const previous=new Map();function replace(name,value){previous.set(name,Object.getOwnPropertyDescriptor(globalThis,name));Object.defineProperty(globalThis,name,{configurable:true,writable:true,value});}
 const nodes=new Map();function node(){return {children:[],listeners:{},value:'',files:[],hidden:false,addEventListener(name,fn){this.listeners[name]=fn;},append(...items){this.children.push(...items);},replaceChildren(...items){this.children=[...items];},querySelectorAll(){return this.children.filter(n=>n.tag==='audio'||n.tag==='video');},pause(){},load(){},removeAttribute(){}};}
 const element=id=>{if(!nodes.has(id))nodes.set(id,node());return nodes.get(id);};const requests=[],queued=[];
 class XHR{constructor(){requests.push(this);}open(){}setRequestHeader(){}send(){}abort(){this.aborted=true;this.onabort?.();}succeed(){this.status=201;this.response={id:'asset-id',url:'/api/chat/assets/id'};this.onload();}}
 replace('document',{getElementById:element,createElement:tag=>Object.assign(node(),{tag}),body:{classList:{contains:()=>false}}});replace('window',{isSecureContext:true,MediaRecorder:class{}});replace('navigator',{mediaDevices:{}});replace('MutationObserver',class{observe(){}disconnect(){}});replace('XMLHttpRequest',XHR);replace('fetch',async()=>({ok:true,json:async()=>({state:'CANCELLED'})}));
 const api=mountChatMedia({csrf:async()=>({headerName:'X-CSRF-TOKEN',token:'synthetic'}),queue:item=>queued.push(item),onError:error=>{throw new Error(error);},onControls(){}});
 t.after(()=>{api.dispose();for(const [name,descriptor] of previous)if(descriptor)Object.defineProperty(globalThis,name,descriptor);else delete globalThis[name];});
 return {api,element,requests,queued,select(){element('chat-file').files=[new File(['synthetic'],'photo.png',{type:'image/png'})];element('chat-file').listeners.change();}};
}
test('Send moves preview straight into a pending bubble and frees the composer before upload',async t=>{
 const f=mounted(t);f.select();f.api.send('caption',null);assert.equal(f.queued.length,1);assert.equal(f.queued[0].type,'FILE');assert.match(f.queued[0].localPreview,/^blob:/);assert.equal(f.api.hasDraft(),false);assert.equal(f.element('chat-media-draft').hidden,true);assert.equal(f.requests.length,0);
 const item={...f.queued[0],deadlineAt:Date.now()+300000};const uploading=f.api.upload(item);await Promise.resolve();assert.equal(f.requests.length,1);assert.ok(f.requests[0].timeout>299000);f.requests[0].succeed();assert.equal((await uploading).id,'asset-id');
});
test('Cancel sending immediately aborts upload and a separate queued media item remains usable',async t=>{
 const f=mounted(t);f.select();f.api.send('',null);f.select();f.api.send('',null);const first={...f.queued[0],deadlineAt:Date.now()+300000},second={...f.queued[1],deadlineAt:Date.now()+300000};const uploading=f.api.upload(first);const rejected=assert.rejects(uploading,{name:'AbortError'});await Promise.resolve();const cancelled=f.api.cancelIntent(first.clientMessageId);assert.equal(f.requests[0].aborted,true);await cancelled;await rejected;const next=f.api.upload(second);await Promise.resolve();f.requests[1].succeed();assert.equal((await next).id,'asset-id');
});
