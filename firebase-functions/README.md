# 🔥 Firebase Cloud Functions - Joboost Cash Integration

Ce projet contient les Cloud Functions pour recevoir les callbacks de paiement Joboost Cash et mettre à jour les statuts de licence dans Firebase Realtime Database.

## 📁 Structure du Projet

```text
firebase-functions/
├── src/
│   └── index.ts
├── lib/
├── package.json
├── tsconfig.json
└── README.md
```

## 🚀 Fonctions Disponibles

### 1. `joboostCashWebhook` (HTTP)
- **Type**: `functions.https.onRequest`
- **Méthode**: `POST`
- **Endpoint**: `https://<region>-<project-id>.cloudfunctions.net/joboostCashWebhook`
- **Description**: Reçoit les callbacks Joboost Cash et met à jour `isActive`

### 2. `checkPaymentStatus` (HTTP)
- **Type**: `functions.https.onRequest`
- **Méthode**: `GET`
- **Endpoint**: `https://<region>-<project-id>.cloudfunctions.net/checkPaymentStatus?licenseId=xxx`

### 3. `updateLicenseStatus` (Callable)
- **Type**: `functions.https.onCall`
- **Description**: Mise à jour manuelle d’une licence par un admin authentifié

### 4. `verifyJoboostCashPayment` (Callable)
- **Type**: `functions.https.onCall`
- **Description**: Point d’extension pour interroger l’API Joboost Cash quand l’endpoint exact sera connu

## 📋 Installation

### Prérequis
- Node.js 20 recommandé
- Firebase CLI
- Projet Firebase configuré

### Étapes

```bash
npm install
npm run build
firebase functions:secrets:set JOBOOST_CASH_WEBHOOK_SECRET
firebase functions:secrets:set JOBOOST_CASH_API_TOKEN
firebase deploy --only functions
```

> Runtime configuré dans `SkyPlayerPro/firebase.json` : `nodejs20`.

## 🔧 Configuration

### Webhook Joboost Cash
URL recommandée via Hosting:

```text
https://skyplayerapp.xyz/api/joboost-cash
```

Header de signature attendu:

```text
X-Joboost-Cash-Signature
```

Si `JOBOOST_CASH_WEBHOOK_SECRET` est configuré, la signature devient obligatoire et est vérifiée sur le `raw body` exact reçu.

## 🗃️ Données stockées en base

Exemple sous `licenses/<licenseId>` :

```json
{
  "isActive": true,
  "paymentProvider": "joboost-cash",
  "paymentStatus": "COMPLETED",
  "lastPaymentUpdate": 1234567890000,
  "providerPaymentId": "TX_123456",
  "phoneNumber": "+22501234567",
  "amount": 1000,
  "currency": "XOF",
  "paymentTimestamp": "2024-01-15T10:30:00Z"
}
```

## 🧪 Exemple de payload accepté

```json
{
  "transactionId": "TX_123456789",
  "status": "COMPLETED",
  "phoneNumber": "+22501234567",
  "amount": 1000,
  "currency": "XOF",
  "timestamp": "2024-01-15T10:30:00Z",
  "metadata": {
    "licenseId": "LICENSE_001"
  }
}
```

Le webhook accepte aussi `paymentId` ou `depositId` si `transactionId` n’est pas fourni.

## ⚠️ Limite actuelle

`verifyJoboostCashPayment` est préparée mais pas branchée à une API distante, car l’URL exacte et le contrat de l’API Joboost Cash ne sont pas encore définis dans le projet.

Quand tu me donnes l’endpoint exact et le format de réponse Joboost Cash, je peux finaliser cette fonction.
