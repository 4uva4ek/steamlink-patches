[CmdletBinding()]
param([string]$NdkDirectory)
$ErrorActionPreference = 'Stop'
$csRepo = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$steamlinkBuildRoot = [IO.Path]::GetFullPath((Join-Path $csRepo '../builds/steamlink-patches'))
if (!$NdkDirectory) { $NdkDirectory = Join-Path $csRepo '.android-sdk/ndk/27.2.12479018' }
$csBin = Join-Path $NdkDirectory 'toolchains/llvm/prebuilt/windows-x86_64/bin'
$out = Join-Path $steamlinkBuildRoot 'build/codecs-support-work/host-tests'
New-Item -ItemType Directory -Force -Path $out | Out-Null
$exe = Join-Path $out 'pure_tests.exe'

if (!(Get-Command cl.exe -ErrorAction SilentlyContinue)) {
    $envScript = Join-Path $csRepo '../GalaxyXR-Driver/tools/Enter-PortableBuildEnvironment.ps1'
    if (!(Test-Path $envScript)) { throw "Portable build environment not found: $envScript" }
    . $envScript -NoDownload
}
if (!(Get-Command cl.exe -ErrorAction SilentlyContinue)) { throw 'cl.exe unavailable after environment setup' }

& cl.exe /nologo /std:c++17 /EHsc /O1 /W4 "/Fe:$exe" "/Fo:$out\\" (Join-Path $PSScriptRoot 'pure_tests.cpp')
if ($LASTEXITCODE -ne 0) { throw 'Codecs support host test compilation failed' }
& $exe
if ($LASTEXITCODE -ne 0) { throw 'Codecs support host tests failed' }
