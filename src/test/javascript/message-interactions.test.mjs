import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';
const source=readFileSync(new URL('../../main/resources/static/chat.js',import.meta.url),'utf8');
const binding=source.slice(source.indexOf('function bindMessageInteractions('),source.indexOf('function hideHoverReactions('));
function fixture(){
 const handlers=new Map(),jobs=new Map(),calls={reactions:0,leave:0,context:[]};let sequence=0;
 const element={addEventListener:(name,handler)=>handlers.set(name,handler),contains:()=>false,getBoundingClientRect:()=>({left:30,bottom:90})};
 const timers={setTimeout:fn=>{jobs.set(++sequence,fn);return sequence;},clearTimeout:id=>jobs.delete(id)};
 const context={};vm.createContext(context);vm.runInContext(binding+';globalThis.bind=bindMessageInteractions;',context);context.bind(element,{onReactions:()=>calls.reactions++,onLeave:()=>calls.leave++,onContext:point=>calls.context.push(point)},timers);
 return {calls,jobs,emit(name,data={}){let prevented=false;handlers.get(name)?.({preventDefault:()=>{prevented=true;},...data});return prevented;},hold(){for(const [id,fn] of [...jobs]){jobs.delete(id);fn();}}};
}
test('mouse hover immediately offers reactions and right click opens context actions',()=>{
 const f=fixture();f.emit('pointerenter',{pointerType:'mouse'});assert.equal(f.calls.reactions,1);assert.equal(f.calls.context.length,0);assert.ok(f.emit('contextmenu',{clientX:250,clientY:180}));assert.equal(f.calls.context.length,1);assert.equal(f.calls.context[0].clientX,250);
});
test('keyboard context-menu key and Shift+F10 offer the same Reply/Delete actions',()=>{
 const f=fixture();assert.ok(f.emit('keydown',{key:'ContextMenu'}));assert.ok(f.emit('keydown',{key:'F10',shiftKey:true}));assert.equal(f.calls.context.length,2);assert.equal(f.calls.context[1].clientY,90);assert.equal(f.emit('keydown',{key:'F10',shiftKey:false}),false);
});
test('touch tap reveals reactions; long press opens actions without an extra tap reaction',()=>{
 const tap=fixture();tap.emit('pointerenter',{pointerType:'touch'});assert.equal(tap.calls.reactions,0);tap.emit('pointerdown',{pointerType:'touch',clientX:20,clientY:30});tap.emit('pointerup');assert.equal(tap.calls.reactions,1);assert.equal(tap.jobs.size,0);
 const hold=fixture();hold.emit('pointerdown',{pointerType:'touch',clientX:40,clientY:50});hold.hold();hold.emit('pointerup');assert.equal(hold.calls.context.length,1);assert.equal(hold.calls.reactions,0);
});
test('scrolling, cancelled touch and leaving the message cancel long press',()=>{
 for(const stop of ['move','cancel','leave']){const f=fixture();f.emit('pointerdown',{pointerType:'touch',clientX:0,clientY:0});if(stop==='move')f.emit('pointermove',{clientX:0,clientY:30});else f.emit(stop==='cancel'?'pointercancel':'pointerleave');f.hold();f.emit('pointerup');assert.equal(f.calls.context.length,0);assert.equal(f.calls.reactions,0);assert.equal(f.jobs.size,0);}
});
test('live deletion or hide cannot open stale context/reaction controls',()=>{
 const open=source.slice(source.indexOf('function openMessageActions('),source.indexOf('function renderMessageActions('));
 const hover=source.slice(source.indexOf('function showHoverReactions('),source.indexOf('function placeMessageMenu('));let hidden=0;
 const context={actionBusy:false,records:new Map(),reactionHideTimer:null,clearTimeout(){},hideHoverReactions(){hidden++;},$(){throw new Error('Stale context must not access old menu content');}};vm.createContext(context);vm.runInContext(open+hover+';globalThis.open=openMessageActions;globalThis.hover=showHoverReactions;',context);
 context.open('missing');context.hover('missing',{});assert.equal(hidden,1);context.records.set('deleted',{deletedAt:'2026-11-02T00:00:00Z'});context.hover('deleted',{});assert.equal(hidden,2);
});
