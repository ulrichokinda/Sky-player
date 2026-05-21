package com.skyplayer.pro.data.localshare

import android.content.Context
import android.util.Base64
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.skyplayer.pro.data.model.Playlist
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Gestionnaire de partage local réseau (Wi-Fi Direct / NSD)
 *
 * Permet de partager une playlist et les credentials entre deux appareils
 * sur le même réseau Wi-Fi local sans consommer de data internet.
 *
 * Fonctionnement :
 * 1. L'expéditeur crée un service NSD découvrable
 * 2. Le récepteur scanne et découvre les services disponibles
 * 3. Connexion socket TCP directe entre les deux appareils
 * 4. Transmission chiffrée des données (playlist + credentials)
 */
@Singleton
class NetworkShareManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val SERVICE_TYPE = "_skyplayer._tcp"
        private const val SERVICE_NAME = "SkyPlayerShare"
        private const val SHARE_PORT = 54789
        private const val TIMEOUT_MS = 30000L // 30 secondes
        private const val DISCOVERY_TIMEOUT_MS = 10000L // 10 secondes de scan
        private const val PBKDF2_ITERATIONS = 10000
        private const val AES_KEY_SIZE_BITS = 256
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val GCM_IV_LENGTH_BYTES = 12
    }

    private val nsdManager: NsdManager? = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val wifiManager: WifiManager? = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // État du partage
    private val _shareState = MutableStateFlow<ShareState>(ShareState.Idle)
    val shareState: StateFlow<ShareState> = _shareState

    // Services découverts
    private val _discoveredServices = MutableStateFlow<List<NsdServiceInfo>>(emptyList())
    val discoveredServices: StateFlow<List<NsdServiceInfo>> = _discoveredServices

    private var serverSocket: ServerSocket? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var resolveListener: NsdManager.ResolveListener? = null

    /**
     * Données à partager
     */
    data class ShareData(
        val playlistUrl: String,
        val playlistName: String,
        val credentials: Map<String, String> = emptyMap(),
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Résultat du partage reçu
     */
    data class ReceivedShare(
        val shareData: ShareData,
        val senderDeviceName: String,
        val receivedAt: Long = System.currentTimeMillis()
    )

    // ===== EXPÉDITEUR =====

    /**
     * Démarre le service de partage (mode expéditeur)
     * L'appareil devient découvrable sur le réseau local
     */
    fun startSharing(shareData: ShareData) {
        coroutineScope.launch {
            try {
                _shareState.value = ShareState.Sharing(shareData)

                // Démarrer le serveur socket
                startServerSocket(shareData)

                // Enregistrer le service NSD
                registerService()

                Timber.i("📡 Service de partage démarré sur le port $SHARE_PORT")

                // Timeout auto-stop après 5 minutes
                delay(300000)
                stopSharing()

            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur démarrage partage")
                _shareState.value = ShareState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    /**
     * Arrête le service de partage
     */
    fun stopSharing() {
        coroutineScope.launch {
            try {
                // Annuler l'enregistrement NSD
                registrationListener?.let { listener ->
                    try {
                        nsdManager?.unregisterService(listener)
                    } catch (e: Exception) {
                        Timber.w("Erreur unregister NSD: ${e.message}")
                    }
                }

                // Fermer le socket serveur
                serverSocket?.close()
                serverSocket = null

                _shareState.value = ShareState.Idle
                Timber.i("🛑 Service de partage arrêté")

            } catch (e: Exception) {
                Timber.e(e, "Erreur arrêt partage")
            }
        }
    }

    private fun startServerSocket(shareData: ShareData) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(SHARE_PORT)
                serverSocket?.soTimeout = TIMEOUT_MS.toInt()

                Timber.d("🔌 ServerSocket démarré, attente de connexion...")

                // Attendre une connexion
                val clientSocket = serverSocket?.accept()

                clientSocket?.let { socket ->
                    handleClientConnection(socket, shareData)
                }

            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur ServerSocket")
                _shareState.value = ShareState.Error("Erreur connexion: ${e.message}")
            }
        }
    }

    private suspend fun handleClientConnection(socket: Socket, shareData: ShareData) {
        withContext(Dispatchers.IO) {
            try {
                Timber.i("🔗 Client connecté: ${socket.inetAddress.hostAddress}")
                _shareState.value = ShareState.Transferring(shareData, 0)

                // Générer une clé de chiffrement temporaire
                val encryptionKey = generateEncryptionKey()

                // Envoyer la clé au client (via un canal sécurisé ou affichage)
                val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                // Protocole simple : KEY|DATA
                writer.println("KEY:$encryptionKey")

                // Attendre acquittement
                val ack = reader.readLine()
                if (ack == "READY") {
                    // Chiffrer et envoyer les données
                    val jsonData = shareDataToJson(shareData)
                    val encryptedData = encryptData(jsonData, encryptionKey)

                    writer.println("DATA:$encryptedData")

                    // Attendre confirmation réception
                    val confirm = reader.readLine()
                    if (confirm == "RECEIVED") {
                        _shareState.value = ShareState.Completed
                        Timber.i("✅ Données partagées avec succès")
                    }
                }

            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur transfert")
                _shareState.value = ShareState.Error("Erreur transfert: ${e.message}")
            } finally {
                socket.close()
            }
        }
    }

    private fun registerService() {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "$SERVICE_NAME-${getDeviceName()}"
            serviceType = SERVICE_TYPE
            port = SHARE_PORT
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Timber.i("✅ Service NSD enregistré: ${info.serviceName}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Timber.e("❌ Échec enregistrement NSD: $errorCode")
                _shareState.value = ShareState.Error("Échec enregistrement service")
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) {
                Timber.d("Service NSD désenregistré")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Timber.w("Échec désenregistrement NSD: $errorCode")
            }
        }

        nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    // ===== RÉCEPTEUR =====

    /**
     * Démarre la découverte des services de partage disponibles
     */
    fun startDiscovery() {
        _discoveredServices.value = emptyList()
        _shareState.value = ShareState.Discovering

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Timber.d("🔍 Découverte démarrée: $regType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Timber.d("📡 Service trouvé: ${serviceInfo.serviceName}")

                // Résoudre le service pour obtenir l'IP et le port
                resolveService(serviceInfo)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Timber.d("📡 Service perdu: ${serviceInfo.serviceName}")
                _discoveredServices.value = _discoveredServices.value.filter {
                    it.serviceName != serviceInfo.serviceName
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Timber.d("🛑 Découverte arrêtée")
                _shareState.value = ShareState.Idle
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.e("❌ Échec démarrage découverte: $errorCode")
                _shareState.value = ShareState.Error("Échec découverte")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.w("Échec arrêt découverte: $errorCode")
            }
        }

        nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)

        // Timeout auto-stop
        coroutineScope.launch {
            delay(DISCOVERY_TIMEOUT_MS)
            stopDiscovery()
        }
    }

    /**
     * Arrête la découverte
     */
    fun stopDiscovery() {
        try {
            discoveryListener?.let { listener ->
                nsdManager?.stopServiceDiscovery(listener)
            }
            discoveryListener = null
            _shareState.value = ShareState.Idle
        } catch (e: Exception) {
            Timber.w("Erreur arrêt découverte: ${e.message}")
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        resolveListener = object : NsdManager.ResolveListener {
            override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                Timber.i("✅ Service résolu: ${resolvedInfo.host}:${resolvedInfo.port}")
                _discoveredServices.value = _discoveredServices.value + resolvedInfo
            }

            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Timber.e("❌ Échec résolution: $errorCode")
            }
        }

        nsdManager?.resolveService(serviceInfo, resolveListener)
    }

    /**
     * Connecte à un service et récupère les données partagées
     */
    fun connectAndReceive(serviceInfo: NsdServiceInfo, onReceived: (ReceivedShare) -> Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                _shareState.value = ShareState.Receiving

                val socket = Socket(serviceInfo.host, serviceInfo.port)
                socket.soTimeout = TIMEOUT_MS.toInt()

                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)

                // Lire la clé de chiffrement
                val keyLine = reader.readLine() ?: throw IllegalStateException("Clé de chiffrement manquante")
                val encryptionKey = keyLine.removePrefix("KEY:")

                // Confirmer prêt
                writer.println("READY")

                // Lire les données chiffrées
                val dataLine = reader.readLine() ?: throw IllegalStateException("Données chiffrées manquantes")
                val encryptedData = dataLine.removePrefix("DATA:")

                // Déchiffrer
                val jsonData = decryptData(encryptedData, encryptionKey)
                val shareData = jsonToShareData(jsonData)

                // Confirmer réception
                writer.println("RECEIVED")

                val receivedShare = ReceivedShare(
                    shareData = shareData,
                    senderDeviceName = serviceInfo.serviceName.removePrefix("$SERVICE_NAME-")
                )

                onReceived(receivedShare)
                _shareState.value = ShareState.Completed

                socket.close()

                Timber.i("✅ Données reçues avec succès")

            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur réception")
                _shareState.value = ShareState.Error("Erreur réception: ${e.message}")
            }
        }
    }

    // ===== UTILITAIRES =====

    private fun generateEncryptionKey(): String {
        // Code temporaire à usage unique.
        return Random.nextInt(100000, 999999).toString()
    }

    private fun encryptData(data: String, key: String): String {
        // Chiffrement AES-GCM avec IV aléatoire.
        return try {
            val secretKey = deriveAesKey(key)
            val iv = ByteArray(GCM_IV_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val dataBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            "v1:$ivBase64:$dataBase64"
        } catch (e: Exception) {
            Timber.e(e, "Erreur chiffrement")
            data
        }
    }

    private fun decryptData(encrypted: String, key: String): String {
        // Déchiffrement AES-GCM
        return try {
            val parts = encrypted.split(":")
            if (parts.size != 3 || parts[0] != "v1") {
                throw IllegalArgumentException("Format de payload non supporté")
            }

            val iv = Base64.decode(parts[1], Base64.NO_WRAP)
            val payload = Base64.decode(parts[2], Base64.NO_WRAP)
            val secretKey = deriveAesKey(key)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val clear = cipher.doFinal(payload)
            String(clear, Charsets.UTF_8)
        } catch (e: Exception) {
            Timber.e(e, "Erreur déchiffrement")
            encrypted
        }
    }

    private fun deriveAesKey(pin: String): javax.crypto.SecretKey {
        val keySpec = PBEKeySpec(
            pin.toCharArray(),
            ("skyplayer-share-salt:$pin").toByteArray(Charsets.UTF_8),
            PBKDF2_ITERATIONS,
            AES_KEY_SIZE_BITS
        )
        val keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(keySpec).encoded
        return javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
    }

    private fun shareDataToJson(data: ShareData): String {
        return """
            {
                "playlistUrl": "${data.playlistUrl}",
                "playlistName": "${data.playlistName}",
                "credentials": ${data.credentials.entries.joinToString(",", "{", "}") { "\"${it.key}\":\"${it.value}\"" }},
                "timestamp": ${data.timestamp}
            }
        """.trimIndent()
    }

    private fun jsonToShareData(json: String): ShareData {
        // Parse simplifié (utiliser Gson/Moshi en production)
        val urlRegex = """"playlistUrl":\s*"([^"]+)"""".toRegex()
        val nameRegex = """"playlistName":\s*"([^"]+)"""".toRegex()

        val url = urlRegex.find(json)?.groupValues?.get(1) ?: ""
        val name = nameRegex.find(json)?.groupValues?.get(1) ?: ""

        return ShareData(
            playlistUrl = url,
            playlistName = name
        )
    }

    private fun getDeviceName(): String {
        return try {
            Build.MODEL?.replace(" ", "_") ?: "Unknown"
        } catch (e: Exception) {
            "Device_${Random.nextInt(1000, 9999)}"
        }
    }

    fun release() {
        stopSharing()
        stopDiscovery()
        coroutineScope.cancel()
    }
}

/**
 * États du partage réseau
 */
sealed class ShareState {
    object Idle : ShareState()
    data class Sharing(val data: NetworkShareManager.ShareData) : ShareState()
    object Discovering : ShareState()
    data class Transferring(val data: NetworkShareManager.ShareData, val progress: Int) : ShareState()
    object Receiving : ShareState()
    object Completed : ShareState()
    data class Error(val message: String) : ShareState()
}
