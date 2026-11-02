import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';
// Execute the real chat transition functions with controlled transport/storage, not a UI simulation.
const source=readFileSync(new URL('../../main/resources/static/chat.js',import.meta.url),'utf8');
const merge=source.slice(source.indexOf('function merge('),source.indexOf('function receipt('));
const redact=source.slice(source.indexOf('function redactReferences('),source.indexOf('function renderReplyDraft('));
const transmit=source.slice(source.indexOf('function transmit('),source.indexOf('function flushPending('));
function fixture({connected=true,accepted=true}={}){
 const sounds=[],context={records:new Map(),pending:new Map(),attempted:new Set(),cancelledSends:new Set(),chatMedia:null,SEND_WAIT_MS:300000,hiddenMessages:new Set(),redactReferences(){},selfId:'self',started:true,notificationSounds:{sent:id=>sounds.push(id)},persist(){},render(){},$(){return {};},live:{connected:()=>connected,send:()=>accepted}};
 vm.createContext(context);vm.runInContext(merge+transmit+redact+';globalThis.hooks={merge,transmit};',context);
 const queued={clientMessageId:'intent',body:'hello',type:'TEXT',state:'Queued'};context.pending.set('intent',queued);
 const message={id:'committed',clientMessageId:'intent',senderId:'self',body:'hello',type:'TEXT',giftCode:null,deliveredAt:null,readAt:null};return {context,sounds,queued,message};
}
test('accepted transport alone stays silent; confirmed matching server response chimes exactly once',()=>{
 const f=fixture();f.context.hooks.transmit(f.queued);assert.deepEqual(f.sounds,[]);assert.ok(f.context.attempted.has('intent'));f.context.hooks.merge(f.message);assert.deepEqual(f.sounds,['committed']);f.context.hooks.merge({...f.message,readAt:'2026-11-01T00:00:00Z'});assert.deepEqual(f.sounds,['committed']);
});
test('queued/offline/failed transport and historical own response never produce sent chime',()=>{
 for(const options of [{connected:false},{accepted:false}]){const f=fixture(options);f.context.hooks.transmit(f.queued);assert.equal(f.queued.state,'Sending…');assert.equal(f.context.attempted.size,0);f.context.hooks.merge(f.message);assert.deepEqual(f.sounds,[]);}
 const history=fixture();history.context.hooks.merge(history.message);assert.deepEqual(history.sounds,[]);
});
test('partner message or mismatched body cannot confirm an outgoing intent',()=>{
 const f=fixture();f.context.hooks.transmit(f.queued);f.context.hooks.merge({...f.message,senderId:'partner'});f.context.hooks.merge({...f.message,body:'different'});assert.deepEqual(f.sounds,[]);assert.ok(f.context.pending.has('intent'));f.context.hooks.merge(f.message);assert.deepEqual(f.sounds,['committed']);
});

