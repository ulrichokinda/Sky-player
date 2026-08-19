package com.skyplayer.pro.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.skyplayer.pro.MainActivity
import com.skyplayer.pro.R
import com.skyplayer.pro.data.remote.SkyPlayerBackendApi
import com.skyplayer.pro.di.NetworkModule
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * A1: Service FCM pour recevoir les push notifications.
 *
 * - Enregistre le token FCM auprès du backend
 * - Crée le canal de notification "skyplayer_notifications"
 * - Affiche les notifications (trial expiré, activation, rappels)
 */
@AndroidEntryPoint
class SkyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "skyplayer_notifications"
        const val CHANNEL_NAME = "SkyPlayer Notifications"
        const val CHANNEL_DESC = "Alertes essai, activation et mises à jour"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    /**
     * Nouveau token FCM généré → l'envoyer au backend.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.i("🔑 FCM token refreshed: ${token.take(20)}...")
        registerTokenWithBackend(token)
    }

    /**
     * Notification reçue → afficher selon le type.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Timber.i("📩 FCM message received: ${message.data}")

        val data = message.data
        val type = data["type"] ?: "generic"
        val title = message.notification?.title ?: data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: data["body"] ?: ""

        showNotification(title, body, type)
    }

    private fun registerTokenWithBackend(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val androidId = android.provider.Settings.Secure.getString(
                    contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                ) ?: return@launch

                // Appel Callable Firebase via le backend
                val functions = com.google.firebase.functions.FirebaseFunctions.getInstance()
                val data = hashMapOf<String, Any>(
                    "android_id" to androidId,
                    "fcm_token" to token
                )
                functions.getHttpsCallable("registerFcmToken")
                    .call(data)
                    .addOnSuccessListener { result ->
                        Timber.i("✅ FCM token registered with backend")
                    }
                    .addOnFailureListener { e ->
                        Timber.e("❌ Failed to register FCM token: ${e.message}")
                    }
            } catch (e: Exception) {
                Timber.e(e, "❌ FCM token registration error")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, body: String, type: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", type)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_banner)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
