$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot
New-Item -ItemType Directory -Force .tools | Out-Null
function Get-CheckedFile($url, $destination, $sha256) {
    if (!(Test-Path $destination) -or (Get-FileHash $destination -Algorithm SHA256).Hash -ne $sha256) {
        & curl.exe --ssl-revoke-best-effort -fSL --retry 2 $url -o $destination
        if ($LASTEXITCODE -ne 0) { throw "Download failed: $url" }
    }
    if ((Get-FileHash $destination -Algorithm SHA256).Hash -ne $sha256) { throw "Checksum mismatch: $destination" }
}
# Maven uses its published SHA-512 checksum. TLS certificate checks stay enabled.
if (!(Test-Path '.tools/apache-maven-3.9.12/bin/mvn.cmd')) {
    & curl.exe --ssl-revoke-best-effort -fSL https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.12/apache-maven-3.9.12-bin.zip -o .tools/maven.zip
    if ($LASTEXITCODE -ne 0) { throw 'Maven download failed.' }
    $expected = '0EF0D074761A55E7B982FD7F2CD6EA2DA028289C73156EBD488C0513EFBFF8A64AC5BC0C11C3B8BBCED930CE15238ECEF344B0E065AE54F7CDCA8C86CF39E736'
    if ((Get-FileHash .tools/maven.zip -Algorithm SHA512).Hash -ne $expected) { throw 'Maven checksum mismatch.' }
    Expand-Archive -LiteralPath .tools/maven.zip -DestinationPath .tools -Force
}
if (!(Test-Path '.tools/pgsql/bin/postgres.exe')) {
    Get-CheckedFile 'https://repo.maven.apache.org/maven2/io/zonky/test/postgres/embedded-postgres-binaries-windows-amd64/17.7.0/embedded-postgres-binaries-windows-amd64-17.7.0.jar' '.tools/pg-binaries.jar' 'C488D0A4C94B1FCB525E6503C93FC7A0023092AB33762719C7283418F7BDDD15'
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    if (!(Test-Path '.tools/pg-archive/postgres-windows-x86_64.txz')) {
        [System.IO.Compression.ZipFile]::ExtractToDirectory((Resolve-Path '.tools/pg-binaries.jar'), "$projectRoot/.tools/pg-archive")
    }
    New-Item -ItemType Directory -Force .tools/pgsql | Out-Null
    & tar.exe -xf .tools/pg-archive/postgres-windows-x86_64.txz -C .tools/pgsql
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL extraction failed.' }
}
Write-Output 'Pinned portable tools ready. No system PATH or Windows service was changed.'
# Prime the official wrapper cache from the same verified archive (no second download).
$distributionUrl = 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.12/apache-maven-3.9.12-bin.zip'
$distributionKey = ([Security.Cryptography.SHA256]::Create().ComputeHash([byte[]][char[]]$distributionUrl) | ForEach-Object { $_.ToString('x2') }) -join ''
$wrapperCache = Join-Path $projectRoot ".tools/maven-user/wrapper/dists/apache-maven-3.9.12/$distributionKey"
if (!(Test-Path "$wrapperCache/bin/mvn.cmd")) {
    New-Item -ItemType Directory -Force $wrapperCache | Out-Null
    Copy-Item '.tools/apache-maven-3.9.12/*' $wrapperCache -Recurse -Force
}
