import {watchPairing} from '/pairing-socket.js';
const $=s=>document.querySelector(s), $$=s=>document.querySelectorAll(s);
let home,dates,page=0,editing=null,loading=false,refreshAgain=false,midnightTimer,poll,stopSocket,epoch=0,renderedDates='';
let memoryHomeBusy=false,memoryHomeIdentity='';
async function loadHomeMemories(){
  if(memoryHomeBusy)return;memoryHomeBusy=true;const revision=epoch;
  try{const result=await api('/api/memories');if(revision!==epoch)return;const items=result.items.slice(0,3),key=JSON.stringify(items);if(key===memoryHomeIdentity)return;memoryHomeIdentity=key;
    const fragment=document.createDocumentFragment();if(!items.length){const box=document.createElement('div');box.className='empty-card';const h=document.createElement('h3');h.textContent='A moment worth keeping.';const link=document.createElement('a');link.href='/memories.html';link.textContent='Save your first memory →';box.append(h,link);fragment.append(box);}
    for(const item of items){const link=document.createElement('a');link.href=`/memories.html#${item.id}`;const photo=item.assets.find(a=>a.thumbnail);if(photo){const img=document.createElement('img');img.src=photo.thumbnail;img.alt='';img.loading='lazy';link.append(img);}const title=document.createElement('strong');title.textContent=item.title;const day=document.createElement('p');day.textContent=pretty(item.memoryDate);link.append(title,day);fragment.append(link);}$('#home-memories').replaceChildren(fragment);
  }catch{if(revision===epoch){$('#home-memories').textContent='Memories could not be refreshed. Open Memories to try again.';memoryHomeIdentity='';}}
  finally{memoryHomeBusy=false;}
}
const pretty=date=>new Intl.DateTimeFormat('en-GB',{day:'numeric',month:'long',year:'numeric',timeZone:'UTC'}).format(new Date(`${date}T00:00:00Z`));
const dayLabel=days=>days===0?'Today':days===1?'Tomorrow':`In ${days.toLocaleString()} days`;
async function api(path,method='GET',body,key){
  const headers={};
  if(method!=='GET'){
    const c=await fetch('/api/csrf',{cache:'no-store',signal:AbortSignal.timeout(10000)});if(!c.ok)throw new Error('Could not prepare your save. Try again.');
    const csrf=await c.json();headers[csrf.headerName]=csrf.token;
    if(body!==undefined)headers['Content-Type']='application/json';if(key)headers['Idempotency-Key']=key;
  }
  const response=await fetch(path,{method,headers,cache:'no-store',body:body===undefined?undefined:JSON.stringify(body),signal:AbortSignal.timeout(10000)});
  if(response.status===401){location.replace('/?expired=1');throw new Error('Please sign in again.');}
  if(response.status===403){location.replace('/space.html');throw new Error('Refresh your session to continue.');}
  if(!response.ok){const error=await response.json().catch(()=>({message:'Check your connection and try again.'}));const failure=new Error(error.message);failure.code=error.code;failure.fields=error.fieldErrors;throw failure;}
  return response.status===204?null:response.json();
}
function route(){
  const view=['dates','profile'].includes(location.hash.slice(1))?location.hash.slice(1):'home';
  for(const item of ['home','dates','profile'])$(`#view-${item}`).hidden=item!==view;
  for(const link of $$('[data-view]')){if(link.dataset.view===view)link.setAttribute('aria-current','page');else link.removeAttribute('aria-current');}
  $('#page-title').textContent={home:'Your favorite place is a person.',dates:'Our dates.',profile:'Your own little corner.'}[view];
  if(home)$('#welcome').textContent=`${pretty(home.calendar.today)} · Welcome home, ${home.self.displayName}`;
}
function render(){
  loadHomeMemories();
  for(const node of $$('[data-day]'))node.textContent=home.calendar.dayNumber.toLocaleString();
  for(const node of $$('[data-start]'))node.textContent=pretty(home.relationshipDate);
  for(const node of $$('[data-anniversary]'))node.textContent=pretty(home.calendar.anniversary);
  for(const node of $$('[data-anniversary-count]'))node.textContent=dayLabel(home.calendar.daysUntilAnniversary);
  $('#couple-names').textContent=home.members.map(m=>m.displayName).join(' & ');
  $('#initials').replaceChildren(...home.members.map(m=>{const span=document.createElement('span');span.textContent=Array.from(m.displayName)[0];return span;}));
  $('#profile-initial').textContent=Array.from(home.self.displayName)[0];$('#profile-name').textContent=home.self.displayName;$('#profile-email').textContent=home.self.email;
  $('#next-title').textContent=home.nextEvent?.title??'No special dates yet';
  $('#next-detail').textContent=home.nextEvent?`${pretty(home.nextEvent.nextDate)} · ${dayLabel(home.nextEvent.daysUntil)}`:'Make room for your next little celebration.';
  for(const badge of $$('[data-chat-unread]')){badge.textContent=home.unreadMessages>99?'99+':String(home.unreadMessages||0);badge.hidden=!home.unreadMessages;}
  // Preserve keyboard focus and row controls on background polls with unchanged data.
  const datesIdentity=JSON.stringify([page,dates.items]);
  if(datesIdentity!==renderedDates){
  renderedDates=datesIdentity;$('#date-list').replaceChildren();
  if(!dates.items.length){const card=document.createElement('div');card.className='empty-card';const h=document.createElement('h3');h.textContent=page?'No more dates on this page.':'Your next little celebration starts here.';const p=document.createElement('p');p.textContent='Add a birthday, an anniversary, or a day that means something to you both.';card.append(h,p);$('#date-list').append(card);}
  for(const date of dates.items){
    const card=document.createElement('article');card.className='card event-card';const badge=document.createElement('div');badge.className='event-badge';badge.textContent=date.upcoming?dayLabel(date.daysUntil):'A past memory';
    const main=document.createElement('div');main.className='event-main';const title=document.createElement('h3');title.textContent=date.title;const detail=document.createElement('p');detail.textContent=`${pretty(date.nextDate)}${date.repeatYearly?' · Every year':''}`;main.append(title,detail);
    const actions=document.createElement('div');actions.className='event-actions';
    for(const [label,mode] of [['Edit','event'],['Delete','delete']]){const b=document.createElement('button');b.type='button';b.className='secondary';b.textContent=label;b.setAttribute('aria-label',`${label} ${date.title}`);b.addEventListener('click',()=>openEditor(mode,date));actions.append(b);}
    card.append(badge,main,actions);$('#date-list').append(card);
  }
  }
  $('#previous').disabled=page===0;$('#next').disabled=!dates.hasMore;$('#page-label').textContent=`Page ${page+1}`;
  clearTimeout(midnightTimer);midnightTimer=setTimeout(()=>load(false),Math.min(2147483647,Math.max(1000,Date.parse(home.calendar.nextMidnight)-Date.now()+250)));
  route();$('#content').hidden=false;
}
async function load(initial=false){
  if(loading){refreshAgain=true;return;}loading=true;const generation=epoch;
  if(initial){$('#content').hidden=true;$('#welcome').textContent='Opening your shared space…';}
  try{const [h,d]=await Promise.all([api('/api/home'),api(`/api/dates?page=${page}`)]);if(generation!==epoch)return;home=h;dates=d;render();$('#error').hidden=true;$('#retry').hidden=true;}
  catch(error){$('#error').textContent=home?'Your saved view may be out of date. Check your connection and try again.':error.message;$('#error').hidden=false;$('#retry').hidden=false;}
  finally{loading=false;if(refreshAgain){refreshAgain=false;load(false);}}
}
function openEditor(mode,event){
  if(!home)return;
  editing={mode,event,key:crypto.randomUUID(),version:mode==='profile'?home.self.version:mode==='relationship'?home.version:event?.version};
  $('#editor-form').reset();$('#form-error').hidden=true;$('#reload-latest').hidden=true;$('#save').disabled=false;$('#save').textContent=mode==='delete'?'Delete date':'Save';
  $('#editor-title').textContent={profile:'A name that feels like you.',relationship:'The day it all began.',event:event?'Edit our special day.':'A day worth keeping.',delete:'Delete this special day?'}[mode];
  for(const [id,show] of [['name-field',mode==='profile'],['title-field',mode==='event'],['date-field',mode==='event'||mode==='relationship'],['repeat-field',mode==='event'],['confirm-field',mode==='relationship'],['delete-message',mode==='delete'],['reminder-note',mode==='event']])$(`#${id}`).hidden=!show;
  $('#display-name').value=home.self.displayName;$('#event-title').value=event?.title??'';$('#event-date').value=mode==='relationship'?home.relationshipDate:event?.eventDate??home.calendar.today;$('#event-date').max=mode==='relationship'?home.calendar.today:'9999-12-31';$('#repeat-yearly').checked=event?.repeatYearly??false;$('#date-label').textContent=mode==='relationship'?'Shared relationship start date':'Date';
  $('#delete-message').textContent=`“${event?.title??''}” will be removed for both of you. This cannot be undone.`;
  for(const node of $$('#editor input'))node.removeAttribute('aria-invalid');$('#editor').showModal();
}
function closeEditor(){if($('#save').disabled)return;$('#editor').close();editing=null;}
$('#editor-form').addEventListener('submit',async event=>{
  event.preventDefault();if(!editing||$('#save').disabled)return;$('#form-error').hidden=true;
  for(const node of $$('#editor input'))node.removeAttribute('aria-invalid');
  const {mode,event:date,version,key}=editing;let path,method,body;
  if(mode==='profile'){path='/api/profile';method='PATCH';body={displayName:$('#display-name').value,version};}
  else if(mode==='relationship'){path='/api/relationship-date';method='PATCH';body={relationshipDate:$('#event-date').value||null,version,confirmed:$('#confirm-change').checked};}
  else if(mode==='delete'){path=`/api/dates/${date.id}?version=${version}`;method='DELETE';}
  else{path=date?`/api/dates/${date.id}`:'/api/dates';method=date?'PATCH':'POST';body={title:$('#event-title').value,eventDate:$('#event-date').value||null,repeatYearly:$('#repeat-yearly').checked,version};}
  $('#save').disabled=true;$('#save').textContent='Saving…';
  try{await api(path,method,body,mode==='event'&&!date?key:undefined);$('#editor').close();editing=null;$('#status').textContent=mode==='delete'?'Special date deleted.':'Your changes are saved.';await load(false);}
  catch(error){$('#form-error').textContent=error.message||'We could not confirm this save. Check your connection. Retrying the same new date is safe.';$('#form-error').hidden=false;
    for(const [field,id] of Object.entries({displayName:'display-name',title:'event-title',eventDate:'event-date',relationshipDate:'event-date'}))if(error.fields?.[field])$(`#${id}`).setAttribute('aria-invalid','true');
    $('#reload-latest').hidden=!['VERSION_CONFLICT','DATE_NOT_FOUND','IDEMPOTENCY_CONFLICT'].includes(error.code);$('#form-error').scrollIntoView({block:'nearest'});
  }finally{$('#save').disabled=false;$('#save').textContent=mode==='delete'?'Delete date':'Save';}
});
$('#close-editor').addEventListener('click',closeEditor);$('#cancel-editor').addEventListener('click',closeEditor);$('#editor').addEventListener('cancel',event=>{if($('#save').disabled)event.preventDefault();});
$('#reload-latest').addEventListener('click',()=>{closeEditor();load(false);});
$('#edit-profile').addEventListener('click',()=>openEditor('profile'));$('#edit-relationship').addEventListener('click',()=>openEditor('relationship'));$('#add-date').addEventListener('click',()=>openEditor('event'));
$('#previous').addEventListener('click',()=>{page=Math.max(0,page-1);load(false);});$('#next').addEventListener('click',()=>{page++;load(false);});$('#retry').addEventListener('click',()=>load(!home));
$('#logout').addEventListener('click',async()=>{$('#logout').disabled=true;try{await api('/api/auth/logout','POST');location.replace('/?signedout=1');}catch{$('#error').textContent='Could not confirm sign out. Check your connection and try again.';$('#error').hidden=false;$('#logout').disabled=false;}});
window.addEventListener('hashchange',route);
window.addEventListener('pageshow',()=>{route();load(true);stopSocket?.();stopSocket=watchPairing(()=>load(false),status=>$('#connection').textContent=status);clearInterval(poll);poll=setInterval(()=>{if(!document.hidden)load(false);},15000);});
document.addEventListener('visibilitychange',()=>{if(!document.hidden)load(false);});
window.addEventListener('pagehide',()=>{epoch++;stopSocket?.();clearInterval(poll);clearTimeout(midnightTimer);$('#content').hidden=true;$('#editor').close();home=null;dates=null;$('#couple-names').textContent='';$('#initials').replaceChildren();});