test('deleted and hidden own responses clear pending text without a sent sound or resurrection',()=>{
 for(const state of [{deletedAt:'2026-11-01T00:00:00Z',body:'',version:1},{hidden:true,body:''}]){const f=fixture();f.context.hooks.transmit(f.queued);f.context.hooks.merge({...f.message,...state});assert.equal(f.context.pending.size,0);assert.equal(f.context.attempted.size,0);assert.deepEqual(f.sounds,[]);if(state.hidden)assert.equal(f.context.records.size,0);else assert.equal(f.context.records.get('committed').body,'');}
});
test('older live payload cannot overwrite a newer deletion or restore hidden message content',()=>{
 const f=fixture();f.context.hooks.merge({...f.message,deletedAt:'2026-11-01T00:00:00Z',body:'',version:3});f.context.hooks.merge({...f.message,version:0});assert.equal(f.context.records.get('committed').body,'');f.context.hooks.merge({...f.message,hidden:true,body:'',version:3});f.context.hooks.merge(f.message);assert.equal(f.context.records.size,0);
});
test('deleting an original redacts loaded reply previews even before the next REST snapshot',()=>{
 const f=fixture();f.context.records.set('reply',{id:'reply',replyToId:'committed',reply:{id:'committed',body:'private original'}});f.context.hooks.merge({...f.message,deletedAt:'2026-11-01T00:00:00Z',body:'',version:1});assert.equal(f.context.records.get('reply').reply.unavailable,true);assert.equal(f.context.records.get('reply').reply.body,undefined);
});
test('reply target is transmitted and mismatched confirmation cannot clear another reply intent',()=>{
 const f=fixture();let payload;f.queued.replyToId='original';f.context.live.send=(_,value)=>{payload=value;return true;};f.context.hooks.transmit(f.queued);assert.equal(payload.replyToId,'original');f.context.hooks.merge({...f.message,replyToId:'other'});assert.deepEqual(f.sounds,[]);assert.equal(f.context.pending.size,1);f.context.hooks.merge({...f.message,replyToId:'original'});assert.deepEqual(f.sounds,['committed']);
});
test('read receipts exclude chat obscured by dialogs or the full-page call',()=>{
 const code=source.slice(source.indexOf('function visibleIncoming('),source.indexOf('function flushAcks('));
 const dialogs={'#gifts-dialog':{open:false},'#message-actions':{open:false},'#notification-inbox':{open:false}},call={hidden:true};let inspected=0;
 const context={document:{visibilityState:'visible',hasFocus:()=>true,getElementById:()=>call},$:key=>dialogs[key],innerHeight:500,selfId:'self',records:new Map([['incoming',{senderId:'partner',readAt:null}]]),log:{getBoundingClientRect:()=>{inspected++;return {top:0,bottom:100};},querySelectorAll:()=>[{dataset:{id:'incoming'},getBoundingClientRect:()=>({top:10,bottom:30})}]}};
 vm.createContext(context);vm.runInContext(code+';globalThis.visible=visibleIncoming;',context);assert.deepEqual(Array.from(context.visible()),['incoming']);
 for(const dialog of Object.values(dialogs)){dialog.open=true;inspected=0;assert.equal(context.visible().length,0);assert.equal(inspected,0);dialog.open=false;}call.hidden=false;assert.equal(context.visible().length,0);
});
test('unavailable reply recovery keeps typed intent and only detaches after explicit action',()=>{
 const f=fixture();f.queued.body='မင်္ဂလာပါ 🌸';f.queued.replyToId='deleted-original';f.queued.code='REPLY_UNAVAILABLE';f.queued.type='GIFT';f.queued.giftCode='safe-sticker';f.queued.giftName='Sticker';f.context.crypto={randomUUID:()=> 'new-intent'};f.context.attempted.add('intent');
 const helper=source.slice(source.indexOf('function resendWithoutReply('),source.indexOf('// CHAT-02B:'));vm.runInContext(helper+';globalThis.detach=resendWithoutReply;',f.context);
 assert.ok(f.context.pending.has('intent'));f.context.detach('intent');assert.equal(f.context.pending.has('intent'),false);const replacement=f.context.pending.get('new-intent');assert.equal(replacement.body,'မင်္ဂလာပါ 🌸');assert.equal(replacement.giftCode,'safe-sticker');assert.equal(replacement.replyToId,null);assert.ok(f.context.attempted.has('new-intent'));assert.equal(f.context.attempted.has('intent'),false);assert.deepEqual(f.sounds,[]);
});

test('sending deadline covers text and media and reconnect does not extend the five minute wait',()=>{
 for(const type of ['TEXT','FILE','GIFT']){const f=fixture({connected:false});f.queued.type=type;if(type==='FILE')f.queued.attachmentId='asset';f.context.hooks.transmit(f.queued);const deadline=f.queued.deadlineAt;assert.ok(deadline>Date.now()+299000);f.context.hooks.transmit(f.queued);assert.equal(f.queued.deadlineAt,deadline);assert.equal(f.queued.state,'Sending…');f.queued.deadlineAt=Date.now()-1;f.context.checkSendTimeouts();assert.equal(f.queued.state,'Failed');f.context.hooks.transmit(f.queued);assert.equal(f.queued.state,'Failed');f.context.hooks.transmit(f.queued,true);assert.equal(f.queued.state,'Sending…');assert.ok(f.queued.deadlineAt>Date.now());}
});

test('media remains pending while upload resolves; cancelling prevents a late upload from sending',async()=>{
 const f=fixture();let resolve,transportSends=0;f.context.live.send=()=>{transportSends++;return true;};f.context.chatMedia={upload:()=>new Promise(r=>resolve=r)};f.queued.type='FILE';f.context.hooks.transmit(f.queued);assert.equal(f.queued.state,'Sending…');assert.equal(transportSends,0);f.context.cancelledSends.add('intent');f.context.pending.delete('intent');resolve({id:'late'});await Promise.resolve();await Promise.resolve();assert.equal(transportSends,0);
});

test('late real server confirmation resolves a timed-out text without duplicating the send',()=>{
 const f=fixture();f.context.hooks.transmit(f.queued);f.queued.deadlineAt=Date.now()-1;f.context.checkSendTimeouts();assert.equal(f.queued.state,'Failed');f.context.hooks.merge(f.message);assert.equal(f.context.pending.size,0);assert.equal(f.context.records.size,1);assert.deepEqual(f.sounds,['committed']);
});
