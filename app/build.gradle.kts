plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinCompose)
    
    // Google Services Gradle plugin for Firebase
    alias(libs.plugins.googleServices)
}

android {
    namespace = "com.skyplayer.pro"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.skyplayer.pro"
        minSdk = 24  // Android 7.0+ pour compatibilité universelle
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0-Pro"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Support multi-architecture pour tous les appareils
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }

        // Configuration BUFFER AGRESSIF pour réseaux instables (Afrique)
        // Objectif: 2 minutes d'avance maximum pour résister aux coupures
        buildConfigField("int", "MIN_BUFFER_MS", "90000")    // 90s avant démarrage
        buildConfigField("int", "MAX_BUFFER_MS", "120000")   // 120s MAXIMUM (2 min)
        buildConfigField("int", "BUFFER_FOR_PLAYBACK_MS", "5000")
        buildConfigField("int", "BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS", "10000")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    
    // Material Components (pour les thèmes)
    implementation("com.google.android.material:material:1.11.0")

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
    // Versions compatibles Kotlin 1.9.22
    // Realtime Database pour config utilisateur et abonnements
    implementation("com.google.firebase:firebase-database-ktx:20.3.0")
    // Analytics pour statistiques d'usage - version 21.x stable
    implementation("com.google.firebase:firebase-analytics-ktx:21.5.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// Configuration Google Services Plugin
googleServices {
    disableVersionCheck = false
}
