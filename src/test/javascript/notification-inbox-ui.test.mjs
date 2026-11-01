import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';
const source=readFileSync(new URL('../../main/resources/static/notification-inbox.js',import.meta.url),'utf8');
const helpers=source.slice(source.indexOf('function notificationEntryRoots('));
const c={};vm.createContext(c);vm.runInContext(helpers+';globalThis.roots=notificationEntryRoots;globalThis.unread=unreadItems;',c);
test('Chat never mounts an inbox bell; Home/Memories retain their header/mobile entries',()=>{
 assert.equal(c.roots({querySelector:()=>({}),querySelectorAll:()=>{throw Error('Chat must not mount entries');}}).length,0);
 const entries=[{id:'header'},{id:'mobile'}];assert.equal(c.roots({querySelector:()=>null,querySelectorAll:selector=>{assert.equal(selector,'.world-header,.memory-heading,.mobile-nav');return entries;}}),entries);
});
test('Read notifications stay out of the list including a later read-state change',()=>{
 const read={id:'read',readAt:'2026-11-02T01:00:00Z'},unread={id:'unread',readAt:null};const items=new Map([[read.id,read],[unread.id,unread]]);
 assert.equal(c.unread(items.values()).length,1);assert.equal(c.unread(items.values())[0].id,'unread');unread.readAt='2026-11-02T02:00:00Z';assert.equal(c.unread(items.values()).length,0);
});
