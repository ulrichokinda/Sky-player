# 🎬 Guide - Optimisation de la Qualité Vidéo

## Nouvelles Fonctionnalités Implémentées

### 1. **Sélecteur de Qualité Vidéo**
L'utilisateur peut maintenant choisir manuellement la qualité de lecture pour optimiser sa connexion :

| Qualité | Résolution | Débit Requis | Usage |
|---------|-----------|--------------|-------|
| **Auto** | Adaptatif | Variable | Recommandé - s'adapte à votre connexion |
| **Basse** | 480p | ~0.8 Mbps | Connexions très lentes (3G) |
| **Moyenne** | 720p | ~2.5 Mbps | Connexions normales (ADSL) |
| **Haute** | 1080p | ~5 Mbps | Fibre / Bon WiFi |
| **UHD** | 4K | ~15 Mbps | Connexions ultra-rapides |

### 2. **Modes Prédéfinis**
Trois boutons rapides dans les paramètres :

- 🟢 **Économie** : Qualité moyenne + tampon 60s (pour données limitées)
- 🟠 **Équilibré** : Qualité auto + tampon 30s (recommandé)
- 🔵 **Performance** : Qualité max + faible latence (connexion rapide)

### 3. **Options Avancées**

#### Ajustement Automatique
- Active la détection de bande passante en temps réel
- Ajuste automatiquement la qualité si la connexion fluctue
- Évite les saccades sur connexions instables

#### Durée du Tampon (Buffer)
- **10-30s** : Faible latence (direct/live)
- **30-60s** : Équilibré (recommandé)
- **60-120s** : Réseau lent (évite interruptions)

#### Mode Faible Latence
- Réduit le délai entre le serveur et l'écran
- Utile pour les directs sportifs
- Augmente légèrement les risques de buffering

### 4. **Indicateur de Connexion**
Un badge affiche la qualité recommandée selon votre débit :
- 🟠 < 1 Mbps : Connexion lente - Qualité basse recommandée
- 🟡 1-3 Mbps : Connexion moyenne - Qualité 720p
- 🟢 3-6 Mbps : Bonne connexion - Qualité 1080p
- 🔵 > 6 Mbps : Excellente connexion - 4K possible

## 📱 Comment Utiliser

### Accéder aux Paramètres
1. Ouvrir l'app **SkyPlayer Pro**
2. Aller dans **Paramètres** (icône ⚙️)
3. Section **"Qualité Vidéo & Streaming"**

### Changer la Qualité
1. Sélectionner un **mode rapide** (Économie/Équilibré/Performance)
2. OU choisir manuellement dans la liste détaillée
3. OU activer **"Ajustement automatique"** pour laisser l'app décider

### Optimiser une Connexion Lente
Si vous avez des saccades :
1. Choisissez **"Économie"** ou **"Basse (480p)"**
2. Augmentez le **tampon à 60-90 secondes**
3. Désactivez le **mode faible latence**
4. Activez l'**ajustement automatique**

### Maximiser la Qualité
Si vous avez une fibre rapide :
1. Choisissez **"Performance"** ou **"UHD (4K)"**
2. Mettez le **tampon à 20-30s** pour moins d'attente
3. Activez le **mode faible latence** si besoin

## ⚙️ Fichiers Modifiés/Créés

### Nouveaux Fichiers
- `VideoQuality.kt` - Modèle des qualités disponibles
- `StreamingPreferencesViewModel.kt` - Gestion des préférences
- `QualitySelector.kt` - UI du sélecteur de qualité

### Fichiers Modifiés
- `SettingsScreen.kt` - Ajout de la section Qualité Vidéo
- `EncryptedPrefs.kt` - Sauvegarde des préférences

## 🔧 Paramètres Recommandés par Type de Connexion

### Connexion Mobile (3G/4G limité)
```
Qualité: Basse (480p)
Tampon: 60s
Auto-ajustement: ON
Faible latence: OFF
```

### ADSL / Connexion Moyenne
```
Qualité: Moyenne (720p)
Tampon: 30-45s
Auto-ajustement: ON
Faible latence: OFF
```

### Fibre / Bon WiFi
```
Qualité: Haute (1080p)
Tampon: 20-30s
Auto-ajustement: ON
Faible latence: Optionnel
```

### Connexion Ultra-Rapide
```
Qualité: UHD (4K)
Tampon: 15-20s
Auto-ajustement: ON
Faible latence: ON
```

## 💡 Conseils

1. **Commencez par "Équilibré"** - C'est le réglage optimal pour la plupart des utilisateurs
2. **Si ça saccade** - Baissez la qualité d'un cran et augmentez le tampon
3. **Pour le sport** - Activez le mode faible latence + tampon 20s
4. **Pour les films** - Qualité haute + tampon 30-60s pour éviter interruptions
5. **En déplacement** - Mode Économie pour préserver vos données

## 🚀 Prochaine Étape

Compilez et installez l'APK :
```bash
./gradlew :app:assembleDebug
```

Les réglages sont sauvegardés automatiquement et appliqués instantanément au prochain démarrage d'un stream.
