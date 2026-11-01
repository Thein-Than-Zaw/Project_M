$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$selectionPath = Join-Path $projectRoot '.local/database.json'
if ((Test-Path $selectionPath) -and ((Get-Content -Raw $selectionPath | ConvertFrom-Json).mode -eq 'installed')) {
    Write-Output 'Installed PostgreSQL is managed by its Windows service and remains running. This helper only stops the optional portable cluster.'
    exit 0
}
& "$projectRoot/.tools/pgsql/bin/pg_ctl.exe" -D "$projectRoot/.local/pgdata" -m fast -w stop
exit $LASTEXITCODE
