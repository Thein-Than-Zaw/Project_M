import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';
const source=readFileSync(new URL('../../main/resources/static/chat.js',import.meta.url),'utf8');
function extract(start,end){return source.slice(source.indexOf(start),source.indexOf(end));}
test('right click opens native top-layer menu, positions it and keeps Reply focus without scrolling',()=>{
 const calls=[];const panel={open:false,showModal(){calls.push('modal');this.open=true;}};const context={actionBusy:false,records:new Map([['id',{}]]),hideHoverReactions(){},renderMessageActions(){},placeMessageMenu(){calls.push('position');},$:id=>id==='#message-actions'?panel:id==='#message-action-error'?{}:{disabled:false,focus:options=>calls.push(options.preventScroll?'focus-without-scroll':'bad')}};
 vm.createContext(context);vm.runInContext(extract('function openMessageActions(','function renderMessageActions(')+';openMessageActions("id",{clientX:10,clientY:20});',context);assert.deepEqual(calls,['modal','position','focus-without-scroll']);
});
test('earlier history requests once under concurrent top scroll and preserves prepend render mode',async()=>{
 let resolve,requests=0,renderMode;const loading={textContent:'',classList:{add(){},remove(){}}},button={hidden:false};const context={epoch:1,olderBusy:false,hasOlder:true,started:true,records:new Map([['m',{sequence:50}]]),$:id=>id==='#older'?button:loading,get:async path=>{requests++;assert.equal(path,'/api/chat?before=50');return new Promise(r=>resolve=r);},merge(){},render:mode=>renderMode=mode};vm.createContext(context);vm.runInContext(extract('async function loadEarlier(',"$('#older').addEventListener")+';globalThis.load=loadEarlier;',context);
 const first=context.load();await context.load();assert.equal(requests,1);resolve({hasMore:false,items:[]});await first;assert.equal(renderMode,'older');assert.equal(loading.textContent,'');assert.equal(context.olderBusy,false);
});
test('history failure reveals explicit retry and clears loading instead of retry-looping',async()=>{
 const status={textContent:'',classList:{add(){},remove(){}}},button={hidden:true};const context={epoch:1,olderBusy:false,hasOlder:true,started:true,records:new Map([['m',{sequence:50}]]),$:id=>id==='#older'?button:status,get:async()=>{throw new Error('offline');}};vm.createContext(context);vm.runInContext(extract('async function loadEarlier(',"$('#older').addEventListener")+';globalThis.load=loadEarlier;',context);await context.load();assert.equal(button.hidden,false);assert.equal(context.olderBusy,false);assert.match(status.textContent,/Could not load/);
});
test('sticker auto-pagination needs visible browse, more results, no pending load and near-bottom',()=>{
 const dialog={open:true,scrollHeight:1000,scrollTop:700,clientHeight:300};let calls=0;const context={$:()=>dialog,selectedGift:null,giftHasMore:true,giftBusy:false,loadGifts:more=>{assert.equal(more,true);calls++;}};vm.createContext(context);vm.runInContext(extract('function autoLoadStickers(',"$('#gifts-dialog').addEventListener('scroll'")+';globalThis.load=autoLoadStickers;',context);context.load();assert.equal(calls,1);context.giftBusy=true;context.load();context.giftBusy=false;context.selectedGift={};context.load();context.selectedGift=null;context.giftHasMore=false;context.load();assert.equal(calls,1);
});
test('composer stays bounded in a short keyboard viewport while retaining scroll for long drafts',()=>{
 const input={style:{},scrollHeight:240};const context={$:()=>input,window:{visualViewport:{height:300}},innerHeight:800};vm.createContext(context);vm.runInContext(extract('function resizeComposer(','function resizeChatViewport(')+';resizeComposer();',context);assert.equal(input.style.height,'66px');assert.equal(input.style.overflowY,'auto');
});
test('activity labels distinguish unknown, active and expired partner activity using server time',()=>{
 const text=readFileSync(new URL('../../main/resources/static/profile-ui.js',import.meta.url),'utf8');const fn=text.slice(text.indexOf('export function activityText('),text.indexOf('let stopped=')).replace('export ','');const c={};vm.createContext(c);vm.runInContext(fn+';globalThis.label=activityText;',c);assert.equal(c.label({active:true}),'Active');assert.equal(c.label({active:false}),'Offline');assert.equal(c.label({lastActiveAt:'2026-11-01T18:00:00Z',serverTime:'2026-11-01T18:02:15Z'}),'Active 2 min ago');
});
test('default browser timers do not use a plain object as the native Window receiver',()=>{
 const handlers=new Map();const context={setTimeout:function(){'use strict';assert.equal(this,undefined);return 1;},clearTimeout:function(){'use strict';assert.equal(this,undefined);},element:{addEventListener:(key,fn)=>handlers.set(key,fn),contains:()=>false}};
 vm.createContext(context);vm.runInContext(extract('function bindMessageInteractions(','function hideHoverReactions(')+';bindMessageInteractions(element,{onReactions(){},onLeave(){},onContext(){}});',context);
 handlers.get('contextmenu')({preventDefault(){},clientX:1,clientY:2});handlers.get('pointerdown')({pointerType:'touch',clientX:1,clientY:2});handlers.get('pointercancel')();
});
