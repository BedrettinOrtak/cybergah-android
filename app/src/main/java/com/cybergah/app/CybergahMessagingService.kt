package com.cybergah.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CybergahMessagingService : FirebaseMessagingService() {

    companion object {
        const val TAG = "CybergahFCM"
        const val CHANNEL_ID_CONTENT = "cybergah_content"
        const val CHANNEL_ID_FORUM = "cybergah_forum"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Yeni FCM token: $token")
        // Token'ı sunucuya gönder
        sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Bildirim alındı: ${message.data}")

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Cybergah"

        val body = message.notification?.body
            ?: message.data["body"]
            ?: "Yeni içerik mevcut!"

        val url = message.data["url"] ?: "https://cybergah.com"
        val type = message.data["type"] ?: "content"

        showNotification(title, body, url, type)
    }

    private fun showNotification(title: String, body: String, url: String, type: String) {
        val channelId = when (type) {
            "forum" -> CHANNEL_ID_FORUM
            else -> CHANNEL_ID_CONTENT
        }

        // Tıklanınca açılacak intent
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data = Uri.parse(url)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Bildirim sesi
        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setSound(defaultSound)
            .setVibrate(longArrayOf(0, 250, 100, 250))
            .setColor(getColor(R.color.brand_red))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Kanalları oluştur (Android 8+)
        createNotificationChannels(notificationManager)

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannels(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Yeni İçerik kanalı
            val contentChannel = NotificationChannel(
                CHANNEL_ID_CONTENT,
                "Yeni İçerikler",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Yeni yazı, rehber ve inceleme bildirimleri"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 100, 250)
            }

            // Forum kanalı
            val forumChannel = NotificationChannel(
                CHANNEL_ID_FORUM,
                "Forum Bildirimleri",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Forum konularındaki yeni cevaplar"
                enableVibration(true)
            }

            manager.createNotificationChannels(listOf(contentChannel, forumChannel))
        }
    }

    private fun sendTokenToServer(token: String) {
        // TODO: Token'ı Django backend'e gönder
        // Örnek: POST https://cybergah.com/api/fcm/register/
        // Body: { "token": token, "platform": "android" }
        Log.d(TAG, "Token sunucuya gönderilecek: $token")
    }
}
