import {watchPairing} from '/pairing-socket.js';
const $=selector=>document.querySelector(selector);
let busy=false,checking=false,current,preview,joinIntent,stopSocket,poll;
function error(message){$('#account-error').textContent=message;$('#account-error').hidden=false;$('#account-error').focus();}
function clearError(){$('#account-error').hidden=true;$('#retry').hidden=true;}
async function request(path,body,key){
  let headers={},method='GET';
  if(body!==undefined){const response=await fetch('/api/csrf',{cache:'no-store',signal:AbortSignal.timeout(10000)});if(!response.ok)throw new Error('Could not verify your session. Refresh and try again.');const csrf=await response.json();headers={'Content-Type':'application/json',[csrf.headerName]:csrf.token};if(key)headers['Idempotency-Key']=key;method='POST';}
  const response=await fetch(path,{method,headers,body:body===undefined?undefined:JSON.stringify(body),cache:'no-store',signal:AbortSignal.timeout(15000)});
  if(response.status===401){location.replace('/?expired=1');throw new Error('Your session has expired.');}
  const result=response.status===204?{}:await response.json();
  if(!response.ok){const failure=new Error(result.message||'Please refresh and try again.');failure.code=result.code;throw failure;}
  return result;
}
async function action(button,work){if(busy)return;busy=true;button.disabled=true;clearError();try{await work();}catch(failure){error(failure.message||'Connection lost. Please try again.');$('#retry').hidden=false;}finally{busy=false;button.disabled=false;}}
async function load(){
  if(checking)return;checking=true;
  try{
    const [account,space]=await Promise.all([request('/api/me'),request('/api/space')]);current=space;
    if(space.state==='ACTIVE'){stopSocket?.();location.replace('/home.html');return;}
    $('#account-content').hidden=false;$('#account-status').textContent=`Your account: ${account.displayName}`;
    $('#invite-card').hidden=!account.canInvitePartner;
    if(!account.canInvitePartner){$('#partner-code').value='';$('#code-result').hidden=true;}
    $('#setup-card').hidden=space.state!=='UNPAIRED'||!space.canCreate;
    $('#waiting-card').hidden=space.state!=='WAITING';$('#join-card').hidden=space.state!=='UNPAIRED'||!!preview;
    if(!$('#shared-date').value)$('#shared-date').value=space.signupRelationshipDate;
    if(space.state==='WAITING'){
      $('#waiting-date').textContent=space.relationshipDate;
      if(!stopSocket)stopSocket=watchPairing(load,text=>{$('#connection-state').textContent=text;});
    }else if(!space.canCreate)$('#join-help').textContent='Your partner has already started your space. Enter the private pairing code they shared with you.';
  }catch(failure){error(failure.message||'We could not load your space.');$('#retry').hidden=false;}
  finally{checking=false;}
}
$('#create-space').addEventListener('submit',event=>{event.preventDefault();const button=event.currentTarget.querySelector('button');action(button,async()=>{
  if(!$('#shared-date').value||!$('#create-confirm').checked)throw new Error('Choose your relationship date and confirm it before creating your space.');
  await request('/api/spaces',{relationshipDate:$('#shared-date').value,confirmed:true});await load();
});});
$('#create-pair-code').addEventListener('click',event=>action(event.currentTarget,async()=>{
  $('#pair-code-result').hidden=true;$('#pair-code').value='';
  const invite=await request('/api/invitations',{});$('#pair-code').value=invite.code;
  $('#pair-expiry').textContent=`Expires ${new Date(invite.expiresAt).toLocaleString('en-GB',{timeZone:'Asia/Yangon'})} Myanmar time.`;$('#pair-code-result').hidden=false;$('#create-pair-code').textContent='Replace pairing invitation';
}));
$('#preview-invite').addEventListener('submit',event=>{event.preventDefault();action(event.currentTarget.querySelector('button'),async()=>{
  preview=await request('/api/invitations/preview',{code:$('#join-code').value.trim()});
  $('#inviter-name').textContent=`${preview.inviterName} invited you to your shared space.`;
  $('#your-signup-date').textContent=preview.signupRelationshipDate;$('#canonical-date').textContent=preview.relationshipDate;
  $('#date-difference').hidden=preview.signupRelationshipDate===preview.relationshipDate;$('#join-confirm').checked=false;
  $('#preview-card').hidden=false;$('#join-card').hidden=true;$('#setup-card').hidden=true;joinIntent=null;$('#join-confirm').focus();
});});
$('#confirm-join').addEventListener('click',event=>action(event.currentTarget,async()=>{
  if(!preview||!$('#join-confirm').checked)throw new Error('Please confirm the shared relationship date before joining.');
  joinIntent??={key:crypto.randomUUID(),body:{code:$('#join-code').value.trim(),confirmationToken:preview.confirmationToken,confirmedRelationshipDate:preview.relationshipDate,confirmed:true}};
  try{await request('/api/invitations/join',joinIntent.body,joinIntent.key);location.replace('/home.html');}
  catch(failure){await load();if(['PREVIEW_EXPIRED','INVALID_INVITATION'].includes(failure.code)){$('#preview-card').hidden=true;$('#join-card').hidden=false;preview=null;joinIntent=null;}throw failure;}
}));
$('#cancel-preview').addEventListener('click',()=>{preview=null;joinIntent=null;$('#preview-card').hidden=true;load();});
$('#create-code').addEventListener('click',event=>action(event.currentTarget,async()=>{
  $('#code-result').hidden=true;$('#partner-code').value='';const result=await request('/api/auth/partner-registration-code',{});
  $('#partner-code').value=result.code;$('#code-expiry').textContent=`Expires ${new Date(result.expiresAt).toLocaleString('en-GB',{timeZone:'Asia/Yangon'})} Myanmar time.`;$('#code-result').hidden=false;
}));
for(const [button,input] of [['#copy-code','#partner-code'],['#copy-pair-code','#pair-code']])$(button).addEventListener('click',async()=>{try{await navigator.clipboard.writeText($(input).value);$(button).textContent='Copied';}catch{$(input).select();error('Select and copy the code manually.');}});
$('#logout').addEventListener('click',event=>action(event.currentTarget,async()=>{await request('/api/auth/logout',{});stopSocket?.();location.replace('/?signedout=1');}));
$('#retry').addEventListener('click',()=>{clearError();load();});
window.addEventListener('pageshow',()=>{load();clearInterval(poll);poll=setInterval(load,15000);});
window.addEventListener('pagehide',()=>{stopSocket?.();stopSocket=null;clearInterval(poll);$('#account-content').hidden=true;for(const id of ['#pair-code','#partner-code','#join-code'])$(id).value='';preview=null;joinIntent=null;});
