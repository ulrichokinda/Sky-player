import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinCompose)

    // Google Services Gradle plugin for Firebase
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
    alias(libs.plugins.baselineProfile)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
val hasReleaseKeystore = keystorePropertiesFile.exists()
if (hasReleaseKeystore) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

android {
    namespace = "com.skyplayer.pro"
    compileSdk = 37

    if (hasReleaseKeystore) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        applicationId = "com.skyplayer.pro"
        minSdk = 26  // Android 8.0+ pour compatibilité universelle
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0-Pro"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["ALLOW_CLEARTEXT_TRAFFIC"] = "false"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Support multi-architecture pour tous les appareils
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }

        // Buffering : démarrage rapide (1.2s) + maintien 15s/50s anti-coupure
        // Le buffer de maintien élevé protège les réseaux instables, le seuil de
        // démarrage bas garantit une première image quasi-instantanée (live & VOD).
        buildConfigField("int", "BUFFER_FOR_PLAYBACK_MS", "1200")
        buildConfigField("int", "BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS", "3000")
        buildConfigField("int", "MIN_BUFFER_MS", "15000")
        buildConfigField("int", "MAX_BUFFER_MS", "50000")
        buildConfigField("boolean", "ALLOW_CLEARTEXT_TRAFFIC", "false")

        // Injection des secrets depuis local.properties
        val licenseKey = (localProperties.getProperty("LICENSE_API_KEY") ?: project.findProperty("LICENSE_API_KEY")?.toString() ?: "").let {
            if (it.startsWith("\"") && it.endsWith("\"")) it else "\"$it\""
        }
        val backendUrl = (localProperties.getProperty("BACKEND_BASE_URL") ?: project.findProperty("BACKEND_BASE_URL")?.toString() ?: "").let {
            if (it.startsWith("\"") && it.endsWith("\"")) it else "\"$it\""
        }

        buildConfigField("String", "LICENSE_API_KEY", licenseKey)
        buildConfigField("String", "BACKEND_BASE_URL", backendUrl)
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            manifestPlaceholders["ALLOW_CLEARTEXT_TRAFFIC"] = "false"
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
            buildConfigField("boolean", "ALLOW_CLEARTEXT_TRAFFIC", "true")
            manifestPlaceholders["ALLOW_CLEARTEXT_TRAFFIC"] = "true"
            // Note: Pas de applicationIdSuffix pour compatibilité google-services.json
            // Si vous voulez un suffixe, ajoutez le package name correspondant dans Firebase Console
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Baseline Profiles : génération avec ./gradlew :app:generateBaselineProfile (appareil requis)
    baselineProfile {
        // Le profil généré est sauvegardé dans src/main/baselineProfiles/baseline-prof.txt
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        checkReleaseBuilds = true
        abortOnError = true
        baseline = file("lint-baseline.xml")
        checkAllWarnings = true
        warningsAsErrors = false
        showAll = true
        xmlReport = true
        htmlReport = true
        // Faux positif connu : grpc-core référence javax.naming (classe jamais chargée sur Android)
        disable += "InvalidPackage"
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.compose.foundation)

    // Material Components (pour les thèmes)
    implementation(libs.material)

    // Navigation Compose
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    // Media3 ExoPlayer - Moteur de lecture principal
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room - Base de données locale
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coil - Chargement et cache d'images
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.coil.video)

    // DataStore - Préférences
    implementation(libs.androidx.datastore.preferences)

    // Paging 3
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // Retrofit & Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Splash Screen
    implementation(libs.androidx.core.splashscreen)

    // Security Crypto - Chiffrement données sensibles
    implementation(libs.androidx.security.crypto)

    // Timber Logging
    implementation(libs.timber)

    // QR Code - Génération de codes QR pour configuration TV
    implementation(libs.zxing.core)

    // Firebase - Backend configuration et abonnements
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.crashlytics.ktx)

    // Debug only (UI tooling)
    debugImplementation(libs.androidx.ui.tooling)

    // Tests unitaires
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Baseline Profiles (tests instrumentés, exécution sur appareil)
    implementation(libs.androidx.profileinstaller)
    androidTestImplementation(libs.androidx.benchmark.macro.junit4)
    androidTestImplementation(libs.androidx.junit)
}

// Configuration Google Services Plugin
googleServices {
    disableVersionCheck = false
}

// Désactiver l'upload automatique des fichiers de mapping Crashlytics (évite les erreurs réseau)
afterEvaluate {
    tasks.withType<com.google.firebase.crashlytics.buildtools.gradle.tasks.UploadMappingFileTask>().configureEach {
        enabled = false
    }
}
