# Guide de Compilation Sky Player Pro

## 1. Accès Local (Navigateur)
L'adresse `0.0.0.0` est une adresse système. Sur votre ordinateur, vous devez utiliser :
👉 **http://localhost:3000**

---

## 2. Compilation Android & Android TV
Le projet utilise **Capacitor**. Voici les étapes :

1. **Pré-requis :** Installez [Android Studio](https://developer.android.com/studio).
2. **Build du projet :**
   ```bash
   npm run build
   ```
3. **Synchronisation :**
   ```bash
   npx cap sync
   ```
4. **Ouverture dans Android Studio :**
   ```bash
   npx cap open android
   ```
5. **Android TV :** Dans Android Studio, ouvrez le fichier `AndroidManifest.xml` et assurez-vous que la bannière TV est présente et que le mode "Touchscreen" n'est pas requis.

---

## 3. Smart TV Samsung (Tizen)
1. **Pré-requis :** Installez [Tizen Studio](https://developer.tizen.org/development/tizen-studio/download).
2. **Build :** Utilisez le dossier `dist/` généré par `npm run build`.
3. **Packaging :** Créez un projet "Web Application" dans Tizen Studio et importez le contenu de `dist/`.

---

## 4. Smart TV LG (webOS)
1. **Pré-requis :** Installez le [webOS TV SDK](https://webostv.developer.lge.com/sdk/installation).
2. **Configuration :** Créez un fichier `appinfo.json` dans le dossier `dist/`.
3. **Packaging :**
   ```bash
   ares-package dist/
   ```

---

## 5. Dépendances de Compilation
Pour installer les outils de compilation nécessaires sur votre PC :
```bash
npm install -g @capacitor/cli
npm install @capacitor/android
```
