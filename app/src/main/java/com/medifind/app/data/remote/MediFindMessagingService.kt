package com.medifind.app.data.remote

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
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
        Log.d("FCM", "Message received: ${message.notification?.title} - ${message.notification?.body}")
    }
}