# Deploiement Production - SkyPlayer Pro

Reference de mise en production de l'app Android et de son backend business
**Sky-player** (`https://github.com/ulrichokinda/Sky-player` — React + Node/Express + Firebase Firestore).

> L'ancien backend PHP (`backend/` : check_mac.php, devices/check.php, reseller/, activation-service/)
> a ete **supprime** de ce depot. L'app ne parle plus qu'au backend Sky-player (Firestore + API REST).

## 1) Backend Sky-player — configuration

Deployer le backend depuis son propre depot (`ulrichokinda/Sky-player`) sur votre hebergeur (Node >= 18).

### Variables d'environnement obligatoires (`.env`)

Se referer a `.env.example` du depot Sky-player :

| Variable | Role |
|----------|------|
| `ACTIVATION_API_KEY` | Cle de communication app Android ↔ API (**requis, a rotationner**) |
| `JOBOOST_MERCHANT_ID` | Identifiant marchand Joboost-Cash |
| `JOBOOST_API_KEY` | Cle API Joboost-Cash |
| `JOBOOST_SECRET_KEY` | Secret webhooks Joboost-Cash |
| `FIREBASE_SERVICE_ACCOUNT` | JSON du compte de service (si acces Admin requis) |
| `GEMINI_API_KEY` | Optionnel (fonctions IA du backend) |

### Regles Firestore

Les regles durcies sont dans `firestore.rules` a la racine du depot Sky-player.
Deployer avec :

```bash
firebase login
firebase use --add
firebase deploy --only firestore:rules
```

## 2) Application Android — configuration

L'URL et la cle du backend sont injectees depuis `local.properties` (jamais committe) :

```properties
BACKEND_BASE_URL=https://votre-backend-deploye.com
LICENSE_API_KEY=ma_cle_activation
```

- `LICENSE_API_KEY` = la valeur de `ACTIVATION_API_KEY` cote serveur.
- La cle est embarquee dans l'APK (extractible) : elle protege les endpoints mais ne remplace
  pas les regles Firestore. Toute decision de licence doit etre re-validee cote serveur.

### Endpoints consommes par l'app

| Endpoint | Role | Auth |
|----------|------|------|
| `POST /api/devices/check` | Statut licence (trial/premium/expire) + playlist | `X-Activation-API-Key` |
| `GET /api/mac/check/{mac}` | Statut d'une MAC (source de verite, fallback + anti-triche) | `X-Activation-API-Key` |
| `GET /api/v1/playlist/{mac}` | Playlist pour une MAC (fallback) | tolerant |

## 3) Rotation des secrets

La cle `ACTIVATION_API_KEY` a circule en clair (fallback en dur dans `server.ts` du depot public
Sky-player et dans l'historique de cette session). Considerer-la compromise et la **rotationner** :

1. Generer une nouvelle valeur : `openssl rand -hex 32`
2. La definir en variable d'environnement du serveur (`ACTIVATION_API_KEY`), **sans fallback en dur** dans le code.
3. Mettre la meme valeur dans `local.properties` → `LICENSE_API_KEY`.
4. Rebuild + redeploy l'app et le backend.

## 4) Checklist go-live

- [ ] Backend Sky-player deploye + variables d'environnement definies
- [ ] `ACTIVATION_API_KEY` rotationnee (plus de valeur en dur dans le code)
- [ ] Regles Firestore deployees (`firestore.rules`)
- [ ] `local.properties` configure (`BACKEND_BASE_URL`, `LICENSE_API_KEY`)
- [ ] Test d'activation reelle d'un device de recette
- [ ] Test de blocage apres expiration (`GET /api/mac/check` → `active: false`)
- [ ] Build release signe : `./gradlew :app:assembleRelease`
- [ ] Baseline profile genere sur appareil : `./gradlew :app:generateBaselineProfile`

## 5) Notes securite

- Ne jamais embarquer de compte de service Firebase Admin dans l'app Android.
- La cle `LICENSE_API_KEY` embarquee dans l'APK est extractible : les regles Firestore
  restent la derniere ligne de defense.
- Le backend refuse les appels Android sans `X-Activation-API-Key` valide (voir `validateActivationApiKey`).
- L'anti-triche horloge utilise l'en-tete HTTP `Date` du serveur (non falsifiable cote client).
