import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';
test('call transport listens only on own queues and refreshes labels without consuming call acknowledgements',async()=>{
 const source=readFileSync(new URL('../../main/resources/static/call-socket.js',import.meta.url),'utf8').replace('export function ','function ');const frames=[],instances=[];let labels=0;
 class Socket{static OPEN=1;constructor(){this.readyState=1;instances.push(this);}send(frame){frames.push(frame);}close(){this.readyState=3;}}
 const c={WebSocket:Socket,location:{protocol:'http:',host:'localhost',hostname:'localhost'},window:{addEventListener(){},removeEventListener(){}},AbortSignal,TextEncoder,fetch:async path=>({ok:true,status:200,json:async()=>path==='/api/me'?{paired:true}:{headerName:'X-CSRF-TOKEN',token:'test'}}),setTimeout:()=>1,clearTimeout(){},setInterval:()=>1,clearInterval(){},ready(){},status(){},expired(){},labels(){labels++;}};
 vm.createContext(c);vm.runInContext(source+';globalThis.transport=callSocket({onEvent(){},onReady:ready,onStatus:status,onExpired:expired,onCsrf(){},onProfileUpdate:labels});',c);await new Promise(resolve=>setImmediate(resolve));const socket=instances[0];socket.onopen();socket.onmessage({data:'CONNECTED\n\n\0'});assert.ok(frames.some(frame=>frame.includes('destination:/user/queue/calls')));assert.ok(frames.some(frame=>frame.includes('destination:/user/queue/pairing')));socket.onmessage({data:'MESSAGE\n\n'+JSON.stringify({type:'partner.nickname.updated'})+'\0'});assert.equal(labels,1);
 const result=c.transport.send('media',{eventId:'action',callId:'call'});socket.onmessage({data:'MESSAGE\n\n'+JSON.stringify({type:'call.ack',eventId:'action',call:{id:'call'}})+'\0'});assert.equal((await result).id,'call');c.transport.stop();
});
test('call name refresh updates only actor-scoped labels without stopping media',async()=>{
 const source=readFileSync(new URL('../../main/resources/static/call-widget.js',import.meta.url),'utf8');let shown,rendered=0;const c={closed:false,home:{self:{id:'me'},members:[]},api:async path=>{assert.equal(path,'/api/space');return {members:[{id:'me',displayName:'Self'},{id:'partner',displayName:'Private nickname'}]};},avatar:(_,person)=>{shown=person;},$:()=>({}),render:()=>rendered++};vm.createContext(c);vm.runInContext(source.slice(source.indexOf('async function refreshCallNames('),source.indexOf("window.addEventListener('partner-nickname-updated'"))+';globalThis.refresh=refreshCallNames;',c);await c.refresh();assert.equal(shown.displayName,'Private nickname');assert.equal(c.home.self.id,'me');assert.equal(rendered,1);c.closed=true;await c.refresh();assert.equal(rendered,1);
});
