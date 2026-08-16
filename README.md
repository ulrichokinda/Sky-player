# Sky Player Pro

**Application IPTV premium optimisée pour l'Afrique** (réseaux instables, Edge/3G/4G).

## Fonctionnalités

- **Lecture** : ExoPlayer Media3 (HLS/DASH), reprise après coupure, reconnexion exponentielle, qualité auto-ajustable, sélecteur de qualité manuel, lecture en arrière-plan.
- **Contenu** : Live TV, VOD (films) et Séries, catégorisation automatique (sport, news, pays, enfants…), EPG, moteur de recommandations.
- **Playlists** : parser M3U universel (gzip, attributs étendus), API Xtream Codes, multi-playlists, favoris, historique avec reprise.
- **Licence** : essai de 14 jours, activation via le backend Sky-player (Firestore + API `X-Activation-API-Key`), vérification temps réel + anti-triche (heure serveur), code parental (PIN).
- **TV** : configuration par QR code pour Android TV, partage local réseau.
- **Design** : Material 3 Dark Mode, splash animé, navigation par sections (Live, VOD, Séries, Favoris).

## Architecture

```
app/src/main/java/com/skyplayer/pro/
├── data/
│   ├── local/          # Room (DAO, entités)
│   ├── model/          # Data classes (Channel, Playlist, ContentType…)
│   ├── parser/         # M3UParser, EpgParser
│   ├── remote/         # XtreamCodes, DeviceCheck, LicenseApi
│   ├── license/        # LicenseManager, TrialPeriod, LicenseSecurityManager
│   ├── organizer/      # ContentClassifier (type + catégorie)
│   └── repository/     # Repositories (Channel, Playlist, Epg, Favoris…)
├── di/                 # Hilt (App, Database, Network)
├── service/            # PlayerService (arrière-plan)
└── ui/                 # Compose : screens, components, navigation, theme
```

**Stack** : Kotlin, Jetpack Compose + Material 3, Media3 ExoPlayer, Hilt, Room, Coil, Retrofit/OkHttp, Firebase (Remote Config, Firestore, Analytics, Crashlytics), DataStore.

**Backend** : le backend business est le dépôt externe `github.com/ulrichokinda/Sky-player` (React + Node/Express + Firebase Firestore). Dans ce dépôt : `firebase-functions/` (Cloud Functions, webhooks paiement) et `public/` (page d'accueil).

## Configuration du buffering

Valeurs réelles dans `app/build.gradle.kts` (`BuildConfig`) :

```kotlin
minBufferMs = 15000                     // 15 secondes cible
maxBufferMs = 50000                     // 50 secondes max
bufferForPlaybackMs = 2500              // démarrage après ~2,5 s de buffer
bufferForPlaybackAfterRebufferMs = 5000
```

## Compilation

```bash
# Build debug
./gradlew :app:assembleDebug

# Build release (signé via keystore.properties)
./gradlew :app:assembleRelease

# Tests unitaires
./gradlew :app:testDebugUnitTest

# Lint
./gradlew :app:lintDebug
```

### Baseline profiles (démarrage plus rapide)

Le setup est en place (`androidx.baselineprofile` + `profileinstaller` + `BaselineProfileGenerator`).
Générer le profil sur un appareil/émulateur connecté (Android 7+) :

```bash
./gradlew :app:generateBaselineProfile
```

Le résultat est écrit dans `app/src/main/baselineProfiles/baseline-prof.txt` et embarqué dans l'APK de release.

## Configuration du backend (app → serveur)

L'URL et la clé du backend sont injectées depuis `local.properties` (non versionné) :

```properties
BACKEND_BASE_URL=https://votre-backend.com
LICENSE_API_KEY=ma_cle_secrete
```

### Backend Sky-player (plateforme web + API)

L'app consomme le backend `github.com/ulrichokinda/Sky-player` (React + Node/Express + Firebase Firestore) :

| Endpoint appelé | Rôle | Auth |
|---|---|---|
| `POST /api/devices/check` | Statut licence (trial/premium/expiré) + playlist | `X-Activation-API-Key` |
| `GET /api/v1/playlist/{mac}` | Playlist pour une MAC (fallback) | tolérant |

- `LICENSE_API_KEY` = la valeur de `ACTIVATION_API_KEY` définie côté backend (variable d'environnement du serveur).
- La clé est embarquée dans l'APK (extractible) : elle protège les endpoints, mais ne remplace pas les règles Firestore.

L'ancien backend PHP (`backend/`) a été **supprimé** : l'app ne parle plus qu'au backend Sky-player (Firestore + API).

## Roadmap

- Paiement intégré à l'app (Mobile Money / cartes) au lieu du flux manuel ID appareil → site.
- EPG complet côté UI (le parseur existe).
- Tests instrumentés + benchmark de démarrage.
- Vérification d'essai entièrement côté serveur (anti-triche horloge).

---
**Version** : 1.0.0-Pro — **Optimisé pour** : Afrique (réseaux instables)
