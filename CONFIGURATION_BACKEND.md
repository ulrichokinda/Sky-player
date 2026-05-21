# 🔌 Configuration Connexion Backend

> **But** : Connecter votre application Android à votre backend existant skyplayerapp.xyz

---

## ⚙️ Étape 1 : Configurer l'URL et la Clé API

### Fichier à modifier : `LicenseApiService.kt`

**Emplacement :**
```
app/src/main/java/com/skyplayer/pro/data/remote/LicenseApiService.kt
```

**Modifiez ces 2 lignes :**

```kotlin
companion object {
    // URL de votre backend déployé (DÉJÀ CONFIGURÉ ✅)
    const val BASE_URL = "https://skyplayerapp.xyz/"
    
    // ⚠️ MODIFIEZ CECI : Mettez votre vraie clé API
    const val API_KEY = "votre-cle-api-a-mettre-ici" 
}
```

### Où trouver votre clé API ?

Votre clé API est définie dans la configuration de votre backend (skyplayerapp.xyz) :

1. Connectez-vous à votre serveur
2. Regardez dans votre fichier de configuration (`.env` ou `ecosystem.config.js`)
3. Copiez la valeur de `API_KEY`
4. Collez-la dans `LicenseApiService.kt`

**Exemple :**
```kotlin
const val API_KEY = "skyplayer-2024-secret-key-xyz123"
```

---

## 🔄 Étape 2 : Choisir le Mode de Connexion

Vous avez **2 options** pour la licence :

### Option A : Backend API uniquement (Recommandé)
Votre app communique UNIQUEMENT avec skyplayerapp.xyz

**Avantages :**
- ✅ Plus sécurisé (Firebase caché derrière votre backend)
- ✅ Vous contrôlez tout
- ✅ Peut fonctionner avec d'autres bases de données plus tard

**Dans `LicenseViewModel.kt`, utilisez :**
```kotlin
private val backendRepository: LicenseBackendRepository
```

### Option B : Backend + Firebase (Hybride)
Votre app utilise les DEUX (backend pour check, Firebase pour temps réel)

**Avantages :**
- ✅ Temps réel Firebase (révocation instantanée)
- ✅ Backup si backend down

**C'est ce qui est déjà configuré par défaut.**

---

## 🧪 Étape 3 : Tester la Connexion

### 1. Vérifier que le backend répond

Dans votre navigateur, testez :
```
https://skyplayerapp.xyz/api/health
```

**Résultat attendu :**
```json
{
  "status": "OK",
  "timestamp": "2024-...",
  "service": "SkyPlayer Activation Service"
}
```

### 2. Compiler l'app

```bash
./gradlew :app:assembleDebug
```

### 3. Tester sur un vrai téléphone

1. Installez l'APK
2. Ouvrez l'app
3. Allez dans **Paramètres** → **Licence & Activation**
4. Vous devriez voir :
   - L'ID appareil
   - Statut de connexion au backend
   - Nombre de jours d'essai restants

### 4. Vérifier les logs

Dans Android Studio, filtrez les logs avec `tag:License` :

```
🏥 Backend health: OK
✅ Backend: Licence vérifiée - Active: false, Essai: 15j
```

---

## 🔧 Dépannage

### Problème : "Failed to connect to skyplayerapp.xyz"

**Causes possibles :**
1. **Pas d'internet sur le téléphone** → Vérifiez connexion WiFi/4G
2. **Backend hors ligne** → Vérifiez sur votre serveur : `pm2 status`
3. **URL incorrecte** → Vérifiez `BASE_URL` dans `LicenseApiService.kt`
4. **Problème SSL** → Assurez-vous que HTTPS fonctionne (certificat valide)

**Test depuis le téléphone :**
```bash
# Si vous avez ADB
curl https://skyplayerapp.xyz/api/health
```

### Problème : "Invalid API Key"

**Solution :**
1. Vérifiez que la clé dans l'APP match celle dans le backend
2. Dans `LicenseApiService.kt` : `const val API_KEY = "..."`
3. Dans votre backend `.env` : `API_KEY=...`
4. Les deux doivent être **identiques**

### Problème : "Device not found"

**Normal !** Cela signifie que l'appareil n'existe pas encore dans la base.

**Solution :**
1. L'app s'enregistre automatiquement dans Firebase
2. Ou vous pouvez l'ajouter manuellement dans votre backend
3. Puis réessayez la vérification

---

## 📊 Architecture Finale

```
┌─────────────────┐
│  App Android    │  ← LicenseApiService.kt
│  SkyPlayer Pro  │     fait des appels API
└────────┬────────┘
         │ HTTPS
         ↓
┌─────────────────────────────┐
│   skyplayerapp.xyz          │  ← Votre backend existant
│   (Votre serveur)           │     Node.js/Express
│   - Reçoit les requêtes     │
│   - Vérifie dans Firebase   │
│   - Répond JSON             │
└────────┬────────────────────┘
         │
         ↓ (en interne)
┌─────────────────────────────┐
│   Firebase Realtime DB      │  ← Base de données
│   (skyplayer-60634)         │
└─────────────────────────────┘
```

---

## 🔐 Sécurité

### Clé API
- ✅ Gardée dans le code (obfusquée en release)
- ✅ Vérifiée côté backend
- ❌ Ne JAMAIS la mettre dans un fichier public

### HTTPS (SSL)
- ✅ Obligatoire en production
- ✅ Votre backend a déjà Let's Encrypt
- ✅ Les données sont chiffrées

### Certificate Pinning (Optionnel)
Pour plus de sécurité, vous pouvez "épingler" le certificat SSL dans l'app :

```kotlin
// Dans LicenseApiService.kt (avancé)
.certificatePinner(
    CertificatePinner.Builder()
        .add("skyplayerapp.xyz", "sha256/...")
        .build()
)
```

---

## 📝 Résumé des Fichiers

| Fichier | Rôle | À Modifier ? |
|---------|------|--------------|
| `LicenseApiService.kt` | Définit l'URL et les endpoints | ✅ Oui (clé API) |
| `LicenseBackendRepository.kt` | Logique de communication | ❌ Non |
| `AppModule.kt` | Injection Retrofit | ❌ Non |
| `LicenseViewModel.kt` | Utilise le repository | ❌ Non |

---

## ✅ Checklist Connexion

- [ ] Backend skyplayerapp.xyz accessible (testez `/api/health`)
- [ ] Clé API copiée depuis le backend vers `LicenseApiService.kt`
- [ ] `BASE_URL` correcte (`https://skyplayerapp.xyz/`)
- [ ] APK compilé sans erreur
- [ ] Test sur téléphone réussi
- [ ] Logs montrent connexion OK

---

## 🆘 Besoin d'aide ?

### Vérifier que le backend fonctionne :
```bash
# Sur votre serveur
pm2 status
pm2 logs
```

### Vérifier la connexion depuis l'app :
Dans Android Studio → Logcat → Filtre `tag:License`

### Erreur réseau ?
- Vérifiez les permissions internet dans `AndroidManifest.xml`
- Testez avec un autre URL (https://httpbin.org/get)

---

## 🎉 Prochaines Étapes

1. ✅ **Configurer la clé API** dans `LicenseApiService.kt`
2. ⏭️ **Compiler et tester** l'APK
3. ⏭️ **Vérifier** les logs de connexion
4. ⏭️ **Déployer** sur Play Store ou distribuer aux clients

**Votre app est maintenant connectée à votre backend !** 🚀
