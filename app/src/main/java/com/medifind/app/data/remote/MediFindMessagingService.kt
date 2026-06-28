package com.medifind.app.data.remote

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MediFindMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        // In a later phase, this token would be sent to the backend
        // and stored against the user, so the server can target this device.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCM", "Message received: ${message.notification?.title} - ${message.notification?.body}")
        // Phase 6 will build actual local notification display here.
        // For now, this confirms messages are being received at all.
    }
}