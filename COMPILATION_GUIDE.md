# Guide de Compilation Complet : Sky Player Pro

Ce guide vous accompagne pour compiler et préparer la soumission de votre application sur les 4 plateformes majeures.

---

## 🛠️ ÉTAPE 0 : Préparation Universelle
Avant toute compilation, vous devez générer les fichiers web finaux :
```bash
npm run build
```
*Le dossier `dist/` contient maintenant votre application prête à être packagée.*

---

## 🎨 0. CONFIGURATION DE L'ICÔNE (À faire AVANT de compiler)
Pour que votre logo apparaisse comme icône de l'application :

1. **Préparez votre logo :** Enregistrez votre logo au format PNG carré (1024x1024px) et nommez-le `icon.png`.
2. **Placez-le :** Mettez ce fichier dans un dossier nommé `assets/` à la racine de votre projet.
3. **Générez les icônes Android :**
   ```bash
   npm install @capacitor/assets -D
   npx capacitor-assets generate --android
   ```
   *Cela va créer automatiquement toutes les tailles d'icônes pour votre APK.*

4. **Pour Samsung & LG :** Copiez votre `icon.png` dans le dossier `dist/` avant de packager.

---

## 🤖 1. ANDROID & ANDROID TV (Capacitor)

### Pré-requis :
- [Android Studio](https://developer.android.com/studio) installé.
- SDK Android 34+ installé via Android Studio.

### Étapes :
1. **Ajouter la plateforme :**
   ```bash
   npx cap add android
   ```
2. **Synchroniser le code :**
   ```bash
   npx cap sync
   ```
3. **Ouvrir dans Android Studio :**
   ```bash
   npx cap open android
   ```
4. **Configuration Android TV (CRITIQUE) :**
   Dans Android Studio, ouvrez `app/src/main/AndroidManifest.xml` :
   - Ajoutez : `<uses-feature android:name="android.software.leanback" android:required="false" />`
   - Ajoutez : `<uses-feature android:name="android.hardware.touchscreen" android:required="false" />`
   - Dans `<activity>`, ajoutez : `<intent-filter> <category android:name="android.intent.category.LEANBACK_LAUNCHER" /> </intent-filter>`
5. **Générer l'APK/Bundle :**
   - `Build` > `Generate Signed Bundle / APK`.

---

## 📺 2. SAMSUNG SMART TV (Tizen Studio)

### Pré-requis :
- [Tizen Studio](https://developer.tizen.org/development/tizen-studio/download) avec "TV Extensions".

### Étapes :
1. **Créer le projet :**
   - Ouvrez Tizen Studio > `File` > `New` > `Tizen Project`.
   - Choisissez `Template` > `TV` > `Web Application`.
2. **Importer le code :**
   - Supprimez le contenu du dossier du projet Tizen.
   - Copiez TOUT le contenu de votre dossier `dist/` (généré à l'étape 0) dans le dossier du projet Tizen.
3. **Configurer le `config.xml` :**
   - Assurez-vous que l'ID de l'application est unique.
   - Ajoutez les privilèges réseau : `http://tizen.org/privilege/internet`.
4. **Build & Package :**
   - Clic droit sur le projet > `Build Package`.
   - Cela génère un fichier `.wgt` prêt pour le Samsung Seller Office.

---

## 📡 3. LG SMART TV (webOS TV SDK)

### Pré-requis :
- [webOS TV SDK](https://webostv.developer.lge.com/sdk/installation) (CLI Tools).

### Étapes :
1. **Créer le fichier `appinfo.json` :**
   Créez ce fichier dans votre dossier `dist/` :
   ```json
   {
     "id": "com.skyplayer.app",
     "version": "1.0.0",
     "vendor": "SkyPlayer",
     "type": "web",
     "main": "index.html",
     "title": "Sky Player Pro",
     "icon": "icon.png",
     "largeIcon": "largeIcon.png",
     "bgImage": "backgroundImage.png"
   }
   ```
2. **Packager l'application :**
   Ouvrez votre terminal dans le dossier racine et tapez :
   ```bash
   ares-package dist/
   ```
3. **Résultat :**
   Cela génère un fichier `.ipk` prêt pour le LG Content Store.

---

## 🚀 4. SOUMISSION (Checklist Finale)

### Éléments requis pour TOUS les stores :
1. **Icônes :** 512x512px (Android), 1920x1080px (Bannière TV).
2. **Captures d'écran :** Au moins 4 captures de l'interface (Lecteur, Dashboard, Home).
3. **Description :** Texte clair expliquant que c'est un lecteur (sans contenu illégal).
4. **Privacy Policy :** Une URL vers votre politique de confidentialité (obligatoire).

### Liens des consoles de développeur :
- **Google Play :** [Play Console](https://play.google.com/console) (25$ une fois).
- **Samsung TV :** [Seller Office](https://seller.samsungapps.com/tv) (Gratuit).
- **LG TV :** [LG Seller Lounge](https://seller.lgappstv.com/) (Gratuit).
