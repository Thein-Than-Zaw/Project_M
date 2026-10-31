async function checkHealth() {
  const status = document.querySelector('#status');
  const button = document.querySelector('#retry');
  button.disabled = true;
  try {
    const response = await fetch('/actuator/health/readiness', {cache: 'no-store', signal: AbortSignal.timeout(8000)});
    const body = await response.json();
    status.textContent = response.ok && body.status === 'UP' ? 'Application and PostgreSQL are ready.' : 'Service is not ready. Check local services and retry.';
  } catch {
    status.textContent = 'Cannot reach the service. Check local services and retry.';
  } finally { button.disabled = false; }
}
document.querySelector('#retry').addEventListener('click', checkHealth);
checkHealth();
