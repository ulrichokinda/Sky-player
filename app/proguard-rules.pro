# Sky Player Pro - Règles ProGuard/R8 de Production
# Optimisé pour la sécurité et la performance

# ==================== OPTIMISATIONS GÉNÉRALES ====================
-allowaccessmodification
-mergeinterfacesaggressively
-overloadaggressively
# -repackageclasses 'com.skyplayer.pro.internal'

# Garder les attributs essentiels pour le debugging (si nécessaire)
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,Exceptions,InnerClasses,EnclosingMethod

# ==================== SUPPRESSION DES LOGS ====================
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

-assumenosideeffects class timber.log.Timber {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# ==================== BIBLIOTHÈQUES CORE ====================

# Media3 ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
# Garder les classes de décodage et de rendu qui pourraient être instanciées par réflexion
-keep class androidx.media3.exoplayer.mediacodec.** { *; }
-keep class androidx.media3.exoplayer.video.** { *; }
-keep class androidx.media3.exoplayer.audio.** { *; }
-keep class androidx.media3.extractor.** { *; }
# Crucial pour IPTV : garder les décodeurs natifs et extensions
-keep class com.google.android.exoplayer2.** { *; }
-keep class com.google.android.exoplayer2.ext.ffmpeg.** { *; }
-keep class com.google.android.exoplayer2.ext.av1.** { *; }
-keep class com.google.android.exoplayer2.ext.vp9.** { *; }
-keep class com.google.android.exoplayer2.ext.opus.** { *; }
-keep class com.google.android.exoplayer2.ext.flac.** { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep class * extends androidx.room.TypeConverter
-dontwarn androidx.room.paging.**

# Hilt & Dagger
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keepclassmembers class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep @dagger.hilt.InstallIn class *
-dontwarn com.google.errorprone.annotations.**

# Retrofit 2
-keepattributes Signature, Exceptions
-keep class retrofit2.** { *; }
-keepclassmembers class retrofit2.** { *; }
-keep class * extends retrofit2.Converter { *; }
-dontwarn retrofit2.Platform$Java8

# OkHttp 3
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
# Empêcher l'obfuscation des méthodes de redirection et de protocole (crucial pour HTTP -> HTTP/2 ou redirections de flux)
-keepclassmembers class okhttp3.internal.publicsuffix.PublicSuffixDatabase {
    public java.lang.String getEffectiveTldPlusOne(java.lang.String);
}
-keep class okhttp3.Protocol { *; }
-keep class okhttp3.CipherSuite { *; }
-keep class okhttp3.TlsVersion { *; }

# Gson / JSON
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers class com.skyplayer.pro.data.model.** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Coil (Image Loading)
-keep class coil.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Android Crypto (Security)
-keep class androidx.security.crypto.** { *; }

# LibVLC
-keep class org.videolan.libvlc.** { *; }
-dontwarn org.videolan.libvlc.**

# Annotation @Keep pour les modèles de données
-keep @androidx.annotation.Keep class com.skyplayer.pro.data.model.** { *; }
-keepclassmembers class com.skyplayer.pro.data.model.** { *; }

# S'assurer que les modèles Retrofit ne sont pas obfusqués
-keepclassmembers class com.skyplayer.pro.data.model.** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ==================== SÉCURITÉ MÉTIER (PROTECTION) ====================

# Obfusquer mais garder les méthodes essentielles des Managers critiques
-keepnames class com.skyplayer.pro.data.license.**
-keepnames class com.skyplayer.pro.data.security.ParentalControlManager
-keepnames class com.skyplayer.pro.data.encrypted.EncryptedPrefs

# Empêcher le retrait du code de vérification de licence
-keepclassmembers class com.skyplayer.pro.data.license.LicenseManager {
    public boolean hasValidAccess();
    public boolean isTrialValid();
}

# Empêcher l'accès direct aux clés de déchiffrement
-keepclassmembers class com.skyplayer.pro.data.encrypted.EncryptedPrefs {
    private static final java.lang.String *;
}

# Protéger les mécanismes de failover et ABR
-keepnames class com.skyplayer.pro.ui.screens.player.AdaptiveBitrateManager
-keepnames class com.skyplayer.pro.data.prefetch.StreamPrefetchManager

# Garder les classes de données pour éviter les crashes au parsing JSON
-keep @androidx.annotation.Keep class *
-keep @androidx.annotation.Keep class ** { *; }
-keep @androidx.annotation.Keep interface *
-keep @androidx.annotation.Keep enum *
-keep class com.skyplayer.pro.data.model.** { *; }
-keepclassmembers class com.skyplayer.pro.data.model.** { *; }
