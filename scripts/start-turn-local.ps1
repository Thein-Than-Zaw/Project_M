$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
Set-Location $taskRoot
if(!(Get-Command docker -ErrorAction SilentlyContinue)){throw 'Docker is not installed. Local direct calls can run without it; a local TURN relay requires a working Docker engine.'}
& docker info --format '{{.ServerVersion}}' | Out-Null
if($LASTEXITCODE -ne 0){throw 'Docker engine is not running. No relay or credentials were created.'}
$turnEnv=Join-Path $taskRoot '.local/turn.env'
if(!(Test-Path -LiteralPath $turnEnv)){
    New-Item -ItemType Directory -Force (Join-Path $taskRoot '.local') | Out-Null
    $turnSecret=[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
    @("TURN_SHARED_SECRET=$turnSecret",'TURN_URLS=turn:127.0.0.1:3478?transport=udp,turn:127.0.0.1:3478?transport=tcp','STUN_URLS=stun:127.0.0.1:3478') | Set-Content -Encoding ascii -LiteralPath $turnEnv
}
& docker compose --env-file $turnEnv -f ops/turn/compose.yml up -d
if($LASTEXITCODE -ne 0){throw 'TURN relay did not start; check Docker output. No connectivity PASS is implied.'}
Write-Output 'Local TURN container started. Restart the app to load .local/turn.env, then verify a relay-only call (?relay=1).'
