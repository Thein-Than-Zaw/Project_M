. "$PSScriptRoot/local-env.ps1"
& "$PSScriptRoot/init-local.ps1"
& "$projectRoot/mvnw.cmd" spring-boot:run
exit $LASTEXITCODE
