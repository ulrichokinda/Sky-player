# 🔥 Firebase Cloud Functions - PawaPay Integration

Ce projet contient les Cloud Functions pour recevoir les callbacks de paiement PawaPay et mettre à jour les statuts de licence dans Firebase Realtime Database.

## 📁 Structure du Projet

```
firebase-functions/
├── src/
│   └── index.ts          # Cloud Functions principales
├── lib/                  # Code compilé (auto-généré)
├── package.json          # Dépendances
├── tsconfig.json         # Configuration TypeScript
├── firebase.json         # Configuration Firebase
└── README.md             # Documentation
```

## 🚀 Fonctions Disponibles

### 1. `pawapayWebhook` (HTTP)
- **Type**: `functions.https.onRequest`
- **Méthode**: POST
- **Endpoint**: `https://<region>-<project-id>.cloudfunctions.net/pawapayWebhook`
- **Description**: Reçoit les callbacks de PawaPay et met à jour `isActive`

### 2. `checkPaymentStatus` (HTTP)
- **Type**: `functions.https.onRequest`
- **Méthode**: GET
- **Endpoint**: `https://<region>-<project-id>.cloudfunctions.net/checkPaymentStatus?licenseId=xxx`
- **Description**: Vérifie le statut de paiement d'une licence

### 3. `updateLicenseStatus` (Callable)
- **Type**: `functions.https.onCall`
- **Description**: Permet aux admins de mettre à jour manuellement le statut

## 📋 Installation

### Prérequis
- Node.js 20 ou supérieur
- Firebase CLI (`npm install -g firebase-tools`)
- Compte Firebase avec projet configuré

### Étapes

1. **Naviguer dans le dossier**
```bash
cd firebase-functions
```

2. **Installer les dépendances**
```bash
npm install
```

3. **Compiler TypeScript**
```bash
npm run build
```

4. **Se connecter à Firebase**
```bash
firebase login
```

