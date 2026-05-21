# Guide de Compilation SkyPlayer Pro

## Commandes pour compiler l'APK

### 1. Ouvrir un terminal PowerShell dans le dossier du projet
```powershell
cd C:\Users\HP\CascadeProjects\SkyPlayerPro
```

### 2. Nettoyer le projet
```powershell
.\gradlew.bat clean
```

### 3. Compiler l'APK Debug
```powershell
.\gradlew.bat :app:assembleDebug
```

### 4. Vérifier que l'APK a été créée
```powershell
Get-ChildItem app\build\outputs\apk\debug\app-debug.apk
```

## Si des erreurs apparaissent

### Erreur 1: "Argument type mismatch: actual type is 'kotlin.String?'"
**Fichiers concernés:**
- `SettingsViewModel.kt` lignes 45, 48, 49, 50
- `StreamingPreferencesViewModel.kt` ligne 42

**Solution:** Ajouter `?: "default"` pour les valeurs nullables

### Erreur 2: "Overload resolution ambiguity"
**Fichier:** `SettingsViewModel.kt` ligne 175

**Solution:** Supprimer une des méthodes `getPlaylistCount()` dupliquées dans `PlaylistRepository.kt`

### Erreur 3: Room/KSP errors
Si des erreurs liées à Room apparaissent:
1. Vérifier que `ContentMetadata.kt` a les annotations `@Entity` correctes
2. Vérifier que `AppDatabase.kt` inclut `ContentMetadata::class` dans les entités
3. Vérifier que `ContentMetadataDao.kt` est correctement défini

## Emplacement de l'APK
Une fois compilée avec succès:
```
app/build/outputs/apk/debug/app-debug.apk
```

## Installation
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```
