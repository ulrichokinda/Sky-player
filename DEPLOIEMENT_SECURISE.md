# Deploiement Production - SkyPlayer Pro

Ce document est la reference de mise en production (app Android + activation web `skyplayerapp.xyz` + Firebase RTDB).

## 1) Pre-check obligatoire

- Verifier que `database.rules.json` contient les regles durcies.
- Verifier que `firebase.json` pointe bien sur `database.rules.json`.
- Verifier que la signature release Android est prete.
- Verifier que le backend d'activation utilise un compte service Firebase Admin (jamais expose cote client).

## 2) Deployer les regles Firebase

Depuis la racine du projet:

```bash
firebase login
firebase use --add
firebase deploy --only database
```

Alternative script PowerShell:

```powershell
./scripts/deploy-rules.ps1
```

## 3) Configurer le backend d'activation du site

Un service Node.js pret est fourni dans `backend/activation-service`.

### Variables d'environnement

Copier `backend/activation-service/.env.example` en `.env` puis renseigner:

- `PORT` (ex: 8787)
- `FIREBASE_DATABASE_URL` (URL RTDB)
- `FIREBASE_SERVICE_ACCOUNT_JSON` (JSON complet du service account, echappe)
- `ACTIVATION_API_KEY` (cle secrete pour ton site)

### Lancement local

```bash
cd backend/activation-service
npm install
npm run start
```

## 4) Flux production recommande (site -> app)

1. Le client copie son ID appareil depuis l'app.
2. Il paie sur `https://skyplayerapp.xyz`.
3. Le backend du site appelle l'endpoint d'activation securise.
4. Le backend ecrit dans `licenses/{deviceId}`:
   - `isActive: true`
   - `activatedBy: ...`
   - `activationDate: Date.now()`
5. L'app detecte l'activation via Firebase en temps reel.

## 5) Endpoints backend fournis

- `POST /activate`
  - body: `deviceId`, `activatedBy`
  - header: `x-api-key`
- `POST /deactivate`
  - body: `deviceId`
  - header: `x-api-key`
- `GET /license/:deviceId`
  - header: `x-api-key`
- `GET /health`
  - pas d'auth

## 6) Checklist go-live

- [ ] Regles Firebase deployees (plus de `.read/.write = true` global)
- [ ] Backend activation en ligne + protege par API key
- [ ] Test activation reelle d'un device de recette
- [ ] Test desactivation et blocage en temps reel
- [ ] Build release APK (`./gradlew :app:assembleRelease`)
- [ ] Verification manuelle ecran licence + ouverture `skyplayerapp.xyz`

## 7) Rollback d'urgence

Si incident critique:

1. Bloquer temporairement la base dans Firebase Console:

```json
{
  "rules": {
    ".read": false,
    ".write": false
  }
}
```

2. Corriger backend/regles.
3. Redeployer les regles officielles.

## 8) Notes securite importantes

- Ne jamais embarquer la cle admin Firebase dans l'app Android.
- Ne jamais exposer `FIREBASE_SERVICE_ACCOUNT_JSON` cote frontend.
- Regenerer l'API key si soupcon de fuite.
- Logger activations/desactivations cote backend pour audit.
- `backend/config.php` est exclu de Git : ses secrets sont charges uniquement via l'environnement (voir section 9).

## 9) Backend PHP — variables d'environnement obligatoires

Le backend PHP (`check_mac.php`, `api/devices/check.php`, `reseller/`) refuse de demarrer si un secret manque.
Copier `backend/config.example.php` vers `backend/config.php` (jamais committe) et definir :

| Variable | Role |
|----------|------|
| `DB_HOST` | Hote MySQL (defaut `localhost`) |
| `DB_NAME` | Base (defaut `skyplayer_db`) |
| `DB_USER` | Utilisateur MySQL (**requis**) |
| `DB_PASS` | Mot de passe MySQL (**requis**) |
| `APK_SECRET_KEY` | Cle secrete webhooks/admin (**requis**) |
| `RESELLER_USER` | Login dashboard revendeur (**requis**) |
| `RESELLER_PASS` | Mot de passe revendeur, jamais `admin123` (**requis**) |
| `TRIAL_DAYS` | Duree d'essai en jours (defaut 14, aligne sur l'app) |
| `APP_KEY` | Cle applicative supplementaire (optionnel) |

Exemples : cPanel (section variables d'environnement), Apache `SetEnv`, nginx `fastcgi_param`.

### Endpoints PHP

- `GET/POST /api/playlist/check_mac.php?mac_address=XX:..` — playlist active pour une MAC (header `X-App-Key`).
- `POST /api/devices/check` — statut licence (trial/premium/expire) + playlist (body JSON : `mac_address`, `android_id`, `brand`, `model`, `android_version`).
- `/reseller/` — dashboard revendeur (login + liaison MAC/playlist), CSRF + rate-limit actifs.

### Migration depuis les anciens secrets en dur

Si vous utilisiez `config.php` avec des valeurs en dur, basculez-les en variables d'environnement
avant de deployer cette version. Puis **faites pivoter** `APK_SECRET_KEY` et `RESELLER_PASS`
(ils ont pu etre exposes).
