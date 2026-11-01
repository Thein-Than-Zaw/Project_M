$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$mediaExe=Join-Path $taskRoot '.tools/ffmpeg/ffmpeg-8.1.2-essentials_build/bin/ffprobe.exe'
if(Test-Path -LiteralPath $mediaExe){& $mediaExe -version | Select-Object -First 1;exit 0}
$sevenZip=Join-Path $env:ProgramFiles '7-Zip/7z.exe'
if(!(Test-Path -LiteralPath $sevenZip)){throw 'Install 7-Zip or supply FFMPEG_PATH/FFPROBE_PATH for your existing FFmpeg installation.'}
$archive=Join-Path $taskRoot '.tools/ffmpeg-8.1.2.7z'
New-Item -ItemType Directory -Force (Join-Path $taskRoot '.tools') | Out-Null
Invoke-WebRequest 'https://www.gyan.dev/ffmpeg/builds/packages/ffmpeg-8.1.2-essentials_build.7z' -OutFile $archive
if((Get-FileHash -LiteralPath $archive).Hash -ne 'E25B682664025D49034C981AFB4BAE36238A40F29A3CC1C713AD9A8B5B3528F6'){throw 'FFmpeg archive checksum mismatch; do not extract.'}
& $sevenZip x $archive ('-o'+(Join-Path $taskRoot '.tools/ffmpeg')) -y
if($LASTEXITCODE -ne 0){throw 'FFmpeg extraction failed'}
& $mediaExe -version | Select-Object -First 1
