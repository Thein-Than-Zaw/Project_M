param([Parameter(ValueFromRemainingArguments=$true)][string[]]$NeonArguments)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
# Honor the user's existing proxy for this CLI child only. Never change Windows
# proxy settings, disable TLS verification, or log proxy credentials.
$savedProxy = @{}
foreach ($name in @('HTTP_PROXY','HTTPS_PROXY','NODE_USE_ENV_PROXY')) {
    $savedProxy[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
$commandExit = 1
try {
    if (!$env:HTTPS_PROXY) {
        $settings = Get-ItemProperty -LiteralPath 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Internet Settings' -ErrorAction SilentlyContinue
        if ($settings.ProxyEnable -and $settings.ProxyServer) {
            $proxyAddress = [string]$settings.ProxyServer
            if ($proxyAddress.Contains('=')) {
                $entry = $proxyAddress.Split(';') | Where-Object { $_.StartsWith('https=') } | Select-Object -First 1
                if (!$entry) { $entry = $proxyAddress.Split(';') | Where-Object { $_.StartsWith('http=') } | Select-Object -First 1 }
                $proxyAddress = if ($entry) { ($entry -split '=',2)[1] } else { '' }
            }
            if ($proxyAddress) {
                if ($proxyAddress -notmatch '^https?://') { $proxyAddress = 'http://' + $proxyAddress }
                $env:HTTPS_PROXY = $proxyAddress
                $env:HTTP_PROXY = $proxyAddress
            }
        }
    }
    if ($env:HTTPS_PROXY) { $env:NODE_USE_ENV_PROXY = '1' }
    $installedCli = Get-Command neon.cmd -ErrorAction SilentlyContinue
    if ($installedCli) { & $installedCli.Source @NeonArguments }
    else { & npx.cmd --yes neon@latest @NeonArguments }
    $commandExit = $LASTEXITCODE
} finally {
    foreach ($name in $savedProxy.Keys) { [Environment]::SetEnvironmentVariable($name,$savedProxy[$name],'Process') }
}
exit $commandExit
