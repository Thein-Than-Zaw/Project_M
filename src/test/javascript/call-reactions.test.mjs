import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';
import {createCallReactionEffects} from '../../main/resources/static/call-reactions.js';
function fixture(reduced=false){
 const timers=new Map(),children=[],announcement={textContent:''};let next=0;
 const layer={ownerDocument:{createElement(){return {style:{setProperty(){}},setAttribute(){},addEventListener(name,fn){this[name]=fn;},remove(){const index=children.indexOf(this);if(index>=0)children.splice(index,1);}};}},append(node){children.push(node);}};
 const effects=createCallReactionEffects({layer,announcement,reducedMotion:()=>reduced,setTimer:fn=>{timers.set(++next,fn);return next;},clearTimer:id=>timers.delete(id)});
 return {effects,timers,children,announcement};
}
test('reaction is a brief bounded visual burst with a screen-reader announcement',()=>{
 const f=fixture();assert.equal(f.effects.show('❤️','Your person'),true);assert.equal(f.children.length,3);assert.equal(f.announcement.textContent,'Your person reacted ❤️');for(const node of f.children)assert.equal(node.textContent,'❤️');for(const fn of [...f.timers.values()])fn();assert.equal(f.children.length,0);assert.equal(f.timers.size,0);
});
test('overlapping bursts remain bounded and call end clears nodes and delayed timers',()=>{
 const f=fixture();for(let i=0;i<20;i++)f.effects.show('🎉');assert.equal(f.children.length,18);assert.equal(f.timers.size,18);f.effects.clear();assert.equal(f.children.length,0);assert.equal(f.timers.size,0);assert.equal(f.announcement.textContent,'');
});
test('reduced motion gives one still reaction and invalid payloads never enter the DOM',()=>{
 const f=fixture(true);assert.equal(f.effects.show('<img onerror=bad>'),false);assert.equal(f.children.length,0);f.effects.show('👏');assert.equal(f.children.length,1);assert.match(f.children[0].className,/is-still/);f.children[0].animationend();assert.equal(f.timers.size,0);
});
test('disposing a call prevents late reactions from creating new effects',()=>{
 const f=fixture();f.effects.show('😂');f.effects.dispose();assert.equal(f.effects.show('🥰'),false);assert.equal(f.children.length,0);assert.equal(f.timers.size,0);
});
test('incoming effects are current-call/current-tab/current-negotiation only and never replay after hiding',async()=>{
 const source=readFileSync(new URL('../../main/resources/static/call-widget.js',import.meta.url),'utf8');const shown=[],panel={hidden:false};
 const c={active:()=>true,own:()=>true,call:{id:'current',state:'ACTIVE'},negotiationId:'neg',seenSignals:new Set(),document:{hidden:false},$:()=>panel,home:{self:{id:'self'},members:[{id:'self'},{id:'partner',displayName:'Partner'}]},reactionEffects:{show:(...args)=>shown.push(args)}};vm.createContext(c);vm.runInContext(source.slice(source.indexOf('async function receive('),source.indexOf('function sendMedia('))+';globalThis.receiveReaction=receive;',c);
 const event={type:'call.reaction',callId:'current',negotiationId:'neg',eventId:'one',payload:{emoji:'❤️'}};await c.receiveReaction(event);await c.receiveReaction(event);assert.deepEqual(shown,[['❤️','Partner']]);
 for(const change of [{callId:'old-call'},{negotiationId:'old-neg'}])await c.receiveReaction({...event,...change,eventId:JSON.stringify(change)});assert.equal(shown.length,1);
 panel.hidden=true;await c.receiveReaction({...event,eventId:'hidden'});panel.hidden=false;await c.receiveReaction({...event,eventId:'hidden'});assert.equal(shown.length,1);
 c.call.state='RECONNECTING';await c.receiveReaction({...event,eventId:'recovering'});c.call.state='ACTIVE';c.own=()=>false;await c.receiveReaction({...event,eventId:'another-tab'});assert.equal(shown.length,1);
});
