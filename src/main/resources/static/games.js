import {watchPairing} from '/pairing-socket.js';

// GAME-01: one server-authoritative couple board; same-document view keeps calls alive.
const template=document.createElement('template');template.innerHTML=`
<button id="games-invite-notice" class="games-invite-notice" type="button" hidden>♡ Your person invited you to play · Open Games</button>
<section id="games-panel" class="games-panel" role="dialog" aria-modal="true" aria-labelledby="games-title" tabindex="-1" hidden>
 <div class="games-shell"><header class="games-heading"><div><span class="eyebrow">A LITTLE PLAY, JUST US</span><h2 id="games-title">Games</h2></div><button id="games-close" class="secondary" type="button" aria-label="Close Games">×</button></header>
 <article class="connect-card"><div class="connect-title"><div><h3>Connect 4</h3><p>Four in a Row · 7 × 6</p></div><span class="connect-mark" aria-hidden="true">●●●●</span></div>
 <p id="games-connection" class="games-connection" role="status">Opening your shared board…</p><p id="games-error" class="field-error" role="alert"></p>
 <div id="game-players" class="game-players"></div><p id="game-status" class="game-status" role="status" aria-live="polite"></p>
 <div id="connect-board" class="connect-board" role="group" aria-label="Connect 4 board, columns 1 to 7"><section id="game-result" class="game-result" aria-labelledby="game-result-title" hidden><div id="game-confetti" class="game-confetti" aria-hidden="true"></div><div class="game-result-card"><span class="game-result-symbol" aria-hidden="true">✦</span><h3 id="game-result-title"></h3><p id="game-result-detail"></p><button id="game-result-dismiss" class="primary" type="button">View board</button></div></section></div>
 <div id="game-actions" class="game-actions"><button id="game-invite" class="primary" type="button">Invite to play</button><button id="game-accept" class="primary" type="button" hidden>Accept &amp; play</button><button id="game-decline" class="secondary" type="button" hidden>Decline</button><button id="game-cancel" class="secondary" type="button" hidden>Cancel invitation</button><button id="game-resign" class="text-button" type="button" hidden>Resign</button><button id="game-retry" class="secondary" type="button" hidden>Retry / check saved action</button></div>
 <div id="game-resign-confirm" class="game-resign-confirm" hidden><p>Resign this game? Your person will win.</p><button id="game-resign-yes" class="primary" type="button">Resign game</button><button id="game-resign-no" class="secondary" type="button">Keep playing</button></div>
 <p class="game-help">Take turns dropping a disc. Connect four across, down or diagonally. Red goes first. No timer — enjoy your time together.</p>
 </article><p class="games-call-note">Your call stays connected while you play. Closing Games keeps your board saved.</p></div>
</section>`;document.body.append(template.content.cloneNode(true));
const $=id=>document.getElementById(id),panel=$('games-panel'),columns=[];
let page=null,pending=null,busy=false,loading=false,reload=false,closed=false,opened=false,previousFocus,stopSocket,poll,denied=false;
const resultEffects=createGameResultEffects({panel:$('game-result'),title:$('game-result-title'),detail:$('game-result-detail'),confetti:$('game-confetti'),document,reduceMotion:()=>window.matchMedia('(prefers-reduced-motion: reduce)').matches});
let layoutFrame=0;
function person(id){return page?.members.find(m=>m.id===id)?.displayName||'Your person';}
export function gameStatus(game,selfId,name){
 if(!game)return 'Invite your person to a little friendly match.';
 if(game.status==='WAITING')return game.inviterId===selfId?'Waiting for your person to accept…':name(game.inviterId)+' invited you to play.';
 if(game.status==='ACTIVE')return game.nextPlayerId===selfId?'Your turn — choose a column.':name(game.nextPlayerId)+' is thinking…';
 if(game.status==='DRAW')return 'A perfect match — it’s a draw.';
 if(game.status==='WON'||game.status==='RESIGNED')return (game.winnerId===selfId?'You won!':name(game.winnerId)+' won!')+(game.status==='RESIGNED'?' · Game resigned.':' · Four in a row.');
 return game.status==='DECLINED'?'Invitation declined. You can invite again.':'Invitation cancelled.';
}
export function canDrop(game,selfId,column,blocked){return !blocked&&game?.status==='ACTIVE'&&game.nextPlayerId===selfId&&game.board[column]==='.';}
function apply(next){if(page?.game&&(!next.game||next.game.version<page.game.version))return;page=next;denied=false;draw();}
async function api(path,body){
 const options={cache:'no-store',signal:AbortSignal.timeout(10000)};
 if(body){const csrf=await api('/api/csrf');options.method='POST';options.headers={[csrf.headerName]:csrf.token,'Content-Type':'application/json'};options.body=JSON.stringify(body);}
 const r=await fetch(path,options);if(!r.ok){const data=await r.json().catch(()=>({}));throw Object.assign(new Error(data.message||'Could not update the game.'),{status:r.status});}return r.json();
}
async function load(){
 if(closed||denied)return;if(loading){reload=true;return;}loading=true;
 try{const next=await api('/api/games/connect4');if(closed)return;apply(next);$('games-connection').textContent='Private board · saved and synced';if(!pending)$('game-retry').hidden=true;}
 catch(error){if(closed)return;if([401,403].includes(error.status)){page=null;pending=null;denied=true;draw();}$('games-connection').textContent='Board connection unavailable';if(opened)$('games-error').textContent=error.message;$('game-retry').hidden=false;}
 finally{loading=false;if(reload&&!closed){reload=false;load();}}
}
async function act(operation,column){
 if(busy||closed||denied)return;if(pending&&operation!=='retry')return;
 if(operation!=='retry')pending={operation,body:{actionId:crypto.randomUUID(),gameId:page?.game?.id||null,version:page?.game?.version??null,column:column??null}};
 if(!pending){load();return;}busy=true;$('games-error').textContent='';$('game-resign-confirm').hidden=true;draw();
 try{const next=await api('/api/games/connect4/'+pending.operation,pending.body);if(closed)return;pending=null;apply(next);$('games-connection').textContent='Private board · saved and synced';}
 catch(error){if(closed)return;const definite=error.status&&error.status<500;if(definite){pending=null;if([401,403].includes(error.status)){denied=true;page=null;}await load();}$('games-error').textContent=definite?error.message:'Action not confirmed. Retry to check whether it was saved. Your board stays safe.';}
 finally{busy=false;if(!closed)draw();}
}
for(let column=0;column<7;column++){
 const button=document.createElement('button');button.type='button';button.className='connect-column';button.setAttribute('aria-label','Drop in column '+(column+1));
 for(let row=0;row<6;row++){const slot=document.createElement('span');slot.className='connect-slot';button.append(slot);}
 button.addEventListener('click',()=>{if(canDrop(page?.game,page?.selfId,column,busy||!!pending||denied))act('move',column);});
 button.addEventListener('keydown',event=>{if(['ArrowLeft','ArrowRight','Home','End'].includes(event.key)){event.preventDefault();const target=event.key==='Home'?0:event.key==='End'?6:(column+(event.key==='ArrowLeft'?6:1))%7;columns[target].focus({preventScroll:true});}});
 columns.push(button);$('connect-board').append(button);
}
function draw(){
 const game=page?.game,blocked=busy||!!pending||!page||denied;
 const status=gameStatus(game,page?.selfId,person);if($('game-status').textContent!==status)$('game-status').textContent=status;
 resultEffects.update(game,page?.selfId,opened&&!document.hidden);
 if(game?.status!=='ACTIVE')$('game-resign-confirm').hidden=true;
 for(let c=0;c<7;c++){
  const button=columns[c],allowed=canDrop(game,page?.selfId,c,blocked);button.setAttribute('aria-disabled',String(!allowed));button.classList.toggle('can-drop',allowed);let occupied=0;
  for(let r=0;r<6;r++){const cell=r*7+c,disc=game?.board[cell]||'.',slot=button.children[r];if(disc!=='.')occupied++;slot.className='connect-slot'+(disc==='R'?' red':disc==='Y'?' yellow':'')+(game?.winningCells.includes(cell)?' winning':'');}
  button.setAttribute('aria-label','Column '+(c+1)+', '+occupied+' of 6 discs'+(allowed?'. Drop your '+(game.nextPlayerId===game.redId?'red':'yellow')+' disc.':'. '+(game?.board[c]!=='.'&&game?'Column full.':'Not your turn.')));
 }
 const players=$('game-players');players.replaceChildren();if(game)for(const [id,color] of [[game.redId,'red'],[game.yellowId,'yellow']]){const label=document.createElement('span');label.className='game-player '+color+(game.nextPlayerId===id?' is-turn':'');label.textContent=(id===page.selfId?'You':person(id))+(game.nextPlayerId===id?' · turn':'');players.append(label);}
 $('game-invite').hidden=!!game&&['WAITING','ACTIVE'].includes(game.status);$('game-invite').textContent=game?'Invite to rematch':'Invite to play';
 $('game-accept').hidden=$('game-decline').hidden=!(game?.status==='WAITING'&&game.inviterId!==page?.selfId);
 $('game-cancel').hidden=!(game?.status==='WAITING'&&game.inviterId===page?.selfId);$('game-resign').hidden=game?.status!=='ACTIVE';$('game-retry').hidden=!pending&&$('games-connection').textContent!=='Board connection unavailable';
 for(const button of $('game-actions').querySelectorAll('button'))button.disabled=busy||denied||!page||!!pending&&button.id!=='game-retry';$('game-retry').disabled=busy;
 const invited=!!(game?.status==='WAITING'&&game.inviterId!==page?.selfId);$('games-invite-notice').hidden=!invited||opened;window.dispatchEvent(new CustomEvent('game-invitation',{detail:{invited}}));scheduleGameLayout();
}
function embed(){const call=document.getElementById('call-panel');if(call&&!call.hidden&&call.dataset.running==='true'){if(panel.parentElement!==call)call.append(panel);panel.inert=false;call.dataset.gamesOpen='true';panel.setAttribute('aria-modal','false');}else{if(call)delete call.dataset.gamesOpen;if(panel.parentElement!==document.body)document.body.append(panel);panel.inert=false;panel.setAttribute('aria-modal','true');}scheduleGameLayout();}
function open(){if(closed)return;previousFocus=document.activeElement;window.dispatchEvent(new Event('games-opening'));opened=true;panel.hidden=false;embed();$('games-invite-notice').hidden=true;panel.focus({preventScroll:true});draw();load();}
function close(){if(closed)return;opened=false;panel.hidden=true;const call=document.getElementById('call-panel');if(call)delete call.dataset.gamesOpen;document.body.append(panel);draw();previousFocus?.focus({preventScroll:true});}
for(const button of document.querySelectorAll('[data-open-games]'))button.addEventListener('click',event=>{event.preventDefault();open();});
$('games-invite-notice').addEventListener('click',open);$('games-close').addEventListener('click',close);
for(const op of ['invite','accept','decline','cancel'])$('game-'+op).addEventListener('click',()=>act(op));
$('game-resign').addEventListener('click',()=>{$('game-resign-confirm').hidden=false;$('game-resign-yes').focus({preventScroll:true});scheduleGameLayout();});$('game-resign-no').addEventListener('click',()=>{$('game-resign-confirm').hidden=true;scheduleGameLayout();});$('game-resign-yes').addEventListener('click',()=>act('resign'));$('game-retry').addEventListener('click',()=>{denied=false;pending?act('retry'):load();});
window.addEventListener('open-games',open);window.addEventListener('call-view-shown',()=>{if(opened)embed();});window.addEventListener('call-view-state',()=>{if(opened)embed();});window.addEventListener('call-view-hiding',()=>{if(opened)close();});
document.addEventListener('keydown',event=>{if(!opened)return;if(event.key==='Escape'){event.preventDefault();event.stopImmediatePropagation();close();}else if(event.key==='Tab'&&panel.parentElement===document.body){const focusable=[...panel.querySelectorAll('button')].filter(b=>!b.disabled&&b.getClientRects().length);if(!focusable.length)return;const first=focusable[0],last=focusable.at(-1);if(event.shiftKey&&(document.activeElement===first||document.activeElement===panel)){event.preventDefault();last.focus();}else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first.focus();}}},true);
stopSocket=watchPairing(event=>{if(!event||event.type==='game.updated')load();},status=>{if(opened)$('games-connection').textContent=status;});
poll=setInterval(()=>{if(!document.hidden)load();},5000);window.addEventListener('online',load);document.addEventListener('visibilitychange',()=>{if(!document.hidden)load();});
window.addEventListener('pagehide',()=>{closed=true;resultEffects.dispose();gameResize?.disconnect();cancelAnimationFrame(layoutFrame);stopSocket?.();clearInterval(poll);pending=null;page=null;panel.hidden=true;panel.replaceChildren();$('games-invite-notice').hidden=true;});
$('game-result-dismiss').addEventListener('click',()=>{resultEffects.dismiss();columns[0].focus({preventScroll:true});});
const gameResize=typeof ResizeObserver==='function'?new ResizeObserver(scheduleGameLayout):null;
gameResize?.observe(panel);for(const child of panel.querySelector('.connect-card').children)if(child.id!=='connect-board')gameResize?.observe(child);
window.addEventListener('resize',scheduleGameLayout);window.visualViewport?.addEventListener('resize',scheduleGameLayout);window.visualViewport?.addEventListener('scroll',scheduleGameLayout);
if(location.hash==='#games')open();else load();

