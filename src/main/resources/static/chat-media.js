// CHAT-05: inline media, local recording preview, cancellable server send intents.
export const MEDIA_LIMITS={image:10000000,document:20000000,video:100000000,voice:10000000,voiceMs:300000};

export function createVoiceCapture({getUserMedia,Recorder,now=Date.now,setInterval,clearInterval,onState,onError}){
 let generation=0,stream,recorder,timer,chunks=[],bytes=0,startedAt=0;
 const release=()=>{clearInterval(timer);timer=null;for(const track of stream?.getTracks()||[])track.stop();stream=null;};
 const result={
  async start(){const revision=++generation;onState({state:'requesting'});
   try{const media=await getUserMedia({audio:true});if(revision!==generation){for(const track of media.getTracks())track.stop();return;}stream=media;
    const mime=['audio/webm;codecs=opus','audio/mp4','audio/ogg;codecs=opus','audio/webm'].find(type=>Recorder.isTypeSupported?.(type));
    recorder=new Recorder(media,mime?{mimeType:mime}:{});const current=recorder;chunks=[];bytes=0;startedAt=now();
    current.ondataavailable=event=>{if(revision!==generation||!event.data?.size)return;bytes+=event.data.size;if(bytes>MEDIA_LIMITS.voice){result.cancel();onError('Recording exceeded 10 MB. Record a shorter message.');return;}chunks.push(event.data);};
    current.onerror=()=>{if(revision===generation){result.cancel();onError('The recording stopped unexpectedly. Try again.');}};
    current.onstop=()=>{release();if(revision!==generation)return;const blob=new Blob(chunks,{type:current.mimeType||mime||'audio/webm'});chunks=[];if(!blob.size){onState({state:'idle'});onError('No audio was recorded. Try again.');return;}onState({state:'preview',blob,seconds:Math.min(300,(now()-startedAt)/1000)});};
    current.start(250);onState({state:'recording',seconds:0});timer=setInterval(()=>{if(revision!==generation)return;const elapsed=now()-startedAt;onState({state:'recording',seconds:elapsed/1000});if(elapsed>=MEDIA_LIMITS.voiceMs)result.stop();},250);
   }catch(error){release();if(revision===generation){onState({state:'idle'});onError(error.name==='NotAllowedError'?'Microphone permission was denied. Allow it to record a voice message.':'Microphone is busy or unavailable. Try again.');}}
  },
  stop(){if(recorder&&recorder.state!=='inactive'){recorder.stop();release();}},
  cancel(){generation++;if(recorder){recorder.onstop=null;recorder.ondataavailable=null;try{if(recorder.state!=='inactive')recorder.stop();}catch{}}recorder=null;chunks=[];bytes=0;release();onState({state:'idle'});}
 };return result;
}

export function validateChatFile(file){
 const extension=file.name.split('.').at(-1).toLowerCase();let kind,limit;
 if(['jpg','jpeg','png','webp'].includes(extension)){kind='IMAGE';limit=MEDIA_LIMITS.image;}
 else if(extension==='mp4'){kind='VIDEO';limit=MEDIA_LIMITS.video;}
 else if(['pdf','docx','xlsx','pptx'].includes(extension)){kind='DOCUMENT';limit=MEDIA_LIMITS.document;}
 else throw new Error('Choose JPG, PNG, WebP, MP4, PDF, DOCX, XLSX or PPTX.');
 if(!file.size||file.size>limit)throw new Error(`Use a ${kind==='VIDEO'?'video up to 100 MB and 3 minutes':kind==='IMAGE'?'photo up to 10 MB':'document up to 20 MB'}.`);
 return kind;
}

