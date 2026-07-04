package com.medifind.app.data.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.medifind.app.MainActivity
import com.medifind.app.data.repository.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MediFindMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        val tokenManager = TokenManager(applicationContext)
        val jwt = tokenManager.getToken()
        if (jwt != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    RetrofitInstance.api.updateFcmToken(
                        token = "Bearer $jwt",
                        request = FcmTokenRequest(token)
                    )
                } catch (e: Exception) {
                    Log.e("FCM", "Failed to register rotated token", e)
                }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: "MediFind AI"
        val body = message.notification?.body ?: return
        val type = message.data["type"]

        Log.d("FCM", "Message received: $title - $body (type=$type)")
        showNotification(title, body, type)
    }

    private fun showNotification(title: String, body: String, type: String?) {
        val channelId = if (type == "reservation_status") "reservation_updates" else "general_updates"
        val channelName = if (type == "reservation_status") "Reservation Updates" else "General Updates"

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (type == "reservation_status") {
                putExtra("navigate_to", "my_reservations")
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}