// GAME-02: fit all42 cells to the real remaining card area, including browser bars.
export function fitConnectBoard(width,height,padding=8,gap=4){
 if(width<=2*padding+6*gap||height<=2*padding+5*gap)return {width:0,height:0,cell:0};
 const cell=Math.max(0,Math.floor(Math.min((width-2*padding-6*gap)/7,(height-2*padding-5*gap)/6)*10)/10);
 return {width:Math.max(0,7*cell+6*gap+2*padding),height:Math.max(0,6*cell+5*gap+2*padding),cell};
}
function scheduleGameLayout(){if(closed||layoutFrame)return;layoutFrame=requestAnimationFrame(()=>{layoutFrame=0;fitGameView();});}
function fitGameView(){
 const call=document.getElementById('call-panel'),board=$('connect-board');if(!opened||panel.parentElement!==call){call?.style.removeProperty('--game-call-height');call?.style.removeProperty('--game-call-top');board.style.removeProperty('--game-board-width');board.style.removeProperty('--game-board-height');return;}
 const view=window.visualViewport;call.style.setProperty('--game-call-height',(view?.height||innerHeight)+'px');call.style.setProperty('--game-call-top',(view?.offsetTop||0)+'px');
 const card=panel.querySelector('.connect-card'),style=getComputedStyle(card),number=value=>parseFloat(value)||0;
 let height=card.clientHeight-number(style.paddingTop)-number(style.paddingBottom);
 const visible=[...card.children].filter(child=>child!==board&&getComputedStyle(child).display!=='none');
 for(const child of visible){const c=getComputedStyle(child);height-=child.getBoundingClientRect().height+number(c.marginTop)+number(c.marginBottom);}
 height-=number(style.rowGap)*visible.length;
 const width=card.clientWidth-number(style.paddingLeft)-number(style.paddingRight),boardStyle=getComputedStyle(board),fit=fitConnectBoard(width,Math.max(0,height),number(boardStyle.paddingLeft),number(boardStyle.columnGap));
 board.style.setProperty('--game-board-width',fit.width+'px');board.style.setProperty('--game-board-height',fit.height+'px');
}
export function gameResultPresentation(game,selfId){
 if(!game||!['WON','RESIGNED','DRAW'].includes(game.status))return null;
 if(game.status==='DRAW')return {kind:'draw',title:'It’s a Draw!',detail:'A perfect match. Ready for another round?'};
 return game.winnerId===selfId?{kind:'win',title:'You Win!',detail:'A little victory, shared with your favorite person.'}:{kind:'partner',title:'Partner Wins!',detail:'Your person takes this round. Rematch?'};
}
export function createGameResultEffects({panel,title,detail,confetti,document,reduceMotion=()=>false,setTimer=setTimeout,clearTimer=clearTimeout}){
 let previous=null,current=null,timer=null;const dismissed=new Set();
 function clear(){clearTimer(timer);timer=null;confetti.replaceChildren();panel.classList.remove('is-celebrating');}
 return {
  update(game,selfId,visible){
   const result=gameResultPresentation(game,selfId),transition=previous?.id===game?.id&&previous?.status==='ACTIVE'&&!!result;
   previous=game?{id:game.id,status:game.status}:null;current=game?.id||null;
   if(!result||!visible||dismissed.has(current)){clear();panel.hidden=true;return;}
   panel.hidden=false;panel.dataset.result=result.kind;title.textContent=result.title;detail.textContent=result.detail;
   if(transition&&!reduceMotion()){
    clear();panel.classList.add('is-celebrating');
    for(let i=0;i<18;i++){const piece=document.createElement('i');piece.style.setProperty('--piece-x',(7+i*5%88)+'%');piece.style.setProperty('--piece-delay',(i%6*.06)+'s');piece.style.setProperty('--piece-drift',((i%2?1:-1)*(18+i%5*12))+'px');piece.style.setProperty('--piece-color',['#e6b84a','#c94972','#9dd4c8','#c4b6ed'][i%4]);confetti.append(piece);}
    timer=setTimer(clear,2400);
   }
  },
  dismiss(){if(current){dismissed.add(current);if(dismissed.size>40)dismissed.delete(dismissed.values().next().value);}clear();panel.hidden=true;},
  dispose(){clear();dismissed.clear();previous=null;current=null;panel.hidden=true;}
 };
}
