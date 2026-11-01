import test from 'node:test';
import assert from 'node:assert/strict';
import {createBrowserNotifications} from '../../main/resources/static/browser-notifications.js';
function fixture({shared=new Map(),permission='default',background=true,secure=true}={}){
 const notices=[],state={background,prompts:0,focus:0},storage={getItem:k=>shared.get(k)??null,setItem:(k,v)=>shared.set(k,v)};
 class Notice{static permission=permission;static async requestPermission(){state.prompts++;this.permission='granted';return 'granted';}constructor(title,options){this.title=title;this.options=options;this.closed=false;notices.push(this);}close(){this.closed=true;this.onclose?.();}}
 const engine=createBrowserNotifications({storage,api:Notice,secure,isBackground:()=>state.background,focus:()=>state.focus++});return {engine,Notice,state,shared,notices};
}
const alert=id=>({id,title:'us, always',body:'A new message is waiting.'});
test('default off never requests permission or emits alerts without explicit enable',async()=>{
 const f=fixture();assert.equal(f.engine.enabled,false);assert.equal(await f.engine.show(alert('one')),false);assert.equal(f.state.prompts,0);await f.engine.enable();assert.equal(f.state.prompts,1);assert.equal(f.engine.enabled,true);assert.equal(await f.engine.show(alert('two')),true);assert.equal(f.notices[0].options.silent,true);
});
test('foreground app suppresses automatic alerts, but explicit test can show one',async()=>{
 const f=fixture({background:false});await f.engine.enable();assert.equal(await f.engine.show(alert('foreground')),false);assert.equal(await f.engine.show({...alert('test'),test:true}),true);assert.equal(f.notices.length,1);
});
test('same event deduplicates across tabs; off immediately closes visible alerts',async()=>{
 const shared=new Map(),a=fixture({shared}),b=fixture({shared,permission:'granted'});await a.engine.enable();b.engine.sync();await a.engine.show(alert('same'));assert.equal(await b.engine.show(alert('same')),false);a.engine.disable();assert.ok(a.notices[0].closed);assert.equal(await b.engine.show(alert('off-before-storage-event')),false);b.engine.sync();assert.equal(b.engine.enabled,false);
});
test('denied permission is actionable, never repeatedly prompts, and keeps switch off',async()=>{
 const f=fixture({permission:'denied'});await assert.rejects(f.engine.enable(),/browser site settings/);assert.equal(f.state.prompts,0);assert.equal(f.engine.enabled,false);assert.equal(await f.engine.show(alert('no')),false);
});
test('unsupported insecure context never requests permission',async()=>{
 const f=fixture({secure:false});assert.equal(f.engine.supported,false);await assert.rejects(f.engine.enable(),/unavailable/);assert.equal(f.state.prompts,0);
});
test('disabling while permission is pending cannot re-enable notifications later',async()=>{
 const f=fixture();let grant;f.Notice.requestPermission=()=>new Promise(resolve=>{grant=resolve;});const enabling=f.engine.enable();f.engine.disable();f.Notice.permission='granted';grant('granted');assert.equal(await enabling,false);assert.equal(f.engine.enabled,false);
});
test('click focuses existing app and invokes safe action; dismiss/dispose close alerts',async()=>{
 const f=fixture();await f.engine.enable();let clicked=0;await f.engine.show({...alert('call'),onClick:()=>clicked++});f.notices[0].onclick();assert.equal(f.state.focus,1);assert.equal(clicked,1);assert.ok(f.notices[0].closed);await f.engine.show(alert('terminal'));f.engine.dismiss('terminal');assert.ok(f.notices[1].closed);await f.engine.show(alert('last'));f.engine.dispose();assert.ok(f.notices[2].closed);
});
test('constructor failure preserves in-app operation with visible settings feedback',async()=>{
 const shared=new Map([['couple.browserNotifications.enabled','true']]);class Unsupported{static permission='granted';static async requestPermission(){return 'granted';}constructor(){throw new TypeError('unsupported constructor');}}
 const engine=createBrowserNotifications({storage:{getItem:k=>shared.get(k),setItem:(k,v)=>shared.set(k,v)},api:Unsupported,secure:true,isBackground:()=>true,focus(){}});assert.equal(await engine.show(alert('unsupported')),false);assert.match(engine.error,/In-app alerts/);
});
test('a queued popup cannot appear after call dismissal or page disposal',async()=>{
 for(const action of ['dismiss','dispose']){
  const shared=new Map([['couple.browserNotifications.enabled','true']]);let deliver,count=0;
  class Native{static permission='granted';static async requestPermission(){return 'granted';}constructor(){count++;}}
  const engine=createBrowserNotifications({storage:{getItem:k=>shared.get(k),setItem:(k,v)=>shared.set(k,v)},api:Native,secure:true,isBackground:()=>true,focus(){},lock:task=>new Promise(resolve=>{deliver=()=>resolve(task());})});
  const result=engine.show(alert('call'));if(action==='dismiss')engine.dismiss('call');else engine.dispose();deliver();assert.equal(await result,false);assert.equal(count,0);
 }
});
test('lock denial is contained and does not reject unrelated message/call handlers',async()=>{
 const shared=new Map([['couple.browserNotifications.enabled','true']]);class Native{static permission='granted';static async requestPermission(){return 'granted';}}
 const engine=createBrowserNotifications({storage:{getItem:k=>shared.get(k),setItem:(k,v)=>shared.set(k,v)},api:Native,secure:true,isBackground:()=>true,focus(){},lock:()=>Promise.reject(new Error('unavailable'))});assert.equal(await engine.show(alert('safe')),false);assert.match(engine.error,/temporarily unavailable/);
});
