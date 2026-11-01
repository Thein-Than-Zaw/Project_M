import assert from 'node:assert/strict';

const base = `http://127.0.0.1:${process.env.SERVER_PORT || '8082'}`;
for (const [path, status] of [
  ['/actuator/health/readiness', 200], ['/', 200], ['/api/csrf', 200],
  ['/api/me', 401], ['/api/messages', 401], ['/actuator/env', 401],
]) {
  const response = await fetch(base + path, { signal: AbortSignal.timeout(15000), redirect: 'manual' });
  assert.equal(response.status, status, path);
  if (path.includes('readiness')) assert.deepEqual(await response.json(), { status: 'UP' });
  if (path === '/') assert.match(response.headers.get('content-security-policy'), /frame-ancestors 'none'/);
  console.log(`PASS ${path} HTTP ${response.status}`);
}
const write = await fetch(`${base}/api/memories`, {
  method: 'POST', body: '{}', headers: { 'Content-Type': 'application/json' },
  signal: AbortSignal.timeout(15000),
});
assert.equal(write.status, 403);
console.log('PASS untrusted write blocked HTTP 403');
// Exercise AccountStore through Spring Security without creating an account.
const csrfResponse = await fetch(`${base}/api/csrf`);
const csrf = await csrfResponse.json();
const cookie = csrfResponse.headers.getSetCookie().map(value => value.split(';')[0]).join('; ');
const login = await fetch(`${base}/api/auth/login`, {
  method: 'POST',
  headers: { Cookie: cookie, [csrf.headerName]: csrf.token, 'Content-Type': 'application/x-www-form-urlencoded' },
  body: new URLSearchParams({ email: 'neon-connection-smoke@example.invalid', password: 'deliberately-invalid-test-value' }),
  redirect: 'manual', signal: AbortSignal.timeout(15000),
});
assert.equal(login.status, 401);
assert.match(await login.text(), /INVALID_CREDENTIALS/);
console.log('PASS database-backed login lookup HTTP 401 INVALID_CREDENTIALS (no account created)');
