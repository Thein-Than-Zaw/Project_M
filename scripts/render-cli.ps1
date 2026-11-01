param([Parameter(ValueFromRemainingArguments=$true)][string[]]$RenderArguments)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot
$binary=Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools/render-2.34.0') -Filter '*.exe' | Select-Object -First 1
if (!$binary) { throw 'Install the verified Render CLI archive in .tools/render-2.34.0 first.' }
$savedSettings=@{}
foreach ($name in @('HTTP_PROXY','HTTPS_PROXY','RENDER_CLI_CONFIG_DIR')) {
    $savedSettings[$name]=[Environment]::GetEnvironmentVariable($name,'Process')
}
$commandExit=1
try {
    # Keep this project's CLI token private and separate from global MCP config.
    $env:RENDER_CLI_CONFIG_DIR=Join-Path $projectRoot '.local/render-cli'
    if (!$env:HTTPS_PROXY) {
        $settings=Get-ItemProperty -LiteralPath 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Internet Settings' -ErrorAction SilentlyContinue
        if ($settings.ProxyEnable -and $settings.ProxyServer) {
            $proxyAddress=[string]$settings.ProxyServer
            if ($proxyAddress.Contains('=')) {
                $entry=$proxyAddress.Split(';') | Where-Object { $_.StartsWith('https=') } | Select-Object -First 1
                if (!$entry) { $entry=$proxyAddress.Split(';') | Where-Object { $_.StartsWith('http=') } | Select-Object -First 1 }
                $proxyAddress=if ($entry) { ($entry -split '=',2)[1] } else { '' }
            }
            if ($proxyAddress) {
                if ($proxyAddress -notmatch '^https?://') { $proxyAddress='http://'+$proxyAddress }
                $env:HTTP_PROXY=$proxyAddress;$env:HTTPS_PROXY=$proxyAddress
            }
        }
    }
    & $binary.FullName @RenderArguments
    $commandExit=$LASTEXITCODE
} finally {
    foreach ($name in $savedSettings.Keys) { [Environment]::SetEnvironmentVariable($name,$savedSettings[$name],'Process') }
}
exit $commandExit
