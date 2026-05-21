# Diagnostic Crash APK - Sky Player Pro

## Problème
APK s'installe mais crash immédiatement au lancement (Android 8)

## Causes probables et solutions

### 1. Erreur Firebase (La plus probable)

**Vérification :** Le fichier `google-services.json` doit être valide.

**Test rapide :** Supprimez temporairement Firebase pour voir si l'app démarre.

Dans `SkyPlayerApplication.kt`, commentez l'initialisation Firebase :
```kotlin
// Initialisation Firebase (obligatoire pour éviter les crashes)
try {
    // FirebaseApp.initializeApp(this)
    // FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    // Timber.i("🔥 Firebase initialisé avec succès")
    Timber.i("⚠️ Firebase désactivé pour test")
} catch (e: Exception) {
    Timber.e(e, "❌ Erreur initialisation Firebase: ${e.message}")
}
```

**Reconstruisez l'APK** et testez. Si ça marche, le problème vient de Firebase.

---

### 2. Erreur Hilt/Dagger Injection

**Vérification :** Regardez dans Android Studio > Logcat pour voir l'erreur exacte.

Recherchez :
- `Caused by: java.lang.ClassNotFoundException`
- `DaggerSkyPlayerApplication_HiltComponents`
- `Missing binding`

---

### 3. Comment obtenir l'erreur exacte (CRUCIAL)

#### Méthode A : Via Android Studio Logcat

1. Ouvrez Android Studio
2. Connectez votre téléphone en USB
3. Cliquez sur **Logcat** (en bas de la fenêtre)
4. Lancez l'app sur le téléphone
5. Cherchez les lignes rouges avec **"FATAL EXCEPTION"** ou **"AndroidRuntime"**

#### Méthode B : Via ligne de commande (si ADB disponible)

```bash
# Dans le dossier platform-tools d'Android SDK
./adb logcat -d | grep -i "skyplayer\|fatal\|crash"
```

#### Méthode C : Sur le téléphone directement

1. Téléchargez l'app **"Logcat Reader"** depuis Play Store
2. Ouvrez l'app et accordez les permissions
3. Lancez Sky Player Pro (qui va crash)
4. Retournez dans Logcat Reader et cherchez l'erreur

---

### 4. Erreurs courantes et solutions

#### Erreur : `google-services.json is missing`
**Solution :** Vérifiez que le fichier est bien dans `app/google-services.json`

#### Erreur : `Default FirebaseApp is not initialized`
**Solution :** Ajoutez dans `build.gradle` (app) :
```kotlin
plugins {
    id("com.google.gms.google-services")
}
```

#### Erreur : `Cannot create an instance of class... ViewModel`
**Solution :** Problème Hilt - vérifiez que tous les @Inject sont corrects

#### Erreur : `ClassNotFoundException`
**Solution :** Problème de multidex ou de librairie manquante

---

### 5. Solution temporaire pour test

Créez une version "minimal" sans Firebase :

**Dans `SkyPlayerApplication.kt` :**
```kotlin
override fun onCreate() {
    super.onCreate()
    
    if (BuildConfig.DEBUG) {
        Timber.plant(Timber.DebugTree())
    }
    
    // Désactiver Firebase temporairement
    /*
    try {
        FirebaseApp.initializeApp(this)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        Timber.i("🔥 Firebase initialisé avec succès")
    } catch (e: Exception) {
        Timber.e(e, "❌ Erreur initialisation Firebase: ${e.message}")
    }
    */
    
    Timber.d("📱 Sky Player Pro démarré (sans Firebase)")
}
```

**Dans `build.gradle` (app) :**
```kotlin
// Commentez Firebase temporairement
// implementation("com.google.firebase:firebase-database-ktx:20.3.0")
// implementation("com.google.firebase:firebase-analytics-ktx:21.5.0")
```

**Reconstruisez l'APK :**
```bash
./gradlew clean assembleDebug
```

---

## Prochaines étapes

1. **Obtenez l'erreur exacte** depuis Logcat (Android Studio ou app Logcat Reader)
2. **Notez la ligne exacte** qui cause le crash
3. **Envoyez-moi l'erreur** pour que je puisse la corriger

---

## Alternative : Build sans Firebase

Si vous voulez un APK qui fonctionne immédiatement sans Firebase :

1. Je peux créer une version "offline" sans Firebase
2. L'app fonctionnera avec les playlists locales uniquement
3. Vous pourrez ajouter Firebase plus tard

Dites-moi si vous voulez cette option !
