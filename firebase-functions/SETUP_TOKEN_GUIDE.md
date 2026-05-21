# 🔐 Guide Complet : Configurer le Token PawaPay sur Firebase

## ÉTAPE 1 : Ouvrir le Terminal

### Méthode A : Terminal VS Code (Recommandé)
1. Dans VS Code, appuyez sur : **`Ctrl + ` `** (touche backtick sous Echap)
2. OU cliquez sur : **Terminal** → **New Terminal**

### Méthode B : Terminal Windows
1. Appuyez sur : **`Win + R`**
2. Tapez : **`cmd`**
3. Appuyez sur : **Entrée**

---

## ÉTAPE 2 : Aller dans le Bon Dossier

Dans le terminal, tapez EXACTEMENT ceci (puis appuyez sur Entrée) :

```bash
cd C:\Users\HP\CascadeProjects\SkyPlayerPro\firebase-functions
```

**Vérifiez que vous êtes dans le bon dossier :**
```bash
pwd
```
→ Doit afficher : `C:\Users\HP\CascadeProjects\SkyPlayerPro\firebase-functions`

---

## ÉTAPE 3 : Vérifier que Firebase CLI est Installé

Tapez :
```bash
firebase --version
```

**Si vous voyez un numéro de version** (ex: `13.0.0`) → ✅ Passez à l'étape 4

**Si vous voyez une erreur** → Installez Firebase CLI :
```bash
npm install -g firebase-tools
```

---

## ÉTAPE 4 : Se Connecter à Firebase (Si pas déjà fait)

Tapez :
```bash
firebase login
```

**Ce qui va se passer :**
1. Une fenêtre de navigateur s'ouvre automatiquement
2. Connectez-vous avec votre compte Google (celui du projet Firebase)
3. Cliquez sur **"Autoriser"** ou **"Allow"**
4. Retournez dans le terminal → Vous devriez voir **"Success!"**

---

## ÉTAPE 5 : Configurer le Token API PawaPay ⭐

**C'est l'étape la plus importante !**

Dans le terminal, copiez-collez cette commande ENTIÈRE (sur une seule ligne) :

```bash
firebase functions:config:set pawapay.api_token="eyJraWQiOiIxIiwiYWxnIjoiRVMyNTYifQ.eyJ0dCI6IkFBVCIsInN1YiI6IjIwMjcyIiwibWF2IjoiMSIsImV4cCI6MjA5MzY5MDc1NCwiaWF0IjoxNzc4MDcxNTU0LCJwbSI6IkRBRixQQUYiLCJqdGkiOiIwZTFhZGQ3Yi1jNjIzLTQxYjQtODI4OC05NmNiZGYwOGIyZjEifQ.Owr2S681bp4kH_TB9MEXoZ5G0y_5FYL1LPnU9kv-eTsWNUJ1eSDso7oXhpZF9rQbvhtB_4cHP9QBb3Oa8uMXVA"
```

**Appuyez sur Entrée**

**Si tout va bien, vous verrez :**
```
✔  Functions config updated.
    Project: skyplayer-60634
    Config: 
      pawapay.api_token: eyJraWQiOiIxIiwiYWxnI...[masqué]
```

---

## ÉTAPE 6 : Vérifier que le Token est bien Enregistré

Tapez :
```bash
firebase functions:config:get
```

**Vous devriez voir :**
```json
{
  "pawapay": {
    "api_token": "eyJraWQiOiIxIiwiYWxnI..."
  }
}
```

✅ **Le token est maintenant sécurisé sur Firebase !**

---

## ÉTAPE 7 : Redéployer les Functions

Maintenant que le token est configuré, vous DEVEZ redéployer :

### 7.1 Compiler le code TypeScript
```bash
npm run build
```

**Attendez que vous voyiez :**
```
> tsc

[terminé sans erreur]
```

### 7.2 Déployer sur Firebase
```bash
firebase deploy --only functions
```

**Attendez le message :**
```
✔  Deploy complete!

Project Console: https://console.firebase.google.com/project/skyplayer-60634/overview
```

---

## ✅ VÉRIFICATION FINALE

Testez que tout fonctionne avec cette commande :

```bash
firebase functions:log --follow
```

Dans un autre terminal, envoyez un test webhook :
```bash
curl -X POST https://us-central1-skyplayer-60634.cloudfunctions.net/pawapayWebhook \
  -H "Content-Type: application/json" \
  -d '{"depositId":"TEST_001","status":"COMPLETED","amount":1000,"currency":"XOF","metadata":{"licenseId":"TEST_LICENSE"}}'
```

**Dans les logs, vous devriez voir :**
```
📥 Callback PawaPay reçu: {...}
✅ Licence TEST_LICENSE mise à jour - isActive: true
```

---

## 🚨 RÉSOLUTION DES PROBLÈMES

### Problème 1 : "firebase: command not found"
**Solution :**
```bash
npm install -g firebase-tools
```

### Problème 2 : "Error: Not authenticated"
**Solution :**
```bash
firebase login
```
Puis réessayez la commande config.

### Problème 3 : "Error: No project found"
**Solution :**
```bash
firebase use --add
```
Sélectionnez : `skyplayer-60634`

### Problème 4 : Le token est trop long et la commande ne marche pas
**Solution :** Utilisez un fichier temporaire :

1. Créez un fichier `set-token.bat` dans le dossier `firebase-functions`
2. Collez dedans :
```batch
@echo off
firebase functions:config:set pawapay.api_token="eyJraWQiOiIxIiwiYWxnIjoiRVMyNTYifQ.eyJ0dCI6IkFBVCIsInN1YiI6IjIwMjcyIiwibWF2IjoiMSIsImV4cCI6MjA5MzY5MDc1NCwiaWF0IjoxNzc4MDcxNTU0LCJwbSI6IkRBRixQQUYiLCJqdGkiOiIwZTFhZGQ3Yi1jNjIzLTQxYjQtODI4OC05NmNiZGYwOGIyZjEifQ.Owr2S681bp4kH_TB9MEXoZ5G0y_5FYL1LPnU9kv-eTsWNUJ1eSDso7oXhpZF9rQbvhtB_4cHP9QBb3Oa8uMXVA"
pause
```
3. Double-cliquez sur `set-token.bat`

---

## 📋 RÉSUMÉ DES COMMANDES

**Copiez-collez ces commandes une par une dans l'ordre :**

```bash
# 1. Aller dans le dossier
cd C:\Users\HP\CascadeProjects\SkyPlayerPro\firebase-functions

# 2. Se connecter (si pas déjà fait)
firebase login

# 3. Configurer le token
firebase functions:config:set pawapay.api_token="eyJraWQiOiIxIiwiYWxnIjoiRVMyNTYifQ.eyJ0dCI6IkFBVCIsInN1YiI6IjIwMjcyIiwibWF2IjoiMSIsImV4cCI6MjA5MzY5MDc1NCwiaWF0IjoxNzc4MDcxNTU0LCJwbSI6IkRBRixQQUYiLCJqdGkiOiIwZTFhZGQ3Yi1jNjIzLTQxYjQtODI4OC05NmNiZGYwOGIyZjEifQ.Owr2S681bp4kH_TB9MEXoZ5G0y_5FYL1LPnU9kv-eTsWNUJ1eSDso7oXhpZF9rQbvhtB_4cHP9QBb3Oa8uMXVA"

# 4. Vérifier
firebase functions:config:get

# 5. Compiler
npm run build

# 6. Déployer
firebase deploy --only functions
```

---

**🎉 Après ces étapes, votre token PawaPay sera sécurisé et vos fonctions pourront appeler l'API PawaPay !**
