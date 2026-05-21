#!/usr/bin/env powershell
cd C:\Users\HP\CascadeProjects\SkyPlayerPro
Write-Host "=== COMPILATION SKYPLAYER PRO ===" -ForegroundColor Cyan

# Nettoyage
Write-Host "Nettoyage..."
if (Test-Path app\build) { Remove-Item -Recurse -Force app\build }

# Compilation
Write-Host "Compilation en cours..."
$output = .\gradlew.bat :app:assembleDebug --console=plain 2>&1
$output | Out-File build_result.txt

# Vérification
if (Test-Path app\build\outputs\apk\debug\app-debug.apk) {
    Write-Host "✅ BUILD REUSSI!" -ForegroundColor Green
    $apk = Get-Item app\build\outputs\apk\debug\app-debug.apk
    Write-Host "APK: $($apk.Name) - $([math]::Round($apk.Length/1MB,2)) MB"
} else {
    Write-Host "❌ BUILD ECHOUE" -ForegroundColor Red
    Write-Host "Erreurs:" -ForegroundColor Yellow
    $output | Select-String "e: " | Select-Object -First 20
}
