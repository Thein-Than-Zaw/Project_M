import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';
const source=readFileSync(new URL('../../main/resources/static/chat.js',import.meta.url),'utf8');
function element(tag){return {tag,children:[],classList:{add(){}},append(...nodes){this.children.push(...nodes);}};}
function context(){const c={document:{createElement:element},selfId:'self',time:()=> '12:00',date:()=> '3 Nov',cancelledSends:new Set()};vm.createContext(c);return c;}
test('Edited appears beside original time only after a real edit and never on deleted messages',()=>{
 const c=context();vm.runInContext(source.slice(source.indexOf('function messageMetadata('),source.indexOf('function render(mode='))+';globalThis.meta=messageMetadata;',c);
 const base={senderId:'self',createdAt:'2026-11-03T00:00:00Z',type:'TEXT'};
 assert.deepEqual(Array.from(c.meta(base).children,n=>n.textContent),['12:00','Sent']);
 const edited=c.meta({...base,editedAt:'2026-11-03T01:00:00Z'});assert.deepEqual(Array.from(edited.children,n=>n.textContent),['12:00','Edited','Sent']);assert.equal(edited.children[0].dateTime,base.createdAt);
 assert.deepEqual(Array.from(c.meta({...base,editedAt:'some edit',deletedAt:'deleted'}).children,n=>n.textContent),['12:00','Deleted']);
 assert.deepEqual(Array.from(c.meta({...base,type:'CALL',readAt:'read'}).children,n=>n.textContent),['12:00']);
});
test('video call entries show direction, every outcome and connected duration without normal message body',()=>{
 const c=context();vm.runInContext(source.slice(source.indexOf('function messageBubble('),source.indexOf('function messageMetadata('))+source.slice(source.indexOf('function callDuration('),source.indexOf('function editCount('))+';globalThis.bubble=messageBubble;',c);
 for(const [state,label] of Object.entries({ENDED:'Ended',MISSED:'Missed',DECLINED:'Declined',CANCELLED:'Cancelled',FAILED:'Connection interrupted'})){
  const node=c.bubble({type:'CALL',senderId:'partner',call:{state}});assert.equal(node.children[0].textContent,'Incoming video call');assert.equal(node.children[1].textContent,label);
 }
 const connected=c.bubble({type:'CALL',senderId:'self',call:{state:'ENDED',connectedAt:'time',durationSeconds:3661}});assert.equal(connected.children[0].textContent,'Outgoing video call');assert.equal(connected.children[1].textContent,'Ended · 1:01:01');
});
test('confirmed retry of an edited original clears its old outbox draft without reverting the edit',()=>{
 const c={records:new Map(),pending:new Map([['key',{clientMessageId:'key',body:'original',type:'TEXT'}]]),attempted:new Set(),cancelledSends:new Set(),hiddenMessages:new Set(),selfId:'self',persist(){},redactReferences(){},notificationSounds:{sent(){}},chatMedia:null};vm.createContext(c);vm.runInContext(source.slice(source.indexOf('function merge('),source.indexOf('function receipt('))+';globalThis.mergeMessage=merge;',c);
 c.mergeMessage({id:'saved',clientMessageId:'key',senderId:'self',body:'edited',type:'TEXT',giftCode:null,editedAt:'2026-11-03T00:00:00Z',version:1});assert.equal(c.pending.size,0);assert.equal(c.records.get('saved').body,'edited');
});
