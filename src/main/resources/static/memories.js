import {watchPairing} from '/pairing-socket.js';
const $=s=>document.querySelector(s),$$=s=>document.querySelectorAll(s);
let context,filter='ALL',entries=new Map(),cursor=null,loading=false,again=false,generation=0,pageEpoch=0,identity='',draft=null,detail=null,confirmation=null,stopSocket,poll,started=false;
const labels={ALL:'All',IMAGE:'Photos',VIDEO:'Videos',DOCUMENT:'Documents',FAVORITES:'Favorites',TRASH:'Trash'};
const node=(tag,text,cls)=>{const e=document.createElement(tag);if(text!==undefined)e.textContent=text;if(cls)e.className=cls;return e;};
const size=bytes=>`${(bytes/1000000).toLocaleString(undefined,{maximumFractionDigits:1})} MB`;
const date=value=>new Intl.DateTimeFormat('en-GB',{day:'numeric',month:'short',year:'numeric',timeZone:'UTC'}).format(new Date(`${value}T00:00:00Z`));
function name(id){return context?.members.find(m=>m.id===id)?.displayName||'Your partner';}
function notice(text){$('#memory-notice').textContent=text;$('#memory-notice').hidden=false;}
function error(text){$('#error').textContent=text;$('#error').hidden=false;$('#retry-load').hidden=false;}
async function csrf(){const r=await fetch('/api/csrf',{cache:'no-store',signal:AbortSignal.timeout(10000)});if(!r.ok)throw new Error('Could not prepare this change. Retry.');return r.json();}
async function api(path,method='GET',body,key){
  const headers={};if(method!=='GET'){const c=await csrf();headers[c.headerName]=c.token;if(body!==undefined)headers['Content-Type']='application/json';if(key)headers['Idempotency-Key']=key;}
  const r=await fetch(path,{method,headers,body:body===undefined?undefined:JSON.stringify(body),cache:'no-store',signal:AbortSignal.timeout(30000)});
  if(r.status===401){location.replace('/?expired=1');throw new Error('Please sign in again.');}
  if(r.status===403){location.replace('/space.html');throw new Error('Connect with your partner first.');}
  if(!r.ok){const data=await r.json().catch(()=>({}));const failure=new Error(data.message||'This change could not be confirmed. Check your connection and retry.');failure.code=data.code;throw failure;}
  return r.status===204?null:r.json();
}
async function load(more=false){
  if(loading){if(!more)again=true;return;}loading=true;const epoch=++generation,current=filter;$('#load-more').disabled=true;
  try{
    const page=await api(`/api/memories?filter=${current}${more&&cursor?`&before=${cursor}`:''}`),trash=await api('/api/memories/trash/preview');if(epoch!==generation||current!==filter)return;
    if(!more)entries.clear();for(const entry of page.items)entries.set(entry.id,entry);cursor=page.nextCursor;context.today=page.today;
    render();$('#list-status').textContent=current==='TRASH'?`Trash keeps memories for 3 days.${trash.pending?` ${trash.pending} permanent deletion(s) are being retried.`:''}`:`${entries.size} ${entries.size===1?'memory':'memories'} shown · ${labels[current]}`;
    $('#empty-trash').hidden=current!=='TRASH';$('#empty-trash').disabled=!trash.count;$('#load-more').hidden=!cursor;$('#error').hidden=true;$('#retry-load').hidden=true;
  }catch(failure){error(failure.message);}finally{loading=false;$('#load-more').disabled=false;if(again){again=false;load();}}
}
function render(){
  const next=JSON.stringify([filter,[...entries.values()]]);if(next===identity)return;identity=next;const content=document.createDocumentFragment();
  if(!entries.size){const box=node('div',undefined,'memory-empty');box.append(node('span',filter==='TRASH'?'♧':'♡'),node('h2',filter==='TRASH'?'A little less clutter.':'Your moments belong here.'),node('p',filter==='TRASH'?'Nothing in Trash right now.':'Add a photo, a short video, or a document to keep a memory together.'));content.append(box);}
  for(const item of entries.values()){
    const card=node('article',undefined,'memory-card'),cover=node('button',undefined,'memory-cover');cover.type='button';cover.setAttribute('aria-label',`Open memory ${item.title}`);
    const image=filter==='TRASH'?null:item.assets.find(a=>a.thumbnail);
    if(image){const img=node('img');img.src=image.thumbnail;img.alt='';img.loading='lazy';img.addEventListener('error',()=>{img.hidden=true;});cover.append(img);}
    else{const fallback=node('div',undefined,'file-cover');fallback.append(node('span',filter==='TRASH'?'♧':'▤'),node('small',filter==='TRASH'?'In Trash':'A little something, saved.'));cover.append(fallback);}
    cover.append(node('span',`${item.assets.length} ${item.assets.length===1?'file':'files'}`, 'memory-kind'));cover.addEventListener('click',()=>openDetail(item.id));
    const body=node('div',undefined,'memory-card-body'),top=node('div',undefined,'memory-card-top'),title=node('button',item.title,'memory-card-title');title.type='button';title.addEventListener('click',()=>openDetail(item.id));top.append(title);
    if(filter!=='TRASH'){const favorite=node('button',item.favorite?'♥':'♡','favorite-button');favorite.type='button';favorite.setAttribute('aria-label',`${item.favorite?'Unfavorite':'Favorite'} ${item.title}`);favorite.setAttribute('aria-pressed',String(item.favorite));favorite.addEventListener('click',async()=>{favorite.disabled=true;try{await api(`/api/memories/${item.id}/favorite`,'PUT',{favorite:!item.favorite});await load();}catch(failure){error(failure.message);}finally{favorite.disabled=false;}});top.append(favorite);}
    body.append(top,node('p',`${date(item.memoryDate)} · Saved by ${name(item.createdBy)}`));if(item.caption)body.append(node('p',item.caption,'memory-caption-preview'));
    if(filter==='TRASH')body.append(node('p',`Restore before ${new Intl.DateTimeFormat('en-GB',{dateStyle:'medium',timeStyle:'short',timeZone:'Asia/Yangon'}).format(new Date(item.expiresAt))}`));
    card.append(cover,body);content.append(card);
  }$('#memory-grid').replaceChildren(content);
}
function editorBusy(){return $('#save-memory').dataset.busy==='true';}
function openEditor(memory=null){
  if(!context)return;draft={id:memory?.id,version:memory?.version,key:crypto.randomUUID(),items:(memory?.assets||[]).map(asset=>({asset,existing:true,state:'ready'})),uploading:false};
  $('#memory-form').reset();$('#memory-title').value=memory?.title||'';$('#memory-caption').value=memory?.caption||'';$('#memory-date').value=memory?.memoryDate||context.today;$('#memory-date').max=context.today;$('#editor-title').textContent=memory?'Edit our memory.':'Save a little memory.';$('#form-error').textContent='';$('#reload-editor').hidden=true;$('#save-memory').dataset.busy='false';$('#save-memory').disabled=false;$('#save-memory').textContent='Save memory';renderUploads();$('#memory-editor').showModal();$('#memory-title').focus();
}
async function closeEditor(){
  if(editorBusy())return;const old=draft;draft=null;$('#memory-editor').close();if(old)for(const item of old.items){item.xhr?.abort();if(!item.existing&&item.asset)api(`/api/memory-uploads/${item.asset.id}`,'DELETE').catch(()=>{});}
}
function renderUploads(){
  if(!draft)return;const fragment=document.createDocumentFragment();
  for(const item of draft.items){
    const row=node('div',undefined,'upload-row');if(item.asset?.thumbnail){const img=node('img');img.src=item.asset.thumbnail;img.alt='';row.append(img);}else row.append(node('span','▤','upload-icon'));
    const info=node('div',undefined,'upload-info');info.append(node('strong',item.asset?.name||item.file.name),node('p',`${size(item.asset?.size||item.file.size)} · ${item.error||({ready:'Ready to save',queued:'Waiting to upload',uploading:item.percent===100?'Checking file…':`Uploading… ${item.percent||0}%`,failed:'Upload failed. Retry or remove this file.'}[item.state])}`));
    if(item.state==='uploading'){const progress=node('progress');progress.max=100;progress.value=item.percent||0;progress.setAttribute('aria-label',`Upload progress for ${item.file.name}`);info.append(progress);}
    const actions=node('div',undefined,'upload-actions');if(item.state==='failed'){const retry=node('button','Retry upload');retry.type='button';retry.addEventListener('click',()=>{item.state='queued';item.error='';renderUploads();processUploads();});actions.append(retry);}
    const remove=node('button','Remove');remove.type='button';remove.disabled=editorBusy();remove.setAttribute('aria-label',`Remove ${item.asset?.name||item.file.name}`);remove.addEventListener('click',()=>{item.xhr?.abort();draft.items=draft.items.filter(p=>p!==item);if(item.asset&&!item.existing)api(`/api/memory-uploads/${item.asset.id}`,'DELETE').catch(()=>{});renderUploads();});actions.append(remove);row.append(info,actions);fragment.append(row);
  }$('#upload-list').replaceChildren(fragment);if(!editorBusy())$('#save-memory').disabled=draft.items.some(p=>p.state!=='ready');
}
function upload(item,current){return new Promise(async(resolve,reject)=>{
  try{const c=await csrf();if(draft!==current||!current.items.includes(item))return reject(new Error('Upload cancelled.'));
    const xhr=new XMLHttpRequest();item.xhr=xhr;xhr.open('POST','/api/memory-uploads');xhr.timeout=120000;xhr.setRequestHeader(c.headerName,c.token);xhr.setRequestHeader('Idempotency-Key',item.key);
    xhr.upload.onprogress=event=>{if(event.lengthComputable&&draft===current){const percent=Math.round(event.loaded/event.total*100);if(percent!==item.percent){item.percent=percent;renderUploads();}}};
    xhr.onload=()=>{let data;try{data=JSON.parse(xhr.responseText);}catch{}if(xhr.status===401){location.replace('/?expired=1');reject(new Error('Please sign in again.'));}else if(xhr.status>=200&&xhr.status<300&&data?.id)resolve(data);else reject(new Error(data?.message||'Upload failed. Retry or remove this file.'));};
    xhr.onerror=xhr.ontimeout=()=>reject(new Error('Upload not confirmed. Retry uses the same file identifier.'));xhr.onabort=()=>reject(new Error('Upload cancelled.'));const form=new FormData();form.append('file',item.file);xhr.send(form);
  }catch(failure){reject(failure);}
});}
async function processUploads(){
  const current=draft;if(!current||current.uploading)return;current.uploading=true;
  try{for(const item of current.items){if(draft!==current)break;if(item.state!=='queued')continue;item.state='uploading';item.percent=0;renderUploads();try{const asset=await upload(item,current);if(draft!==current||!current.items.includes(item)){api(`/api/memory-uploads/${asset.id}`,'DELETE').catch(()=>{});continue;}item.asset=asset;item.state='ready';item.error='';}catch(failure){if(current.items.includes(item)){item.state='failed';item.error=failure.message;}}if(draft===current)renderUploads();}}
  finally{current.uploading=false;if(draft===current&&current.items.some(p=>p.state==='queued'))processUploads();}
}
$('#memory-files').addEventListener('change',()=>{
  if(!draft||editorBusy())return;const selected=[...$('#memory-files').files];$('#memory-files').value='';$('#form-error').textContent='';
  if(draft.items.length+selected.length>10){$('#form-error').textContent='A memory can hold up to 10 files in total.';return;}
  for(const file of selected){const ext=file.name.split('.').pop().toLowerCase(),image=['jpg','jpeg','png','webp'].includes(ext),video=ext==='mp4',document=['pdf','docx','xlsx','pptx'].includes(ext);if(!image&&!video&&!document||file.size<1||file.size>(video?100000000:image?10000000:20000000)){$('#form-error').textContent=`${file.name}: choose a supported file within its upload limit.`;return;}}
  for(const file of selected)draft.items.push({file,key:crypto.randomUUID(),state:'queued',existing:false});renderUploads();processUploads();
});
$('#memory-form').addEventListener('submit',async event=>{
  event.preventDefault();if(!draft||editorBusy())return;const current=draft,revision=pageEpoch,title=$('#memory-title').value,caption=$('#memory-caption').value;
  if(!title.trim()||Array.from(title.trim()).length>120||Array.from(caption).length>2000||!$('#memory-date').value||$('#memory-date').value>context.today||!draft.items.length||draft.items.some(i=>i.state!=='ready')){$('#form-error').textContent='Add a title (up to 120 characters), today or an earlier date, and 1–10 ready files. Caption can contain up to 2,000 characters.';return;}
  $('#form-error').textContent='';$('#save-memory').dataset.busy='true';$('#save-memory').disabled=true;$('#save-memory').textContent='Saving…';$('#memory-files').disabled=true;renderUploads();
  try{const saved=await api(current.id?`/api/memories/${current.id}`:'/api/memories',current.id?'PATCH':'POST',{title,caption,memoryDate:$('#memory-date').value,assetIds:current.items.map(i=>i.asset.id),version:current.version},current.id?undefined:current.key);if(revision!==pageEpoch)return;draft=null;$('#memory-editor').close();notice('Your memory is saved.');await load();await openDetail(saved.id,false);}
  catch(failure){$('#form-error').textContent=failure.message;$('#reload-editor').hidden=!['VERSION_CONFLICT','MEMORY_NOT_FOUND','IDEMPOTENCY_CONFLICT'].includes(failure.code);}
  finally{$('#save-memory').dataset.busy='false';$('#save-memory').disabled=false;$('#save-memory').textContent='Save memory';$('#memory-files').disabled=false;if(draft===current)renderUploads();}
});
async function openDetail(id,trash=filter==='TRASH'){
  try{detail=await api(`/api/memories/${id}?trash=${trash}`);$('#detail-title').textContent=detail.title;$('#detail-date').textContent=date(detail.memoryDate);$('#detail-caption').textContent=detail.caption;$('#detail-audit').textContent=`Saved by ${name(detail.createdBy)} · Last updated by ${name(detail.updatedBy)}`;$('#detail-state').textContent=trash?'IN TRASH · RESTORE WITHIN 3 DAYS':'OUR LITTLE MEMORY';$('#detail-error').textContent='';
    const content=document.createDocumentFragment();for(const asset of detail.assets){const box=node('article',undefined,'detail-asset');if(!trash&&asset.kind==='IMAGE'){const image=node('img');image.src=asset.url;image.alt=asset.name;image.loading='lazy';box.append(image);}else if(!trash&&asset.kind==='VIDEO'){const video=node('video');video.controls=true;video.preload='metadata';video.playsInline=true;video.src=asset.url;video.setAttribute('aria-label',asset.name);box.append(video);}box.append(node('p',`${asset.name} · ${asset.kind.toLowerCase()} · ${size(asset.size)}`));if(!trash){const link=node('a',`Download ${asset.name}`);link.href=asset.url+'?download=true';link.target='_blank';link.rel='noopener';box.append(link);}content.append(box);}$('#detail-gallery').replaceChildren(content);
    for(const id of ['detail-edit','detail-delete','detail-favorite'])$('#'+id).hidden=trash;$('#detail-restore').hidden=!trash;$('#detail-favorite').textContent=detail.favorite?'♥ Favorited':'♡ Favorite';if(!$('#memory-detail').open)$('#memory-detail').showModal();
  }catch(failure){if($('#memory-detail').open)$('#detail-error').textContent=failure.message;else error(failure.message);}
}
function closeDetail(){$('#memory-detail').close();for(const video of $$('#detail-gallery video')){video.pause();video.removeAttribute('src');video.load();}$('#detail-gallery').replaceChildren();detail=null;}
function confirm(title,message,action,label){confirmation=action;$('#confirm-title').textContent=title;$('#confirm-message').textContent=message;$('#confirm-error').textContent='';$('#review-trash').hidden=true;$('#confirm-action').textContent=label;$('#confirm-action').disabled=false;$('#memory-confirm').showModal();$('#confirm-cancel').focus();}
async function emptyTrash(){try{const preview=await api('/api/memories/trash/preview');if(!preview.count){notice('Trash is empty.');return;}if($('#memory-confirm').open)$('#memory-confirm').close();confirm('Empty Trash permanently?',`${preview.count} ${preview.count===1?'memory':'memories'} and every attached file in this Trash batch will be permanently deleted now. This cannot be undone.`,async()=>{const result=await api('/api/memories/trash/empty','POST',{snapshot:preview.snapshot,confirmed:true});notice(result.pending?`${result.pending} deletion(s) need another storage retry. Restore is no longer available.`:'Trash permanently emptied.');closeDetail();await load();},'Empty Trash permanently');}catch(failure){error(failure.message);}}
$('#confirm-action').addEventListener('click',async()=>{if(!confirmation||$('#confirm-action').disabled)return;$('#confirm-action').disabled=true;try{await confirmation();$('#memory-confirm').close();confirmation=null;}catch(failure){$('#confirm-error').textContent=failure.message;$('#review-trash').hidden=failure.code!=='TRASH_CHANGED';}finally{$('#confirm-action').disabled=false;}});
$('#confirm-cancel').addEventListener('click',()=>{if(!$('#confirm-action').disabled){$('#memory-confirm').close();confirmation=null;}});$('#memory-confirm').addEventListener('cancel',event=>{if($('#confirm-action').disabled)event.preventDefault();});$('#review-trash').addEventListener('click',emptyTrash);$('#empty-trash').addEventListener('click',emptyTrash);
$('#detail-delete').addEventListener('click',()=>{const current=detail;confirm('Move this memory to Trash?',`“${current.title}” will leave your shared memories. You can restore it for 3 days, unless Trash is emptied sooner.`,async()=>{await api(`/api/memories/${current.id}?version=${current.version}`,'DELETE');closeDetail();notice('Memory moved to Trash for 3 days.');await load();},'Move to Trash');});
$('#detail-restore').addEventListener('click',async()=>{const button=$('#detail-restore');button.disabled=true;try{const restored=await api(`/api/memories/${detail.id}/restore`,'POST',{version:detail.version});notice('Memory restored.');await load();await openDetail(restored.id,false);}catch(failure){$('#detail-error').textContent=failure.message;}finally{button.disabled=false;}});
$('#detail-edit').addEventListener('click',()=>{const current=detail;closeDetail();openEditor(current);});$('#detail-favorite').addEventListener('click',async()=>{const button=$('#detail-favorite');button.disabled=true;try{await api(`/api/memories/${detail.id}/favorite`,'PUT',{favorite:!detail.favorite});await openDetail(detail.id,false);await load();}catch(failure){$('#detail-error').textContent=failure.message;}finally{button.disabled=false;}});
$('#detail-close').addEventListener('click',closeDetail);$('#memory-detail').addEventListener('close',()=>{for(const video of $$('#detail-gallery video'))video.pause();});$('#detail-reload').addEventListener('click',()=>openDetail(detail.id,detail.state==='TRASHED'));
$('#editor-close').addEventListener('click',closeEditor);$('#editor-cancel').addEventListener('click',closeEditor);$('#memory-editor').addEventListener('cancel',event=>{event.preventDefault();closeEditor();});$('#reload-editor').addEventListener('click',async()=>{const id=draft?.id;await closeEditor();if(id)try{openEditor(await api(`/api/memories/${id}`));}catch(failure){error(failure.message);}await load();});
$('#add-memory').addEventListener('click',()=>openEditor());for(const button of $$('[data-filter]'))button.addEventListener('click',()=>{filter=button.dataset.filter;for(const b of $$('[data-filter]'))b.setAttribute('aria-pressed',String(b===button));identity='';cursor=null;load();});$('#load-more').addEventListener('click',()=>load(true));$('#retry-load').addEventListener('click',()=>load());
$('#logout').addEventListener('click',async()=>{$('#logout').disabled=true;try{await api('/api/auth/logout','POST');stopSocket?.();location.replace('/?signedout=1');}catch(failure){error(failure.message);$('#logout').disabled=false;}});
async function start(){
  if(started)return;started=true;const revision=pageEpoch;
  try{const loaded=await api('/api/home');if(revision!==pageEpoch)return;context=loaded;$('#main').hidden=false;$('#couple-names').textContent=context.members.map(m=>m.displayName).join(' & ');$('#initials').replaceChildren(...context.members.map(m=>node('span',Array.from(m.displayName)[0])));context.today=context.calendar.today;$('#add-memory').disabled=false;await load();if(revision!==pageEpoch)return;
    if(/^#[a-f0-9-]{36}$/i.test(location.hash))await openDetail(location.hash.slice(1),false);
    stopSocket=watchPairing(update=>{if(update?.notification)notice(update.notification);load();},text=>$('#connection').textContent=text);poll=setInterval(()=>{if(!document.hidden)load();},15000);
  }catch(failure){if(revision===pageEpoch){error(failure.message);started=false;}}
}
window.addEventListener('pageshow',start);window.addEventListener('pagehide',()=>{pageEpoch++;generation++;stopSocket?.();clearInterval(poll);started=false;closeDetail();if(draft)for(const item of draft.items)item.xhr?.abort();draft=null;context=null;entries.clear();identity='';$('#memory-grid').replaceChildren();$('#main').hidden=true;$('#memory-editor').close();$('#memory-confirm').close();$('#memory-form').reset();});document.addEventListener('visibilitychange',()=>{if(!document.hidden&&context)load();});start();
