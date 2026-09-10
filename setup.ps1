param([switch]$AutoRefresh)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$sdk = "app/libs/glyph-matrix-sdk-2.0.aar"
if (-not (Test-Path $sdk)) {
    Write-Host "Downloading the Nothing Glyph Matrix SDK..."
    Invoke-WebRequest -UseBasicParsing -Uri "https://raw.githubusercontent.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/main/glyph-matrix-sdk-2.0.aar" -OutFile $sdk
}

$jbr = "C:\Program Files\Android\Android Studio\jbr"
if (Test-Path "$jbr\bin\java.exe") {
    $env:JAVA_HOME = $jbr
    $env:Path = "$jbr\bin;$env:Path"
}

$adb = (Get-Command adb -ErrorAction SilentlyContinue).Source
if (-not $adb) {
    $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
}
if (-not (Test-Path $adb)) {
    throw "ADB not found. In Android Studio, install Android SDK Platform-Tools."
}

& $adb get-state
if ($LASTEXITCODE) { throw "ADB cannot see the phone. Check the USB connection and accept the debugging prompt on the phone." }

& .\gradlew.bat installDebug
if ($LASTEXITCODE) { throw "The Android build or installation failed." }

& $adb shell settings put global nt_glyph_interface_debug_enable 1
if ($AutoRefresh) {
    & $adb shell pm grant com.akil.glyphlife android.permission.WRITE_SECURE_SETTINGS
}

Write-Host "Installed. Add the Glyph Mini Apps tiles from the Quick Settings editor."
