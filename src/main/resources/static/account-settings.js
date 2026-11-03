// ACCOUNT-01: sensitive drafts stay in this document only; no password/token storage/logging.
const $=id=>document.getElementById(id);
let passwordBusy=false,nicknameBusy=false,label=null,nicknameDirty=false,closed=false;
export function validatePasswordChange(current,next,confirm){
 if(!current)return 'Enter your current password.';
 if(Array.from(current).length>128)return 'Current password is too long.';
 const count=Array.from(next).length;if(count<12||count>128)return 'Use between 12 and 128 characters for your new password.';
 if(next!==confirm)return 'Passwords must match.';
 if(current===next)return 'Choose a different new password.';return '';
}
async function request(path,method='GET',body){
 const options={method,cache:'no-store',signal:AbortSignal.timeout(15000)};
 if(method!=='GET'){const token=await request('/api/csrf');options.headers={[token.headerName]:token.token,'Content-Type':'application/json'};options.body=JSON.stringify(body);}
 const response=await fetch(path,options);if(!response.ok){const error=await response.json().catch(()=>({}));throw Object.assign(new Error(error.message||'Could not save this change.'),{status:response.status,code:error.code,fields:error.fieldErrors});}
 return response.status===204?null:response.json();
}
function clearPasswords(){for(const id of ['password-current','password-new','password-confirm']){$(id).value='';$(id).type='password';$(id).removeAttribute('aria-invalid');}$('password-show').textContent='Show passwords';$('password-show').setAttribute('aria-pressed','false');}
function passwordControls(){for(const id of ['password-current','password-new','password-confirm','password-save','password-show'])$(id).disabled=passwordBusy;$('password-form').setAttribute('aria-busy',String(passwordBusy));$('password-save').textContent=passwordBusy?'Changing password…':'Change password';}
$('password-show').addEventListener('click',()=>{const show=$('password-current').type==='password';for(const id of ['password-current','password-new','password-confirm'])$(id).type=show?'text':'password';$('password-show').textContent=show?'Hide passwords':'Show passwords';$('password-show').setAttribute('aria-pressed',String(show));});
$('password-form').addEventListener('submit',async event=>{
 event.preventDefault();if(passwordBusy||closed)return;const current=$('password-current').value,next=$('password-new').value,confirm=$('password-confirm').value;
 const invalid=validatePasswordChange(current,next,confirm);$('password-error').textContent=invalid;if(invalid)return;
 passwordBusy=true;passwordControls();for(const id of ['password-current','password-new','password-confirm'])$(id).removeAttribute('aria-invalid');
 try{await request('/api/auth/change-password','POST',{currentPassword:current,newPassword:next,confirmPassword:confirm});if(closed)return;clearPasswords();location.replace('/?passwordChanged=1');}
 catch(error){if(closed)return;if(error.status===401){clearPasswords();location.replace('/?expired=1');return;}$('password-error').textContent=error.status?error.message:'Could not confirm the change. Try signing in with your new password; do not resend automatically.';for(const [field,id] of Object.entries({currentPassword:'password-current',newPassword:'password-new',confirmPassword:'password-confirm'}))if(error.fields?.[field])$(id).setAttribute('aria-invalid','true');}
 finally{passwordBusy=false;if(!closed)passwordControls();}
});
function nicknameControls(){for(const id of ['partner-nickname-input','nickname-save','nickname-reset'])$(id).disabled=nicknameBusy||!label;$('nickname-form').setAttribute('aria-busy',String(nicknameBusy));$('nickname-save').textContent=nicknameBusy?'Saving…':'Save nickname';}
function showLabel(value){label=value;$('partner-profile-name').textContent=value.profileName;$('partner-nickname-input').value=value.nickname||'';nicknameDirty=false;nicknameControls();}
async function loadNickname(){if(nicknameBusy||closed)return;nicknameBusy=true;nicknameControls();$('nickname-error').textContent='';try{const next=await request('/api/partner-nickname');if(!closed){showLabel(next);$('nickname-reload').hidden=true;}}catch(error){if(!closed){$('nickname-error').textContent=error.message;$('nickname-reload').hidden=false;}}finally{nicknameBusy=false;if(!closed)nicknameControls();}}
async function saveNickname(reset=false){
 if(nicknameBusy||!label||closed)return;const nickname=reset?'':$('partner-nickname-input').value;if(Array.from(nickname).length>60){$('nickname-error').textContent='Use up to 60 characters.';return;}
 nicknameBusy=true;nicknameControls();$('nickname-error').textContent='';$('nickname-status').textContent='';
 try{const next=await request('/api/partner-nickname','PUT',{nickname,version:label.version});if(closed)return;showLabel(next);$('nickname-reload').hidden=true;$('nickname-status').textContent=next.nickname?'Nickname saved — visible only to you.':'Nickname removed. Your partner’s profile name is shown again.';window.dispatchEvent(new CustomEvent('partner-nickname-updated',{detail:next}));}
 catch(error){if(!closed){$('nickname-error').textContent=error.message||'Could not confirm this change. Reload to check before saving again.';$('nickname-reload').hidden=false;}}
 finally{nicknameBusy=false;if(!closed)nicknameControls();}
}
$('nickname-form').addEventListener('submit',event=>{event.preventDefault();saveNickname();});$('partner-nickname-input').addEventListener('input',()=>{nicknameDirty=true;});$('nickname-reset').addEventListener('click',()=>saveNickname(true));$('nickname-reload').addEventListener('click',loadNickname);
function routeAccount(){if(location.hash!=='#security'&&!passwordBusy)clearPasswords();if(location.hash==='#partner'&&!nicknameDirty)loadNickname();}
window.addEventListener('hashchange',routeAccount);window.addEventListener('pageshow',routeAccount);window.addEventListener('pagehide',()=>{closed=true;clearPasswords();$('partner-nickname-input').value='';label=null;});nicknameControls();routeAccount();
