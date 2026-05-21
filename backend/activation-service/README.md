# SkyPlayer Activation Service

Backend de production pour `skyplayerapp.xyz` qui active/desactive les appareils dans Firebase Realtime Database.

## 1) Installation

```bash
cd backend/activation-service
npm install
```

## 2) Configuration

```bash
cp .env.example .env
```

Renseigner:

- `PORT`
- `FIREBASE_DATABASE_URL`
- `ACTIVATION_API_KEY`
- `FIREBASE_SERVICE_ACCOUNT_JSON`

## 3) Demarrage

```bash
npm run start
```

## 4) API

### Health

```bash
curl http://localhost:8787/health
```

### Activer un appareil

```bash
curl -X POST http://localhost:8787/activate \
  -H "Content-Type: application/json" \
  -H "x-api-key: change-me-prod" \
  -d "{\"deviceId\":\"AA:BB:CC:DD:EE:FF:00:11\",\"activatedBy\":\"ops@skyplayerapp.xyz\"}"
```

### Desactiver un appareil

```bash
curl -X POST http://localhost:8787/deactivate \
  -H "Content-Type: application/json" \
  -H "x-api-key: change-me-prod" \
  -d "{\"deviceId\":\"AA:BB:CC:DD:EE:FF:00:11\"}"
```

### Lire une licence

```bash
curl http://localhost:8787/license/AA:BB:CC:DD:EE:FF:00:11 \
  -H "x-api-key: change-me-prod"
```

## 5) Notes production

- Mettre le service derriere HTTPS (reverse proxy / cloud load balancer).
- Restreindre l'IP source si possible (backend paiement uniquement).
- Tourner `ACTIVATION_API_KEY` regulierement.
- Journaliser chaque activation/desactivation pour audit.
