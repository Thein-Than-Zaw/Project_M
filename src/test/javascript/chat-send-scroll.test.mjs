import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';
const source=readFileSync(new URL('../../main/resources/static/chat.js',import.meta.url),'utf8');
const render=source.slice(source.indexOf("function render(mode="),source.indexOf('function visibleIncoming('));
const transmit=source.slice(source.indexOf('function transmit('),source.indexOf('function failSend('));
function node(){return {children:[],dataset:{},classList:{add(){}},append(...items){this.children.push(...items);},addEventListener(){},setAttribute(){}};}
function fixture(){
 const banner={hidden:true},log={scrollHeight:1000,scrollTop:30,clientHeight:300};let reconciles=0,acks=0;
 const c={records:new Map(),pending:new Map(),attempted:new Set(),cancelledSends:new Set(),selfId:'self',started:true,SEND_WAIT_MS:300000,renderIdentity:'before',initial:false,reactionMessageId:null,empty:{hidden:true},log,$:()=>banner,nearBottom:()=>log.scrollHeight-log.scrollTop-log.clientHeight<80,document:{createElement:node,createDocumentFragment:node},messageSignatures:new WeakMap(),messageBubble:node,persist(){},reconcileMessageRows(){reconciles++;log.scrollHeight=1100;},renderReplyDraft(){},renderMessageActions(){},queueAcks(){acks++;},live:{connected:()=>true,send:()=>true}};
 vm.createContext(c);vm.runInContext(render+transmit+';globalThis.draw=render;globalThis.send=transmit;',c);return {c,banner,log,get reconciles(){return reconciles;},get acks(){return acks;}};
}
test('real transmit render followed by explicit send bottom render reveals own text while reading older messages',()=>{
 const f=fixture(),item={clientMessageId:'own',body:'hello',state:'Queued',retryable:true};f.c.pending.set('own',item);
 f.c.send(item);assert.equal(f.log.scrollTop,30);assert.equal(f.banner.hidden,false);assert.equal(f.reconciles,1);
 f.c.draw('bottom');assert.equal(f.log.scrollTop,1100);assert.equal(f.banner.hidden,true);assert.equal(f.reconciles,1,'No redundant DOM reconciliation');assert.equal(f.acks,2);
});
test('same-content bottom command also works for offline text, stickers and inline media drafts',()=>{
 for(const type of ['TEXT','GIFT','FILE']){const f=fixture(),item={clientMessageId:type,body:'caption',type,state:'Sending…',deadlineAt:Date.now()+300000};f.c.pending.set(type,item);f.c.live.connected=()=>false;f.c.draw();f.c.draw('bottom');assert.equal(f.log.scrollTop,1100);assert.equal(f.banner.hidden,true);assert.equal(f.reconciles,1);}
});
test('ordinary unchanged updates never pull the reader away from older messages',()=>{
 const f=fixture();f.c.draw();f.log.scrollTop=45;f.banner.hidden=false;f.c.draw();assert.equal(f.log.scrollTop,45);assert.equal(f.banner.hidden,false);assert.equal(f.reconciles,1);
});
test('changed background render retains reading position while already-at-bottom render follows new content',()=>{
 const reading=fixture();reading.c.draw();assert.equal(reading.log.scrollTop,30);assert.equal(reading.banner.hidden,false);
 const bottom=fixture();bottom.log.scrollTop=700;bottom.c.draw();assert.equal(bottom.log.scrollTop,1100);assert.equal(bottom.banner.hidden,true);
});
