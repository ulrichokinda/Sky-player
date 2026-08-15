# 🔐 Guide : configurer les secrets Joboost Cash sur Firebase

Ce guide documente la configuration actuelle basée sur Firebase Secrets pour l’intégration Joboost Cash.

## Secrets utilisés

- `JOBOOST_CASH_WEBHOOK_SECRET`
- `JOBOOST_CASH_API_TOKEN`

## Étapes

### 1. Aller dans le dossier
```bash
cd C:\Users\HP\CascadeProjects\SkyPlayerPro\firebase-functions
```

### 2. Se connecter à Firebase
```bash
firebase login
```

### 3. Définir le secret webhook
```bash
firebase functions:secrets:set JOBOOST_CASH_WEBHOOK_SECRET
```

### 4. Définir le token API
```bash
firebase functions:secrets:set JOBOOST_CASH_API_TOKEN
```

### 5. Compiler
```bash
npm install
npm run build
```

### 6. Déployer
```bash
firebase deploy --only functions
```

## Vérification rapide

### Tester le webhook
```bash
curl -X POST https://us-central1-skyplayer-60634.cloudfunctions.net/joboostCashWebhook \
  -H "Content-Type: application/json" \
  -H "X-Joboost-Cash-Signature: <signature_hmac_sha256_hex>" \
  -d '{"transactionId":"TEST_001","status":"COMPLETED","amount":1000,"currency":"XOF","metadata":{"licenseId":"TEST_LICENSE"}}'
```

### Logs
```bash
firebase functions:log --only joboostCashWebhook
```

## Important
- Ne stocke jamais un token réel en dur dans ce fichier.
- Si tu modifies `package.json`, pense à relancer `npm install` pour resynchroniser `package-lock.json`.
- `verifyJoboostCashPayment` reste à finaliser dès que l’API distante exacte de Joboost Cash est connue.
