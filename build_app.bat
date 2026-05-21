@echo off
cd /d C:\Users\HP\CascadeProjects\SkyPlayerPro
echo === NETTOYAGE ===
call .\gradlew.bat clean
echo === COMPILATION ===
call .\gradlew.bat :app:assembleDebug --console=plain 2>&1
echo === VERIFICATION ===
if exist app\build\outputs\apk\debug\app-debug.apk (
    echo SUCCESS: APK generee
    dir app\build\outputs\apk\debug\app-debug.apk
) else (
    echo ERREUR: APK non trouvee
)
pause