export function mountChatMedia({csrf,queue,onError,onControls}){
 const $=id=>document.getElementById(id);let stage='idle',file=null,kind=null,url=null,disposed=false;const jobs=new Map();
 const displayTime=seconds=>`${Math.floor(seconds/60)}:${String(Math.floor(seconds%60)).padStart(2,'0')}`;
 function clearPreview(revoke=true){for(const player of $('chat-media-preview').querySelectorAll('audio,video')){player.pause();player.removeAttribute('src');player.load();}if(revoke&&url)URL.revokeObjectURL(url);url=null;$('chat-media-preview').replaceChildren();}
 function controls(){$('chat-media-draft').hidden=stage==='idle';$('media-stop').hidden=stage!=='recording';$('media-cancel').textContent=['requesting','recording'].includes(stage)?'Cancel recording':'Cancel';onControls();}
 function reset(keepUrl=false){capture.cancel();clearPreview(!keepUrl);file=null;kind=null;stage='idle';$('chat-media-status').textContent='';$('chat-file').value='';controls();}
 function preview(selected,selectedKind){clearPreview();file=selected;kind=selectedKind;stage='draft';url=URL.createObjectURL(file);
  if(kind!=='DOCUMENT'){const element=document.createElement(kind==='IMAGE'?'img':kind==='VIDEO'?'video':'audio');element.src=url;if(kind==='IMAGE')element.alt='Selected photo';else{element.controls=true;element.preload='metadata';if(kind==='VIDEO')element.playsInline=true;}$('chat-media-preview').append(element);}
  if(kind==='DOCUMENT'){const label=document.createElement('strong');label.textContent=file.name;$('chat-media-preview').append(label);}$('chat-media-status').textContent='Ready to send';controls();
 }
 const capture=createVoiceCapture({getUserMedia:constraints=>navigator.mediaDevices.getUserMedia(constraints),Recorder:window.MediaRecorder,now:Date.now,setInterval,clearInterval,onError,onState:value=>{
  if(disposed)return;if(value.state==='idle'){if(['requesting','recording'].includes(stage)){stage='idle';$('chat-media-status').textContent='';controls();}return;}
  if(value.state==='preview'){const type=value.blob.type,ext=type.startsWith('audio/mp4')?'m4a':type.startsWith('audio/ogg')?'ogg':'webm';preview(new File([value.blob],`Voice message.${ext}`,{type}), 'VOICE');return;}
  stage=value.state;$('chat-media-preview').replaceChildren();$('chat-media-status').textContent=stage==='requesting'?'Waiting for microphone permission…':`Recording ${displayTime(value.seconds||0)} / 5:00`;controls();
 }});
 function abort(key){const job=jobs.get(key);if(job){job.revision++;job.xhr?.abort();job.xhr=null;job.promise=null;}}
 function release(key){const job=jobs.get(key);if(!job)return;abort(key);URL.revokeObjectURL(job.url);jobs.delete(key);}
 async function cancelServer(key){release(key);const token=await csrf();const response=await fetch(`/api/chat/uploads/intents/${key}`,{method:'DELETE',headers:{[token.headerName]:token.token},signal:AbortSignal.timeout(20000)});if(!response.ok)throw new Error('Cancellation pending. Reconnect to finish cancelling.');return response.json();}
 async function upload(item){
  const job=jobs.get(item.clientMessageId);if(!job)throw new Error('This local file is no longer available. Cancel and select it again.');if(job.promise)return job.promise;
  const revision=job.revision;job.promise=(async()=>{const token=await csrf();if(revision!==job.revision)throw new DOMException('Cancelled','AbortError');const remaining=item.deadlineAt-Date.now();if(remaining<=0)throw new Error('Send timed out. Retry or cancel.');
   return new Promise((resolve,reject)=>{const request=new XMLHttpRequest();job.xhr=request;request.open('POST','/api/chat/uploads'+(job.kind==='VOICE'?'?voice=true':''));request.setRequestHeader(token.headerName,token.token);request.setRequestHeader('Idempotency-Key',item.clientMessageId);request.responseType='json';request.timeout=remaining;
    request.onload=()=>{job.xhr=null;if(request.status>=200&&request.status<300)resolve(request.response);else reject(new Error(request.response?.message||'Upload failed. Retry or cancel.'));};request.onerror=()=>reject(new Error('Connection failed. Retry or cancel.'));request.ontimeout=()=>reject(new Error('Send timed out. Retry or cancel.'));request.onabort=()=>reject(new DOMException('Cancelled','AbortError'));
    const data=new FormData();data.append('file',job.file);request.send(data);
   });})();try{return await job.promise;}finally{if(revision===job.revision)job.promise=null;}
 }
 $('attach-open').addEventListener('click',()=>$('chat-file').click());$('chat-file').addEventListener('change',()=>{const selected=$('chat-file').files[0];if(!selected)return;try{preview(selected,validateChatFile(selected));}catch(error){$('chat-file').value='';onError(error.message);}});
 $('voice-record').addEventListener('click',()=>{if(stage!=='idle')return;if(!window.isSecureContext||!window.MediaRecorder||!navigator.mediaDevices?.getUserMedia){onError('Voice recording needs HTTPS and a supported browser.');return;}capture.start();});
 $('media-stop').addEventListener('click',()=>capture.stop());$('media-cancel').addEventListener('click',()=>reset());
 const observer=new MutationObserver(()=>{if(document.body.classList.contains('call-view-open')&&stage==='recording')capture.stop();});observer.observe(document.body,{attributes:true,attributeFilter:['class']});
 const api={hasDraft:()=>stage!=='idle',busy:()=>['requesting','recording'].includes(stage),ready:()=>stage==='draft',upload,abort,
  send(body,replyToId){if(stage!=='draft'||!file)return;if(Array.from(body).length>4000){onError('Use up to 4,000 characters.');return;}const key=crypto.randomUUID();const job={file,kind,url,revision:0,xhr:null,promise:null};jobs.set(key,job);reset(true);
   try{queue({clientMessageId:key,body,replyToId,type:'FILE',localPreview:job.url,localKind:job.kind,localName:job.file.name});}catch(error){release(key);onError(error.message);}
  },
  confirmed:release,failed:abort,cancelIntent:cancelServer,
  dispose(){if(disposed)return;reset();disposed=true;observer.disconnect();for(const key of [...jobs.keys()])release(key);}
 };controls();return api;
}
