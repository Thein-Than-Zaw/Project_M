import {watchPairing} from '/pairing-socket.js';

// GAME-01: one server-authoritative couple board; same-document view keeps calls alive.
const template=document.createElement('template');template.innerHTML=`
<button id="games-invite-notice" class="games-invite-notice" type="button" hidden>♡ Your person invited you to play · Open Games</button>
<section id="games-panel" class="games-panel" role="dialog" aria-modal="true" aria-labelledby="games-title" tabindex="-1" hidden>
 <div class="games-shell"><header class="games-heading"><div><span class="eyebrow">A LITTLE PLAY, JUST US</span><h2 id="games-title">Games</h2></div><button id="games-close" class="secondary" type="button" aria-label="Close Games">×</button></header>
 <article class="connect-card"><div class="connect-title"><div><h3>Connect 4</h3><p>Four in a Row · 7 × 6</p></div><span class="connect-mark" aria-hidden="true">●●●●</span></div>
 <p id="games-connection" class="games-connection" role="status">Opening your shared board…</p><p id="games-error" class="field-error" role="alert"></p>
 <div id="game-players" class="game-players"></div><p id="game-status" class="game-status" role="status" aria-live="polite"></p>
 <div id="connect-board" class="connect-board" role="group" aria-label="Connect 4 board, columns 1 to 7"></div>
 <div id="game-actions" class="game-actions"><button id="game-invite" class="primary" type="button">Invite to play</button><button id="game-accept" class="primary" type="button" hidden>Accept &amp; play</button><button id="game-decline" class="secondary" type="button" hidden>Decline</button><button id="game-cancel" class="secondary" type="button" hidden>Cancel invitation</button><button id="game-resign" class="text-button" type="button" hidden>Resign</button><button id="game-retry" class="secondary" type="button" hidden>Retry / check saved action</button></div>
 <div id="game-resign-confirm" class="game-resign-confirm" hidden><p>Resign this game? Your person will win.</p><button id="game-resign-yes" class="primary" type="button">Resign game</button><button id="game-resign-no" class="secondary" type="button">Keep playing</button></div>
 <p class="game-help">Take turns dropping a disc. Connect four across, down or diagonally. Red goes first. No timer — enjoy your time together.</p>
 </article><p class="games-call-note">Your call stays connected while you play. Closing Games keeps your board saved.</p></div>
</section>`;document.body.append(template.content.cloneNode(true));
const $=id=>document.getElementById(id),panel=$('games-panel'),columns=[];
let page=null,pending=null,busy=false,loading=false,reload=false,closed=false,opened=false,previousFocus,stopSocket,poll,denied=false;
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
 $('game-status').textContent=gameStatus(game,page?.selfId,person);
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
 const invited=!!(game?.status==='WAITING'&&game.inviterId!==page?.selfId);$('games-invite-notice').hidden=!invited||opened;window.dispatchEvent(new CustomEvent('game-invitation',{detail:{invited}}));
}
function embed(){const call=document.getElementById('call-panel');if(call&&!call.hidden&&call.dataset.running==='true'){if(panel.parentElement!==call)call.append(panel);panel.inert=false;call.dataset.gamesOpen='true';panel.setAttribute('aria-modal','false');}else{if(call)delete call.dataset.gamesOpen;if(panel.parentElement!==document.body)document.body.append(panel);panel.inert=false;panel.setAttribute('aria-modal','true');}}
function open(){if(closed)return;previousFocus=document.activeElement;window.dispatchEvent(new Event('games-opening'));opened=true;panel.hidden=false;embed();$('games-invite-notice').hidden=true;panel.focus({preventScroll:true});load();}
function close(){if(closed)return;opened=false;panel.hidden=true;const call=document.getElementById('call-panel');if(call)delete call.dataset.gamesOpen;document.body.append(panel);draw();previousFocus?.focus({preventScroll:true});}
for(const button of document.querySelectorAll('[data-open-games]'))button.addEventListener('click',event=>{event.preventDefault();open();});
$('games-invite-notice').addEventListener('click',open);$('games-close').addEventListener('click',close);
for(const op of ['invite','accept','decline','cancel'])$('game-'+op).addEventListener('click',()=>act(op));
$('game-resign').addEventListener('click',()=>{$('game-resign-confirm').hidden=false;$('game-resign-yes').focus({preventScroll:true});});$('game-resign-no').addEventListener('click',()=>{$('game-resign-confirm').hidden=true;});$('game-resign-yes').addEventListener('click',()=>act('resign'));$('game-retry').addEventListener('click',()=>{denied=false;pending?act('retry'):load();});
window.addEventListener('open-games',open);window.addEventListener('call-view-shown',()=>{if(opened)embed();});window.addEventListener('call-view-state',()=>{if(opened)embed();});window.addEventListener('call-view-hiding',()=>{if(opened)close();});
document.addEventListener('keydown',event=>{if(!opened)return;if(event.key==='Escape'){event.preventDefault();event.stopImmediatePropagation();close();}else if(event.key==='Tab'&&panel.parentElement===document.body){const focusable=[...panel.querySelectorAll('button')].filter(b=>!b.disabled&&b.getClientRects().length);if(!focusable.length)return;const first=focusable[0],last=focusable.at(-1);if(event.shiftKey&&(document.activeElement===first||document.activeElement===panel)){event.preventDefault();last.focus();}else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first.focus();}}},true);
stopSocket=watchPairing(event=>{if(!event||event.type==='game.updated')load();},status=>{if(opened)$('games-connection').textContent=status;});
poll=setInterval(()=>{if(!document.hidden)load();},5000);window.addEventListener('online',load);document.addEventListener('visibilitychange',()=>{if(!document.hidden)load();});
window.addEventListener('pagehide',()=>{closed=true;stopSocket?.();clearInterval(poll);pending=null;page=null;panel.hidden=true;panel.replaceChildren();$('games-invite-notice').hidden=true;});
if(location.hash==='#games')open();else load();
