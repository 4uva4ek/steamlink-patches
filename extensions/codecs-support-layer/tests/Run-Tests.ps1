[CmdletBinding()]
param([string]$NdkDirectory, [string]$OutputDirectory)
$ErrorActionPreference = 'Stop'
$csRepo = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$steamlinkBuildRoot = [IO.Path]::GetFullPath((Join-Path $csRepo '../builds/steamlink-patches'))
if (!$NdkDirectory) { $NdkDirectory = Join-Path $csRepo '.android-sdk/ndk/27.2.12479018' }
$csBin = Join-Path $NdkDirectory 'toolchains/llvm/prebuilt/windows-x86_64/bin'
$out = if ($OutputDirectory) { [IO.Path]::GetFullPath($OutputDirectory) } else { Join-Path $steamlinkBuildRoot 'build/codecs-support-work/host-tests' }
New-Item -ItemType Directory -Force -Path $out | Out-Null

if (!(Get-Command cl.exe -ErrorAction SilentlyContinue)) {
    $envScript = Join-Path $csRepo '../GalaxyXR-Driver/tools/Enter-PortableBuildEnvironment.ps1'
    if (!(Test-Path $envScript)) { throw "Portable build environment not found: $envScript" }
    . $envScript -NoDownload
}
if (!(Get-Command cl.exe -ErrorAction SilentlyContinue)) { throw 'cl.exe unavailable after environment setup' }

foreach ($suite in @('pure_tests')) {
    $exe = Join-Path $out "$suite.exe"
    & cl.exe /nologo /std:c++17 /EHsc /O1 /W4 "/Fe:$exe" "/Fo:$out\\" (Join-Path $PSScriptRoot "$suite.cpp")
    if ($LASTEXITCODE -ne 0) { throw "Codecs support $suite compilation failed" }
    & $exe
    if ($LASTEXITCODE -ne 0) { throw "Codecs support $suite failed" }
}
