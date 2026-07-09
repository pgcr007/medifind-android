package com.medifind.app.data.repository

import com.medifind.app.data.remote.ChatRequest
import com.medifind.app.data.remote.ChatResponse
import com.medifind.app.data.remote.RetrofitInstance
import org.json.JSONObject
import retrofit2.Response

class ChatRepository {

    private fun extractErrorMessage(response: Response<*>, fallback: String): String {
        val rawBody = response.errorBody()?.string()
        if (rawBody.isNullOrBlank()) return fallback
        return try {
            JSONObject(rawBody).optString("error", fallback)
        } catch (e: Exception) {
            fallback
        }
    }

    suspend fun sendMessage(token: String, message: String, lat: Double?, lng: Double?): Result<ChatResponse> {
        return try {
            val response = RetrofitInstance.api.sendChatMessage(
                token = "Bearer $token",
                request = ChatRequest(message, lat, lng)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(extractErrorMessage(response, "Chat failed")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}