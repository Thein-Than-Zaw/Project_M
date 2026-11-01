$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot
$env:MAVEN_USER_HOME = Join-Path $projectRoot '.tools/maven-user'
$credentialPath = Join-Path $projectRoot '.local/credentials.json'
if (!(Test-Path $credentialPath)) { throw 'Run scripts/init-local.ps1 first.' }
$localCredentials = Get-Content -Raw $credentialPath | ConvertFrom-Json
$databasePort = 54329
$databaseSelectionPath = Join-Path $projectRoot '.local/database.json'
if (Test-Path $databaseSelectionPath) {
    $databaseSelection = Get-Content -Raw $databaseSelectionPath | ConvertFrom-Json
    if (!(($databaseSelection.mode -eq 'installed' -and [int]$databaseSelection.port -eq 5432) -or
          ($databaseSelection.mode -eq 'portable' -and [int]$databaseSelection.port -eq 54329))) {
        throw 'Invalid local database selection; refusing an implicit fallback.'
    }
    $databasePort = [int]$databaseSelection.port
}
$env:DB_URL = "jdbc:postgresql://127.0.0.1:$databasePort/couple_private_space"
$env:DB_USERNAME = 'couple_app'
$env:DB_PASSWORD = $localCredentials.appPassword
$env:TEST_DB_URL = "jdbc:postgresql://127.0.0.1:$databasePort/couple_private_space_test"
$env:SERVER_ADDRESS = '127.0.0.1'
$env:SESSION_COOKIE_SECURE = 'false'
$turnEnvPath = Join-Path $projectRoot '.local/turn.env'
if (Test-Path -LiteralPath $turnEnvPath) {
    foreach ($turnSetting in Get-Content -LiteralPath $turnEnvPath) {
        if ($turnSetting -match '^(TURN_SHARED_SECRET|TURN_URLS|STUN_URLS)=(.*)$') {
            [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
        }
    }
}
