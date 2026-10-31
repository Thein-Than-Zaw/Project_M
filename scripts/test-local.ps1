. "$PSScriptRoot/local-env.ps1"
& "$PSScriptRoot/init-local.ps1"
& "$projectRoot/mvnw.cmd" -B -ntp verify
exit $LASTEXITCODE
