[CmdletBinding()]
param([string]$OutputDirectory)
$ErrorActionPreference = 'Stop'
$tlRepo = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
if (!$OutputDirectory) { $OutputDirectory = [IO.Path]::GetFullPath((Join-Path $tlRepo '../builds/steamlink-patches/extensions/telemetry-layer/build-host')) }
$OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)
if ($OutputDirectory.StartsWith($tlRepo + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase) -or $OutputDirectory -eq $tlRepo) { throw 'Test outputs must stay outside the source checkout' }
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
if (!(Get-Command cl.exe -ErrorAction SilentlyContinue)) { . (Join-Path $tlRepo '../GalaxyXR-Driver/tools/Enter-PortableBuildEnvironment.ps1') -NoDownload }
$tlExe = Join-Path $OutputDirectory 'decoder_probe_tests.exe'
& cl.exe /nologo /std:c++17 /EHsc /O1 /W4 "/Fe:$tlExe" "/Fo:$OutputDirectory\\" (Join-Path $PSScriptRoot 'decoder_probe_tests.cpp')
if ($LASTEXITCODE -ne 0) { throw 'Telemetry host test compilation failed' }
& $tlExe
if ($LASTEXITCODE -ne 0) { throw 'Telemetry host tests failed' }
