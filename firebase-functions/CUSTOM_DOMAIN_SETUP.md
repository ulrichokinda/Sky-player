# 🌐 Configuration Domaine Personnalisé pour Cloud Functions

## Objectif
Utiliser `https://skyplayerapp.xyz/api/joboost-cash` au lieu de l’URL `cloudfunctions.net`.

## Étape 1 — Initialiser Hosting
Depuis la racine du projet :

```bash
cd C:\Users\HP\CascadeProjects\SkyPlayerPro
firebase init hosting
```

Réponses recommandées :
- public directory : `public`
- single-page app : `Yes`
- GitHub deploys : `No`

## Étape 2 — Vérifier la config racine
Le fichier `SkyPlayerPro/firebase.json` doit exposer :

- `/api/joboost-cash` → `joboostCashWebhook`
- `/api/check-status` → `checkPaymentStatus`
- `/api/update-status` → `updateLicenseStatus`

Et utiliser le runtime :

```json
"runtime": "nodejs20"
```

## Étape 3 — Ajouter le domaine personnalisé
Dans Firebase Console > Hosting :
- ajouter `skyplayerapp.xyz`
- suivre les instructions DNS affichées par Firebase

## Étape 4 — Déployer

```bash
firebase deploy --only functions
firebase deploy --only hosting
```

## URLs finales

| Usage | URL |
|---|---|
| Webhook Joboost Cash | `https://skyplayerapp.xyz/api/joboost-cash` |
| Vérification statut | `https://skyplayerapp.xyz/api/check-status?licenseId=...` |

## Header de signature attendu

```text
X-Joboost-Cash-Signature
```

## Exemple de test

```bash
curl -X POST https://skyplayerapp.xyz/api/joboost-cash \
  -H "Content-Type: application/json" \
  -H "X-Joboost-Cash-Signature: <signature_hmac_sha256_hex>" \
  -d '{
    "transactionId": "TEST_001",
    "status": "COMPLETED",
    "amount": 1000,
    "currency": "XOF",
    "metadata": {
      "licenseId": "LICENSE_TEST"
    }
  }'
```

## Remarque
Si `JOBOOST_CASH_WEBHOOK_SECRET` est configuré, la signature est obligatoire et vérifiée sur le `raw body` exact reçu par la function.
