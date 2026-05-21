# Sky Player Pro

**Application IPTV premium optimisée pour l'Afrique**

## Caractéristiques principales

### Optimisation réseau
- **Buffering agressif** : 60-120 secondes de tampon mémoire
- **Reconnexion automatique** exponentielle (backoff)
- **Support réseaux lents** : Edge/3G/4G instable
- **Cache intelligent** des logos pour économiser la data

### Moteur de lecture
- **ExoPlayer Media3** avec configuration personnalisée
- **Support HLS/DASH** pour streaming adaptatif
- **Qualité auto-ajustable** selon le débit réseau
- **Lecture en arrière-plan** avec service dédié

### Interface utilisateur
- **Design Premium Dark Mode** Material 3
- **Navigation fluide** entre Live TV, VOD et Séries
- **Splash Screen animé** avec branding
- **Icônes catégorisées** par type de contenu

### Fonctionnalités
- **Parser M3U universel** haute performance
- **Support Xtream Codes** API
- **Système de Favoris** persistant
- **Historique de lecture** avec reprise
- **Code Parental** (PIN Lock) pour contrôle parental
- **Gestion multi-playlists**

## Architecture technique

```
app/src/main/java/com/skyplayer/pro/
├── data/
│   ├── local/          # Room Database (DAO, Entities)
│   ├── model/          # Data classes (Channel, Playlist, etc.)
│   ├── parser/         # M3UParser
│   ├── remote/         # XtreamCodes API
│   └── repository/     # Repositories (Channel, Playlist)
├── di/                 # Hilt Modules (App, Database, Network)
├── receiver/           # NetworkReceiver
├── service/            # PlayerService (background)
└── ui/
    ├── components/     # Composables réutilisables
    ├── navigation/     # Routes, NavHost
    ├── screens/        # Écrans (Splash, LiveTV, VOD, etc.)
    └── theme/          # Couleurs, Typographie, Theme
```

## Configuration du buffering

```kotlin
// LoadControl personnalisé pour réseaux instables
minBufferMs = 60000      // 60 secondes
maxBufferMs = 120000     // 120 secondes
bufferForPlaybackMs = 2500
bufferForPlaybackAfterRebufferMs = 5000
```

## Dépendances principales

| Catégorie | Bibliothèque |
|-----------|-------------|
| UI | Jetpack Compose, Material 3 |
| Lecteur | Media3 ExoPlayer |
| Injection | Hilt |
| Base de données | Room |
| Images | Coil |
| Réseau | Retrofit, OkHttp |
| Logging | Timber |

## Compilation

```bash
# Build debug
./gradlew :app:assembleDebug

# Build release
./gradlew :app:assembleRelease
```

## Mise en production

- Guide complet: `DEPLOIEMENT_SECURISE.md`
- Deploiement regles Firebase: `scripts/deploy-rules.ps1`
- Backend activation site: `backend/activation-service/`

## Structure du projet créée

- ✅ Configuration Gradle (Kotlin DSL)
- ✅ Fichiers de version centralisés (libs.versions.toml)
- ✅ Splash Screen avec animation
- ✅ Thème Premium Dark Mode
- ✅ Navigation Compose avec 4 sections
- ✅ ExoPlayer avec buffering agressif
- ✅ Reconnexion automatique exponentielle
- ✅ Parser M3U universel
- ✅ Support Xtream Codes
- ✅ Base de données Room (Channels, Playlists, WatchHistory)
- ✅ Système de favoris
- ✅ Code Parental (structure)
- ✅ Cache images avec Coil
- ✅ Service lecteur en arrière-plan

## Prochaines étapes suggérées

1. Implémenter l'écran PIN complet avec clavier numérique
2. Ajouter le guide électronique des programmes (EPG)
3. Intégrer la recherche avec filtre temps réel
4. Ajouter le support Chromecast
5. Implémenter les sous-titres multiples

---
**Version** : 1.0.0-Pro  
**Optimisé pour** : Afrique (réseaux instables)
