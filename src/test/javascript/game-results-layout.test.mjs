import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';
const source=readFileSync(new URL('../../main/resources/static/games.js',import.meta.url),'utf8');
function functions(){const c={};vm.createContext(c);vm.runInContext(source.slice(source.indexOf('export function fitConnectBoard('),source.indexOf('function scheduleGameLayout(')).replace('export ','')+source.slice(source.indexOf('export function gameResultPresentation(')).replaceAll('export function ','function ')+';globalThis.fit=fitConnectBoard;globalThis.effects=createGameResultEffects;globalThis.result=gameResultPresentation;',c);return c;}
function element(){const classes=new Set();return {hidden:true,dataset:{},textContent:'',children:[],style:{setProperty(){}},classList:{add:k=>classes.add(k),remove:k=>classes.delete(k),contains:k=>classes.has(k)},replaceChildren(){this.children=[];},append(n){this.children.push(n);}};}
function feedback(reduced=false){const c=functions(),panel=element(),title=element(),detail=element(),confetti=element(),jobs=new Map();let seq=0;const effects=c.effects({panel,title,detail,confetti,document:{createElement:element},reduceMotion:()=>reduced,setTimer:fn=>{jobs.set(++seq,fn);return seq;},clearTimer:id=>jobs.delete(id)});return {panel,title,detail,confetti,jobs,effects};}
test('board fits both measured width and height across narrow phone, landscape and laptop space',()=>{
 const c=functions();for(const width of [180,240,280,350,480,700])for(const height of [80,120,180,240,360,520]){const board=c.fit(width,height);assert.ok(board.width<=width+.001);assert.ok(board.height<=height+.001);assert.ok(board.cell>0);assert.ok(Math.abs((board.width-40)/7-(board.height-36)/6)<.001,'All42 discs remain square');}
 const short=c.fit(500,180);assert.ok(short.width<220);assert.ok(short.height<=180);assert.equal(c.fit(10,10).cell,0);
});
test('winner labels distinguish self and partner, draw and in-progress boards',()=>{
 const c=functions();assert.equal(c.result({status:'WON',winnerId:'me'},'me').title,'You Win!');assert.equal(c.result({status:'WON',winnerId:'partner'},'me').title,'Partner Wins!');assert.equal(c.result({status:'RESIGNED',winnerId:'partner'},'me').kind,'partner');assert.equal(c.result({status:'DRAW'},'me').kind,'draw');assert.equal(c.result({status:'ACTIVE'},'me'),null);
});
test('live win celebrates once, polling preserves result and dismissal survives further updates',()=>{
 const f=feedback(),active={id:'round',status:'ACTIVE'},won={id:'round',status:'WON',winnerId:'me'};f.effects.update(active,'me',true);assert.equal(f.panel.hidden,true);f.effects.update(won,'me',true);assert.equal(f.title.textContent,'You Win!');assert.equal(f.confetti.children.length,18);assert.equal(f.jobs.size,1);f.effects.update(won,'me',true);assert.equal(f.confetti.children.length,18);assert.equal(f.jobs.size,1);for(const callback of [...f.jobs.values()])callback();assert.equal(f.confetti.children.length,0);f.effects.update(won,'me',true);assert.equal(f.confetti.children.length,0);f.effects.dismiss();f.effects.update(won,'me',true);assert.equal(f.panel.hidden,true);f.effects.update({id:'next',status:'ACTIVE'},'me',true);assert.equal(f.panel.hidden,true);
});
test('historical results and reduced motion use static cards without replayed particles',()=>{
 const historical=feedback();historical.effects.update({id:'old',status:'WON',winnerId:'partner'},'me',true);assert.equal(historical.title.textContent,'Partner Wins!');assert.equal(historical.panel.hidden,false);assert.equal(historical.confetti.children.length,0);
 const reduced=feedback(true);reduced.effects.update({id:'live',status:'ACTIVE'},'me',true);reduced.effects.update({id:'live',status:'WON',winnerId:'me'},'me',true);assert.equal(reduced.panel.hidden,false);assert.equal(reduced.confetti.children.length,0);assert.equal(reduced.jobs.size,0);
});
test('hidden/closed game cancels effects and dispose clears timers and particles',()=>{
 const f=feedback();f.effects.update({id:'round',status:'ACTIVE'},'me',true);f.effects.update({id:'round',status:'WON',winnerId:'me'},'me',true);assert.equal(f.jobs.size,1);f.effects.update({id:'round',status:'WON',winnerId:'me'},'me',false);assert.equal(f.panel.hidden,true);assert.equal(f.jobs.size,0);assert.equal(f.confetti.children.length,0);f.effects.dispose();assert.equal(f.jobs.size,0);
});
test('actual layout measures other controls before fitting, and follows the visual viewport',()=>{
 const vars=new Map(),callVars=new Map(),style=map=>({setProperty:(k,v)=>map.set(k,v),removeProperty:k=>map.delete(k)}),board={style:style(vars)},players={height:22},status={height:18},actions={height:36},help={display:'none',height:40};
 const card={clientWidth:280,clientHeight:300,children:[players,status,board,actions,help]};for(const child of card.children)child.getBoundingClientRect=()=>({height:child.height||0});
 const call={style:style(callVars)},panel={parentElement:call,querySelector:()=>card};const c={panel,opened:true,$:()=>board,document:{getElementById:()=>call},window:{visualViewport:{height:430,offsetTop:5}},innerHeight:800,getComputedStyle:node=>node===card?{paddingTop:'8px',paddingBottom:'8px',paddingLeft:'8px',paddingRight:'8px',rowGap:'6px'}:node===board?{paddingLeft:'8px',columnGap:'4px'}:{display:node.display||'flex',marginTop:'0px',marginBottom:'0px'}};
 vm.createContext(c);vm.runInContext(source.slice(source.indexOf('export function fitConnectBoard('),source.indexOf('export function gameResultPresentation(')).replace('export ','')+';globalThis.fitView=fitGameView;',c);c.fitView();assert.equal(callVars.get('--game-call-height'),'430px');assert.equal(callVars.get('--game-call-top'),'5px');assert.ok(parseFloat(vars.get('--game-board-height'))<=190);assert.ok(parseFloat(vars.get('--game-board-width'))<=264);c.opened=false;c.fitView();assert.equal(vars.size,0);assert.equal(callVars.size,0);
});
test('resize/state events coalesce into one layout pass per frame',()=>{
 const frames=[];let fits=0;const c={closed:false,layoutFrame:0,requestAnimationFrame:fn=>{frames.push(fn);return frames.length;},fitGameView:()=>fits++};vm.createContext(c);vm.runInContext(source.slice(source.indexOf('function scheduleGameLayout('),source.indexOf('function fitGameView('))+';globalThis.schedule=scheduleGameLayout;',c);c.schedule();c.schedule();c.schedule();assert.equal(frames.length,1);frames[0]();assert.equal(fits,1);assert.equal(c.layoutFrame,0);c.closed=true;c.schedule();assert.equal(frames.length,1);
});
