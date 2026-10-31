$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot
$env:MAVEN_USER_HOME = Join-Path $projectRoot '.tools/maven-user'
$credentialPath = Join-Path $projectRoot '.local/credentials.json'
if (!(Test-Path $credentialPath)) { throw 'Run scripts/init-local.ps1 first.' }
$localCredentials = Get-Content -Raw $credentialPath | ConvertFrom-Json
$env:DB_URL = 'jdbc:postgresql://127.0.0.1:54329/couple_private_space'
$env:DB_USERNAME = 'couple_app'
$env:DB_PASSWORD = $localCredentials.appPassword
$env:TEST_DB_URL = 'jdbc:postgresql://127.0.0.1:54329/couple_private_space_test'
$env:SERVER_ADDRESS = '127.0.0.1'
$env:SESSION_COOKIE_SECURE = 'false'
