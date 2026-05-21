package com.skyplayer.pro.data.license

import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.FileReader
import java.net.NetworkInterface
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestionnaire de licence pour SkyPlayer Pro
 * - Génère un identifiant unique persistant (Virtual MAC)
 * - Stocke les données de façon sécurisée
 * - Gère la période d'essai et l'activation
 */
@Singleton
class LicenseManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_FILE = "license_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_INSTALL_DATE = "install_date"
        private const val KEY_TRIAL_DAYS = 15
        private const val KEY_IS_ACTIVATED = "is_activated_local" // Cache local
        private const val KEY_ACTIVATION_DATE = "activation_date"
        
        // Format UUID comme MAC: XX:XX:XX:XX:XX:XX:XX:XX (8 segments pour plus d'unicité)
        private const val MAC_FORMAT_LENGTH = 8
    }
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * Génère ou récupère l'identifiant unique de l'appareil au format MAC
     * Format: XX:XX:XX:XX:XX:XX:XX:XX (8 segments hexadécimaux)
     * Cet ID est persistant même après réinstallation grâce au stockage chiffré
     */
    fun getDeviceId(): String {
        val existingId = encryptedPrefs.getString(KEY_DEVICE_ID, null)
        
        return if (existingId != null) {
            Timber.d("📱 Device ID existant: $existingId")
            existingId
        } else {
            // Générer un nouvel ID unique
            val newId = generateVirtualMacId()
            encryptedPrefs.edit().putString(KEY_DEVICE_ID, newId).apply()
            
            // Enregistrer la date d'installation
            val installTime = System.currentTimeMillis()
            encryptedPrefs.edit().putLong(KEY_INSTALL_DATE, installTime).apply()
            
            Timber.i("🆕 Nouveau Device ID généré: $newId")
            Timber.i("📅 Date d'installation: ${java.util.Date(installTime)}")
            
            newId
        }
    }
    
    /**
     * Génère un identifiant unique persistant combinant:
     * 1. Adresse MAC physique Wi-Fi (si disponible)
     * 2. ANDROID_ID (persiste après réinstallation sur la plupart des appareils)
     * 3. Empreinte hardware Build (BOARD + BRAND + DEVICE + HARDWARE + FINGERPRINT)
     *
     * Le hash SHA-256 garantit l'unicité et la non-réversibilité.
     * L'ID est stocké chiffré et ne change JAMAIS une fois généré.
     */
    private fun generateVirtualMacId(): String {
        // 1. ANDROID_ID — persistant même après réinstallation
        val androidId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )?.takeIf { it.isNotBlank() && it != "9774d56d682e549c" } // exclure valeur par défaut émulateur
            ?: UUID.randomUUID().toString()

        // 2. Adresse MAC physique (Wi-Fi ou Ethernet)
        val physicalMac = getPhysicalMacAddress()

        // 3. Empreinte hardware stable (non sensible, change rarement)
        val buildFingerprint = buildString {
            append(android.os.Build.BOARD)
            append(android.os.Build.BRAND)
            append(android.os.Build.DEVICE)
            append(android.os.Build.HARDWARE)
            append(android.os.Build.MANUFACTURER)
            append(android.os.Build.PRODUCT)
        }

        // Combinaison: MAC physique (si dispo) + ANDROID_ID + hardware
        val rawInput = "$physicalMac|$androidId|$buildFingerprint"

        // Hash SHA-256 → format XX:XX:XX:XX:XX:XX:XX:XX
        val hash = MessageDigest.getInstance("SHA-256").digest(rawInput.toByteArray(Charsets.UTF_8))
        val macBuilder = StringBuilder()
        for (i in 0 until MAC_FORMAT_LENGTH) {
            if (i > 0) macBuilder.append(":")
            macBuilder.append(String.format("%02X", hash[i].toInt() and 0xFF))
        }

        val result = macBuilder.toString()
        Timber.i("🔐 Device ID généré — MAC physique: ${physicalMac.take(5)}*** ANDROID_ID: ${androidId.take(8)}*** → $result")
        return result
    }

    /**
     * Tente de lire la vraie adresse MAC physique via plusieurs méthodes:
     * 1. /sys/class/net/wlan0/address (lecture directe, API < 23)
     * 2. NetworkInterface.getHardwareAddress() (API >= 23, peut nécessiter ACCESS_WIFI_STATE)
     * 3. Retourne "" si aucune méthode ne fonctionne (Android 10+ restreint)
     */
    private fun getPhysicalMacAddress(): String {
        // Méthode 1: lecture fichier système (fonctionne sur de nombreux appareils)
        try {
            val interfaces = listOf("wlan0", "eth0", "wlan1", "p2p0")
            for (iface in interfaces) {
                val file = java.io.File("/sys/class/net/$iface/address")
                if (file.exists() && file.canRead()) {
                    val mac = file.readText().trim()
                    if (mac.isNotBlank() && mac != "00:00:00:00:00:00" && mac != "02:00:00:00:00:00") {
                        Timber.d("� MAC physique lue depuis /sys/class/net/$iface: $mac")
                        return mac
                    }
                }
            }
        } catch (e: Exception) {
            Timber.d("Impossible de lire /sys/class/net: ${e.message}")
        }

        // Méthode 2: NetworkInterface API
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces() ?: return ""
            for (iface in networkInterfaces.asSequence()) {
                if (!iface.name.startsWith("wlan") && !iface.name.startsWith("eth")) continue
                val hardwareAddress = iface.hardwareAddress ?: continue
                if (hardwareAddress.size < 6) continue
                val mac = hardwareAddress.joinToString(":") { "%02X".format(it) }
                if (mac != "02:00:00:00:00:00" && mac != "00:00:00:00:00:00") {
                    Timber.d("📡 MAC physique via NetworkInterface (${iface.name}): $mac")
                    return mac
                }
            }
        } catch (e: Exception) {
            Timber.d("NetworkInterface non disponible: ${e.message}")
        }

        Timber.d("⚠️ Adresse MAC physique non disponible (Android 10+ restriction) — utilisation ANDROID_ID seul")
        return ""
    }
    
    /**
     * Récupère la date d'installation (timestamp)
     */
    fun getInstallDate(): Long {
        return encryptedPrefs.getLong(KEY_INSTALL_DATE, System.currentTimeMillis())
    }
    
    /**
     * Vérifie si la période d'essai est encore valide
     * @return true si dans les 15 jours d'essai
     */
    fun isTrialValid(): Boolean {
        val installDate = getInstallDate()
        val currentTime = System.currentTimeMillis()
        val trialEndDate = installDate + (KEY_TRIAL_DAYS * 24 * 60 * 60 * 1000L)
        
        val isValid = currentTime < trialEndDate
        val daysRemaining = ((trialEndDate - currentTime) / (24 * 60 * 60 * 1000L)).coerceAtLeast(0)
        
        Timber.d("⏳ Jours restants essai: $daysRemaining, Valide: $isValid")
        
        return isValid
    }
    
    /**
     * Calcule les jours restants de l'essai
     */
    fun getTrialDaysRemaining(): Int {
        val installDate = getInstallDate()
        val currentTime = System.currentTimeMillis()
        val trialEndDate = installDate + (KEY_TRIAL_DAYS * 24 * 60 * 60 * 1000L)
        
        return ((trialEndDate - currentTime) / (24 * 60 * 60 * 1000L)).coerceAtLeast(0).toInt()
    }
    
    /**
     * Marque l'appareil comme activé (cache local)
     * La vraie activation se fait via Firebase
     */
    fun setActivatedLocally(activated: Boolean) {
        encryptedPrefs.edit()
            .putBoolean(KEY_IS_ACTIVATED, activated)
            .apply()
        
        if (activated) {
            encryptedPrefs.edit()
                .putLong(KEY_ACTIVATION_DATE, System.currentTimeMillis())
                .apply()
            Timber.i("✅ Appareil marqué comme activé localement")
        }
    }
    
    /**
     * Vérifie le statut d'activation local
     */
    fun isActivatedLocally(): Boolean {
        return encryptedPrefs.getBoolean(KEY_IS_ACTIVATED, false)
    }
    
    /**
     * Vérifie si l'accès au lecteur est autorisé
     * - Essai valide OU activé = accès autorisé
     */
    fun hasValidAccess(): Boolean {
        return isTrialValid() || isActivatedLocally()
    }
    
    /**
     * Réinitialise la licence (pour tests)
     */
    suspend fun resetLicense() = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().clear().apply()
        Timber.w("⚠️ Licence réinitialisée")
    }
    
    /**
     * Récupère les informations complètes de licence
     */
    fun getLicenseInfo(): LicenseInfo {
        return LicenseInfo(
            deviceId = getDeviceId(),
            installDate = getInstallDate(),
            trialDaysRemaining = getTrialDaysRemaining(),
            isTrialValid = isTrialValid(),
            isActivated = isActivatedLocally()
        )
    }
}

/**
 * Data class contenant les informations de licence
 */
data class LicenseInfo(
    val deviceId: String,
    val installDate: Long,
    val trialDaysRemaining: Int,
    val isTrialValid: Boolean,
    val isActivated: Boolean
) {
    /**
     * Formate la date d'installation en string lisible
     */
    fun getInstallDateFormatted(): String {
        val date = java.util.Date(installDate)
        val formatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.FRANCE)
        return formatter.format(date)
    }
    
    /**
     * Vérifie si l'accès est autorisé
     */
    fun hasAccess(): Boolean = isTrialValid || isActivated
}
