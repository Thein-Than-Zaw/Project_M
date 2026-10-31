$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
& "$projectRoot/.tools/pgsql/bin/pg_ctl.exe" -D "$projectRoot/.local/pgdata" -m fast -w stop
exit $LASTEXITCODE