5. **Initialiser le projet** (si ce n'est pas déjà fait)
```bash
firebase init functions
```

6. **Configurer les secrets** (optionnel mais recommandé)
```bash
firebase functions:config:set pawapay.webhook_secret="votre_secret_webhook"
```

7. **Déployer les functions**
```bash
firebase deploy --only functions
```

## 🔧 Configuration

### 1. Configurer le Webhook PawaPay

Dans votre dashboard PawaPay, configurez l'URL de callback :

```
https://<region>-<project-id>.cloudfunctions.net/pawapayWebhook
```

**Regions disponibles**:
- `us-central1` (Iowa)
- `europe-west1` (Belgique)
- `europe-west3` (Francfort)
- `asia-south1` (Mumbai)

### 2. Structure de la Realtime Database

```json
{
  "licenses": {
    "LICENSE_ID_123": {
      "isActive": true,
      "paymentStatus": "COMPLETED",
      "lastPaymentUpdate": 1234567890000,
      "pawapayDepositId": "DEP_123456",
      "pawapayPhoneNumber": "+22501234567",
      "pawapayAmount": 1000,
      "pawapayCurrency": "XOF",
      "pawapayTimestamp": "2024-01-15T10:30:00Z"
    }
  },
  "payment_history": {
    "LICENSE_ID_123": {
      "payment_123": {
        "isActive": true,
        "paymentStatus": "COMPLETED",
        "createdAt": 1234567890000
      }
    }
  }
}
```

## 🔒 Sécurité

### Vérification de Signature
La fonction vérifie automatiquement l'en-tête `X-PawaPay-Signature` si configuré.

### Configuration du Secret
```bash
firebase functions:config:set pawapay.webhook_secret="votre_secret"
```

### Règles de Sécurité (Realtime Database)

```json
{
  "rules": {
    "licenses": {
      "$licenseId": {
        ".read": "auth != null || root.child('licenses/' + $licenseId + '/isActive').val() == true",
        ".write": "auth != null && auth.token.admin == true"
      }
    },
    "payment_history": {
      ".read": "auth != null && auth.token.admin == true",
      ".write": false
    }
  }
}
```

## 📊 Format des Callbacks PawaPay

### Exemple de Payload

```json
{
  "depositId": "DEP_123456789",
  "status": "COMPLETED",
  "phoneNumber": "+22501234567",
  "amount": 1000,
  "currency": "XOF",
  "timestamp": "2024-01-15T10:30:00Z",
  "metadata": {
    "licenseId": "LICENSE_001",
    "userId": "USER_123",
    "plan": "premium"
  }
}
```

### Mapping des Statuts

| Statut PawaPay | isActive | Description |
|----------------|----------|-------------|
| `COMPLETED` | ✅ true | Paiement réussi |
| `SUCCESS` | ✅ true | Paiement confirmé |
| `CONFIRMED` | ✅ true | Transaction validée |
| `ACCEPTED` | ✅ true | Paiement accepté |
| `FAILED` | ❌ false | Échec du paiement |
| `REJECTED` | ❌ false | Paiement refusé |
| `CANCELLED` | ❌ false | Annulé par l'utilisateur |
| `EXPIRED` | ❌ false | Transaction expirée |
| `PENDING` | ❌ false | En attente (temporaire) |

## 🧪 Tests Locaux

### Avec l'Émulateur Firebase

1. **Démarrer les émulateurs**
```bash
npm run serve
```

2. **Tester le webhook**
```bash
curl -X POST http://localhost:5001/<project-id>/<region>/pawapayWebhook \
  -H "Content-Type: application/json" \
  -H "X-PawaPay-Signature: test_signature" \
  -d '{
    "depositId": "DEP_TEST_001",
    "status": "COMPLETED",
    "phoneNumber": "+22501234567",
    "amount": 1000,
    "currency": "XOF",
    "timestamp": "2024-01-15T10:30:00Z",
    "metadata": {
      "licenseId": "TEST_LICENSE_001"
    }
  }'
```

### Vérifier le statut
```bash
curl "http://localhost:5001/<project-id>/<region>/checkPaymentStatus?licenseId=TEST_LICENSE_001"
```

## 📝 Logs et Monitoring

### Voir les logs en temps réel
```bash
firebase functions:log --follow
```

### Logs spécifiques à une fonction
```bash
firebase functions:log --only pawapayWebhook
```

### Dashboard Firebase
Accédez à: [Firebase Console > Functions](https://console.firebase.google.com/project/_/functions)

## 🛠️ Dépannage

### Erreur: "Permission denied"
- Vérifiez que le compte de service Firebase a les droits d'écriture sur la Realtime Database
- Vérifiez les règles de sécurité

### Erreur: "Invalid signature"
- Assurez-vous que `pawapay.webhook_secret` est correctement configuré
- Vérifiez que la signature est calculée correctement côté PawaPay

### Erreur: "licenseId non trouvé"
- Assurez-vous d'envoyer le `licenseId` dans les métadonnées lors de la création du dépôt PawaPay
- Format recommandé: `metadata: { licenseId: "LICENSE_xxx" }`

### Erreur: "Function execution took too long"
- Augmentez le timeout dans `firebase.json`:
```json
{
  "functions": [{
    "timeoutSeconds": 60
  }]
}
```

## 📚 Documentation PawaPay

- [Documentation API PawaPay](https://docs.pawapay.com)
- [Webhooks PawaPay](https://docs.pawapay.com/webhooks)
- [Firebase Functions](https://firebase.google.com/docs/functions)

## 🤝 Support

Pour toute question ou problème :
1. Consultez les logs Firebase Functions
2. Vérifiez la structure de votre Realtime Database
3. Contactez le support PawaPay pour les problèmes de paiement

---

**Version**: 1.0.0  
**Dernière mise à jour**: 2024
