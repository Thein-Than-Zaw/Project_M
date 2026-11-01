. "$PSScriptRoot/local-env.ps1"
$driver = Get-ChildItem '.tools/m2/org/postgresql/postgresql' -Filter 'postgresql-*.jar' -Recurse | Select-Object -First 1
if (!$driver) { throw 'Restore Maven dependencies first.' }
& java --class-path $driver.FullName scripts/IssueSetupToken.java
exit $LASTEXITCODE
