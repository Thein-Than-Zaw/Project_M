$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot
$databaseSelectionPath = Join-Path $projectRoot '.local/database.json'
if (Test-Path $databaseSelectionPath) {
    $databaseSelection = Get-Content -Raw $databaseSelectionPath | ConvertFrom-Json
    if ($databaseSelection.mode -eq 'installed') {
        . "$PSScriptRoot/local-env.ps1"
        $driver = Get-ChildItem '.tools/m2/org/postgresql/postgresql' -Filter 'postgresql-*.jar' -Recurse | Select-Object -First 1
        if (!$driver) { throw 'Restore Maven dependencies first.' }
        & java --class-path $driver.FullName scripts/VerifyLocalDatabase.java
        if ($LASTEXITCODE -ne 0) { throw 'Installed database verification failed. Check the PostgreSQL Windows service; no portable fallback was started.' }
        return
    }
    if ($databaseSelection.mode -ne 'portable' -or [int]$databaseSelection.port -ne 54329) { throw 'Invalid portable database selection.' }
}
$pgBin = Join-Path $projectRoot '.tools/pgsql/bin'
$pgData = Join-Path $projectRoot '.local/pgdata'
if (!(Test-Path "$pgBin/initdb.exe")) { throw 'Run scripts/bootstrap-tools.ps1 first.' }
New-Item -ItemType Directory -Force .local | Out-Null
$credentialPath = Join-Path $projectRoot '.local/credentials.json'
if (!(Test-Path $credentialPath)) {
    function New-LocalPassword { $b = New-Object byte[] 32; $rng = [Security.Cryptography.RandomNumberGenerator]::Create(); try { $rng.GetBytes($b); [Convert]::ToBase64String($b) } finally { $rng.Dispose() } }
    @{adminPassword=New-LocalPassword; appPassword=New-LocalPassword} | ConvertTo-Json | Set-Content -Encoding ASCII $credentialPath
}
$localCredentials = Get-Content -Raw $credentialPath | ConvertFrom-Json
if (!(Test-Path "$pgData/PG_VERSION")) {
    $passwordFile = Join-Path $projectRoot '.local/init-password.tmp'
    try {
        Set-Content -Encoding ASCII $passwordFile $localCredentials.adminPassword
        & "$pgBin/initdb.exe" -D $pgData -U local_operator --pwfile=$passwordFile --auth=scram-sha-256 --encoding=UTF8 --locale=C
        if ($LASTEXITCODE -ne 0) { throw 'initdb failed; inspect output, do not overwrite existing data.' }
        Add-Content -Encoding ASCII "$pgData/postgresql.conf" "`nlisten_addresses = '127.0.0.1'`nport = 54329`ntimezone = 'UTC'`npassword_encryption = 'scram-sha-256'"
    } finally { Remove-Item -LiteralPath $passwordFile -ErrorAction SilentlyContinue }
}
& "$pgBin/pg_ctl.exe" -D $pgData status *> $null
if ($LASTEXITCODE -ne 0) {
    & "$pgBin/pg_ctl.exe" -D $pgData -l "$projectRoot/.local/postgres.log" -w start
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL startup failed.' }
}
$driver = Get-ChildItem '.tools/m2/org/postgresql/postgresql' -Filter 'postgresql-*.jar' -Recurse | Select-Object -First 1
if (!$driver) { throw 'Run ./mvnw.cmd -DskipTests package to restore JDBC dependencies first.' }
$env:LOCAL_PG_ADMIN_PASSWORD = $localCredentials.adminPassword
$env:LOCAL_PG_APP_PASSWORD = $localCredentials.appPassword
try {
    & java --class-path $driver.FullName scripts/ProvisionLocalDatabase.java
    if ($LASTEXITCODE -ne 0) { throw 'Database provisioning failed.' }
} finally { Remove-Item Env:LOCAL_PG_ADMIN_PASSWORD,Env:LOCAL_PG_APP_PASSWORD }
Write-Output 'Local PostgreSQL ready on 127.0.0.1:54329. Credentials stay in ignored .local/credentials.json.'
