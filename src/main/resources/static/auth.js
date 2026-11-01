const login = document.querySelector('#login-form'), signup = document.querySelector('#signup-form');
const errorBanner = document.querySelector('#form-error'), notice = document.querySelector('#notice');
let submitting = false;
function clearErrors() {
  errorBanner.hidden = true;
  document.querySelectorAll('.field-error').forEach(el => { el.textContent = ''; });
  document.querySelectorAll('[aria-invalid]').forEach(el => el.removeAttribute('aria-invalid'));
}
function showMode(create) {
  if (submitting) return;
  clearErrors(); notice.hidden = true; signup.hidden = !create; login.hidden = create;
  document.querySelector('#login-tab').setAttribute('aria-pressed', String(!create));
  document.querySelector('#signup-tab').setAttribute('aria-pressed', String(create));
  document.querySelector('#form-title').textContent = create ? 'Your story starts here.' : 'Welcome home.';
  document.querySelector('#form-subtitle').textContent = create ? 'A private invitation. A little world of your own.' : 'Your little world is waiting for you.';
}
document.querySelector('#login-tab').addEventListener('click', () => showMode(false));
document.querySelector('#signup-tab').addEventListener('click', () => showMode(true));
document.querySelectorAll('[data-password]').forEach(button => button.addEventListener('click', () => {
  const input = document.getElementById(button.dataset.password), visible = input.type === 'password';
  input.type = visible ? 'text' : 'password'; button.textContent = visible ? 'Hide' : 'Show';
  button.setAttribute('aria-pressed', String(visible)); button.setAttribute('aria-label', visible ? 'Hide password' : 'Show password');
}));
function displayError(result, form, create) {
  errorBanner.textContent = result.message || 'Something went wrong. Please try again.'; errorBanner.hidden = false;
  let first;
  for (const [field, message] of Object.entries(result.fieldErrors || {})) {
    const input = form.elements.namedItem(field), label = document.getElementById(`${create ? '' : 'login-'}${field}-error`);
    if (input && label) { input.setAttribute('aria-invalid', 'true'); label.textContent = message; first ||= input; }
  }
  (first || errorBanner).focus();
}
async function send(form, create) {
  if (submitting) return;
  clearErrors(); notice.hidden = true;
  const fields = Object.fromEntries(new FormData(form)), requiredErrors = {};
  for (const input of form.querySelectorAll('[required]')) if (!input.value) requiredErrors[input.name] = 'This field is required.';
  if (Object.keys(requiredErrors).length) { displayError({message: 'Check the highlighted fields.', fieldErrors: requiredErrors}, form, create); return; }
  submitting = true;
  const button = form.querySelector('[type=submit]'), originalText = button.textContent;
  button.disabled = true; button.textContent = create ? 'Creating your account…' : 'Signing in…';
  document.querySelectorAll('.tabs button').forEach(tab => { tab.disabled = true; }); form.setAttribute('aria-busy', 'true');
  try {
    const csrfResponse = await fetch('/api/csrf', {cache: 'no-store', signal: AbortSignal.timeout(15000)});
    if (!csrfResponse.ok) throw new Error('connection');
    const csrf = await csrfResponse.json();
    const response = await fetch(create ? '/api/auth/register' : '/api/auth/login', {method:'POST', headers:{[csrf.headerName]:csrf.token,'Content-Type':create?'application/json':'application/x-www-form-urlencoded'}, body:create?JSON.stringify(fields):new URLSearchParams(fields),signal:AbortSignal.timeout(30000)});
    const result = await response.json().catch(() => ({}));
    if (!response.ok) {
      if (response.status === 429) result.message = `Too many attempts. Try again in ${Math.ceil(Number(response.headers.get('Retry-After') || 60)/60)} minute(s).`;
      displayError(result,form,create); return;
    }
    form.reset();
    if (create) {
      submitting = false; showMode(false); document.querySelector('#login-email').value = fields.email;
      notice.textContent = 'Your account is ready. Sign in to continue.'; notice.hidden = false; document.querySelector('#login-password').focus();
    } else window.location.replace(result.next==='/home.html'?'/home.html':'/space.html');
  } catch {
    displayError({message:create?'We could not confirm the result. Try signing in first; if your account was not created, retry with the same code.':'We could not reach your space. Check your connection and try again.'},form,create);
  } finally {
    submitting = false; button.disabled = false; button.textContent = originalText; form.removeAttribute('aria-busy'); document.querySelectorAll('.tabs button').forEach(tab => { tab.disabled = false; });
  }
}
login.addEventListener('submit',event=>{event.preventDefault();send(login,false);});
signup.addEventListener('submit',event=>{event.preventDefault();send(signup,true);});
if(new URLSearchParams(location.search).has('expired')){notice.textContent='Your session has expired. Please sign in again.';notice.hidden=false;}
if(new URLSearchParams(location.search).has('signedout')){notice.textContent='You have signed out. See you soon.';notice.hidden=false;}
window.addEventListener('pageshow',async()=>{try{const result=await fetch('/api/me',{cache:'no-store'});if(result.ok){const account=await result.json();window.location.replace(account.paired?'/home.html':'/space.html');}}catch{/* Form remains usable. */}});
