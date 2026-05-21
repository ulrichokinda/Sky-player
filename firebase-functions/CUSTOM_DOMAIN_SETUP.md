# 🌐 Configuration Domaine Personnalisé pour Cloud Functions

## Objectif : Utiliser `https://skyplayerapp.xyz/api/pawapay` au lieu de `cloudfunctions.net`

---

## ✅ Étape 1 : Installer Firebase Hosting

Dans votre projet principal (pas firebase-functions) :

```bash
cd C:\Users\HP\CascadeProjects\SkyPlayerPro
firebase init hosting
```

**Réponses aux questions :**
- `? What do you want to use as your public directory?` → `public` (ou `dist`)
- `? Configure as a single-page app?` → `Yes`
- `? Set up automatic builds and deploys with GitHub?` → `No` (ou Yes si vous voulez)

---

## ✅ Étape 2 : Créer fichier `firebase.json` (racine projet)

```json
{
  "hosting": {
    "site": "skyplayerapp",
    "public": "public",
    "ignore": [
      "firebase.json",
      "**/.*",
      "**/node_modules/**"
    ],
    "rewrites": [
      {
        "source": "/api/pawapay",
        "function": "pawapayWebhook"
      },
      {
        "source": "/api/check-status",
        "function": "checkPaymentStatus"
      },
      {
        "source": "/api/**",
        "function": "pawapayWebhook"
      }
    ],
    "headers": [
      {
        "source": "/api/**",
        "headers": [
          {
            "key": "Access-Control-Allow-Origin",
            "value": "*"
          }
        ]
      }
    ]
  },
  "functions": [
    {
      "source": "firebase-functions",
      "codebase": "pawapay-integration",
      "runtime": "nodejs22"
    }
  ]
}
```

---

## ✅ Étape 3 : Configurer Votre Domaine

### Méthode A : Via Firebase Console (Recommandé)

1. Allez sur : https://console.firebase.google.com/project/skyplayer-60634/hosting
2. Cliquez sur **"Add custom domain"**
3. Entrez : `skyplayerapp.xyz`
4. Suivez les instructions DNS

### Méthode B : Via CLI

```bash
firebase hosting:channel:deploy production
firebase hosting:clone skyplayerapp:production skyplayerapp:live
```

---

## ✅ Étape 4 : Configuration DNS (Chez Votre Registrar)

Ajoutez ces enregistrements DNS pour `skyplayerapp.xyz` :

### Type A Records
```
Host: @
Value: 199.36.158.100
```

### Type AAAA Records (IPv6)
```
Host: @
Value: 2001:4860:4802:32::100
```

### OU Utiliser les Nameservers Firebase
```
ns1.firebaseapp.com
ns2.firebaseapp.com
```

---

## ✅ Étape 5 : Déployer

```bash
# 1. D'abord les functions
firebase deploy --only functions

# 2. Puis le hosting avec le domaine
firebase deploy --only hosting
```

---

## 🎯 URLs Finales

| Avant | Après |
|-------|-------|
| `https://us-central1-skyplayer-60634.cloudfunctions.net/pawapayWebhook` | `https://skyplayerapp.xyz/api/pawapay` |
| `https://us-central1-skyplayer-60634.cloudfunctions.net/checkPaymentStatus` | `https://skyplayerapp.xyz/api/check-status` |

---

## 🔒 SSL/HTTPS (Automatique)

Firebase provisionne automatiquement un **certificat SSL gratuit** pour `skyplayerapp.xyz` via Let's Encrypt.

Aucune action requise de votre part !

---

## 📝 Configuration PawaPay

Dans votre Dashboard PawaPay, utilisez maintenant :

```
https://skyplayerapp.xyz/api/pawapay
```

---

## ⚡ Alternative : Cloud Run (Plus Flexible)

Si vous voulez plus de contrôle, déployez sur **Cloud Run** :

```bash
# Construire l'image Docker
gcloud builds submit --tag gcr.io/skyplayer-60634/pawapay-functions

# Déployer sur Cloud Run
gcloud run deploy pawapay-functions \
  --image gcr.io/skyplayer-60634/pawapay-functions \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars "FIREBASE_CONFIG={...}"
```

Puis mappez votre domaine dans **Cloud Console > Cloud Run > Domain Mappings**

---

## 🧪 Test après Configuration

```bash
# Test avec votre domaine personnalisé
curl -X POST https://skyplayerapp.xyz/api/pawapay \
  -H "Content-Type: application/json" \
  -d '{
    "depositId": "TEST_001",
    "status": "COMPLETED",
    "amount": 1000,
    "currency": "XOF",
    "metadata": {
      "licenseId": "LICENSE_TEST"
    }
  }'
```

---

## ❌ Problèmes Courants

| Problème | Solution |
|----------|----------|
| "Domain already in use" | Vérifiez si le domaine est déjà sur un autre projet Firebase |
| DNS ne propage pas | Attendez 24-48h ou vérifiez avec `dig skyplayerapp.xyz` |
| SSL Error | Attendez que Let's Encrypt provisionne (peut prendre quelques heures) |
| 404 Not Found | Vérifiez les `rewrites` dans `firebase.json` |

---

## 📚 Documentation

- [Firebase Hosting Custom Domain](https://firebase.google.com/docs/hosting/custom-domain)
- [Connect Cloud Functions to Hosting](https://firebase.google.com/docs/hosting/functions)
- [Cloud Run Domain Mapping](https://cloud.google.com/run/docs/mapping-custom-domains)

---

**🎉 Avec cette configuration, vos webhooks PawaPay utiliseront votre domaine `skyplayerapp.xyz` !**
