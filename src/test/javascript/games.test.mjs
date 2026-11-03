import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';
const source=readFileSync(new URL('../../main/resources/static/games.js',import.meta.url),'utf8');
function extract(start,end){return source.slice(source.indexOf(start),source.indexOf(end)).replaceAll('export function ','function ');}
test('status covers private invitation, each turn and terminal result without a timer',()=>{
 const c={};vm.createContext(c);vm.runInContext(extract('export function gameStatus(','function apply(')+';globalThis.status=gameStatus;globalThis.can=canDrop;',c);const name=id=>id==='partner'?'Your person':'You';
 assert.match(c.status(null,'me',name),/Invite/);assert.match(c.status({status:'WAITING',inviterId:'me'},'me',name),/Waiting/);assert.match(c.status({status:'WAITING',inviterId:'partner'},'me',name),/Your person invited/);assert.match(c.status({status:'ACTIVE',nextPlayerId:'me'},'me',name),/Your turn/);assert.match(c.status({status:'ACTIVE',nextPlayerId:'partner'},'me',name),/thinking/);assert.match(c.status({status:'DRAW'},'me',name),/draw/);assert.match(c.status({status:'WON',winnerId:'me'},'me',name),/You won/);assert.match(c.status({status:'RESIGNED',winnerId:'partner'},'me',name),/resigned/);assert.match(c.status({status:'CANCELLED'},'me',name),/cancelled/);assert.match(c.status({status:'DECLINED'},'me',name),/declined/);
 const game={status:'ACTIVE',nextPlayerId:'me',board:'.'.repeat(42)};assert.equal(c.can(game,'me',2,false),true);for(const g of [{...game,status:'WAITING'},{...game,status:'WON'},{...game,nextPlayerId:'partner'},{...game,board:'R'+'.'.repeat(41)}])assert.equal(c.can(g,'me',0,false),false);assert.equal(c.can(game,'me',2,true),false);
});
test('uncertain action retries with the same request key and original expected board version',async()=>{
 const requests=[],nodes=new Map();let calls=0;const c={busy:false,closed:false,denied:false,pending:null,page:{game:{id:'game',version:4}},crypto:{randomUUID:()=> 'stable-key'},$:id=>{if(!nodes.has(id))nodes.set(id,{});return nodes.get(id);},draw(){},api:async(path,body)=>{requests.push({path,body});if(++calls===1)throw {status:500};return {game:{id:'game',version:5}};},apply(){},load:async()=>{}};
 vm.createContext(c);vm.runInContext(extract('async function act(','for(let column=0;')+';globalThis.act=act;',c);await c.act('move',3);assert.equal(c.pending.body.actionId,'stable-key');c.page.game.version=6;await c.act('retry');assert.equal(requests.length,2);assert.equal(requests[1].body.version,4);assert.equal(requests[1].body.actionId,'stable-key');assert.equal(c.pending,null);
});
test('definite stale action refreshes instead of silently applying another move',async()=>{
 let refreshed=0;const c={busy:false,closed:false,denied:false,pending:null,page:{game:{id:'game',version:4}},crypto:{randomUUID:()=> 'key'},$:()=>({}),draw(){},api:async()=>{throw {status:409,message:'Game changed'};},load:async()=>{refreshed++;}};vm.createContext(c);vm.runInContext(extract('async function act(','for(let column=0;')+';globalThis.act=act;',c);await c.act('move',1);assert.equal(c.pending,null);assert.equal(refreshed,1);assert.equal(c.busy,false);
});
test('opening Games embeds into current call view without navigation or media teardown',()=>{
 const moved=[],attrs={};const panel={inert:true,setAttribute:(k,v)=>attrs[k]=v},call={hidden:false,dataset:{running:'true'},append:p=>moved.push(['call',p])};const c={panel,document:{getElementById:()=>call,body:{append:p=>moved.push(['body',p])}}};vm.createContext(c);vm.runInContext(extract('function embed(','function open(')+';globalThis.embed=embed;',c);c.embed();assert.equal(moved[0][0],'call');assert.equal(panel.inert,false);assert.equal(call.dataset.gamesOpen,'true');assert.equal(attrs['aria-modal'],'false');call.dataset.running='false';c.embed();assert.equal(moved.at(-1)[0],'body');assert.equal(attrs['aria-modal'],'true');assert.equal(call.dataset.gamesOpen,undefined);
});
test('older HTTP response cannot replace a newer board from a live update',()=>{
 let draws=0;const c={page:{game:{id:'new',version:10}},denied:false,draw:()=>draws++};vm.createContext(c);vm.runInContext(extract('function apply(','async function api(')+';globalThis.applyPage=apply;',c);c.applyPage({game:{id:'old',version:9}});c.applyPage({game:null});assert.equal(c.page.game.id,'new');assert.equal(draws,0);c.applyPage({game:{id:'new',version:11}});assert.equal(c.page.game.version,11);assert.equal(draws,1);
});
