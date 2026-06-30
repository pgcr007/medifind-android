package com.medifind.app.data.repository

import com.medifind.app.data.remote.ChatRequest
import com.medifind.app.data.remote.ChatResponse
import com.medifind.app.data.remote.RetrofitInstance

class ChatRepository {

    suspend fun sendMessage(token: String, message: String, lat: Double?, lng: Double?): Result<ChatResponse> {
        return try {
            val response = RetrofitInstance.api.sendChatMessage(
                token = "Bearer $token",
                request = ChatRequest(message, lat, lng)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Chat failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}