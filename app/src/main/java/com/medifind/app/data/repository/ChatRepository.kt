package com.medifind.app.data.repository

import com.medifind.app.data.remote.ChatRequest
import com.medifind.app.data.remote.RetrofitInstance

class ChatRepository {

    suspend fun sendMessage(token: String, message: String): Result<String> {
        return try {
            val response = RetrofitInstance.api.sendChatMessage(
                token = "Bearer $token",
                request = ChatRequest(message)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.reply)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Chat failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}