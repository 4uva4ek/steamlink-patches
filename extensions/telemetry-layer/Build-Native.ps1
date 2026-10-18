[CmdletBinding()]
param([string]$NdkDirectory, [string]$OpenXrDirectory, [string]$OutputDirectory, [string]$Python)
$ErrorActionPreference = 'Stop'
$tlRepo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$tlBuildRoot = [IO.Path]::GetFullPath((Join-Path $tlRepo '../builds/steamlink-patches'))
if (!$OutputDirectory) { $OutputDirectory = Join-Path $tlBuildRoot 'extensions/telemetry-layer/build-android' }
$OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)
if ($OutputDirectory.StartsWith($tlRepo + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase) -or $OutputDirectory -eq $tlRepo) { throw 'Build outputs must be outside the source checkout' }
if (!$NdkDirectory) { $NdkDirectory = Join-Path $tlRepo '.android-sdk/ndk/27.2.12479018' }
if (!$OpenXrDirectory) { $OpenXrDirectory = Join-Path $tlBuildRoot 'extensions/controller-velocity-layer/build-android/_deps/openxr_headers-src' }
if (!$Python) {
    $tlPython = Join-Path $tlRepo '../builds/SteamVRColourLab/.venv/Scripts/python.exe'
    $Python = if (Test-Path -LiteralPath $tlPython) { $tlPython } else { (Get-Command python -ErrorAction Stop).Source }
}
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$tlDeps = Join-Path $OutputDirectory 'shadowhook-2.0.1'
New-Item -ItemType Directory -Force -Path $tlDeps | Out-Null
$tlAar = Join-Path $tlDeps 'shadowhook-2.0.1.aar'
$tlAarHash = '2cad01ff4d59542958775e19f281981a7e84b39670b5fae75905b8511e10c7cd'
if (!(Test-Path -LiteralPath $tlAar)) {
    Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/com/bytedance/android/shadowhook/2.0.1/shadowhook-2.0.1.aar' -OutFile $tlAar
}
if ((Get-FileHash -LiteralPath $tlAar -Algorithm SHA256).Hash.ToLowerInvariant() -ne $tlAarHash) { throw 'Pinned ShadowHook AAR hash mismatch' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$tlArchive = [IO.Compression.ZipFile]::OpenRead($tlAar)
try {
    $tlEntries = @{
        'prefab/modules/shadowhook/include/shadowhook.h' = 'shadowhook.h'
        'jni/arm64-v8a/libshadowhook.so' = 'libshadowhook.so'
        'jni/arm64-v8a/libshadowhook_nothing.so' = 'libshadowhook_nothing.so'
    }
    foreach ($tlName in $tlEntries.Keys) {
        $tlEntry = $tlArchive.GetEntry($tlName)
        if (!$tlEntry) { throw "Pinned dependency entry missing: $tlName" }
        [IO.Compression.ZipFileExtensions]::ExtractToFile($tlEntry, (Join-Path $tlDeps $tlEntries[$tlName]), $true)
    }
} finally { $tlArchive.Dispose() }
$env:PYTHONDONTWRITEBYTECODE = '1'
& $Python -B (Join-Path $PSScriptRoot 'tools/generate_profiles.py') --profiles (Join-Path $tlRepo 'patches/src/main/resources/steamlink/androidxr/telemetry/profiles.tsv') --output (Join-Path $OutputDirectory 'telemetry_profiles.h')
if ($LASTEXITCODE -ne 0) { throw 'Telemetry admission-profile generation failed' }
$tlBin = Join-Path $NdkDirectory 'toolchains/llvm/prebuilt/windows-x86_64/bin'
$tlLibrary = Join-Path $OutputDirectory 'libgxr_telemetry.so'
& (Join-Path $tlBin 'aarch64-linux-android29-clang++.cmd') -std=c++17 -O2 -fPIC -shared -fvisibility=hidden -static-libstdc++ -Wall -Wextra -Werror -Wno-missing-field-initializers `
    -DXR_USE_PLATFORM_ANDROID "-I$OpenXrDirectory/include" "-I$tlDeps" "-I$OutputDirectory" `
    (Join-Path $PSScriptRoot 'src/telemetry_layer.cpp') -o $tlLibrary "-L$tlDeps" -lshadowhook -llog -ldl `
    '-Wl,-soname,libgxr_telemetry.so' '-Wl,-z,max-page-size=16384' '-Wl,--exclude-libs,ALL'
if ($LASTEXITCODE -ne 0) { throw 'Telemetry Android compilation failed' }
& (Join-Path $tlBin 'llvm-strip.exe') --strip-unneeded $tlLibrary
if ($LASTEXITCODE -ne 0) { throw 'Telemetry strip failed' }
Get-FileHash -LiteralPath $tlLibrary -Algorithm SHA256
Write-Host 'Build output only; stage validated independent telemetry resources and their hash pins separately.'
