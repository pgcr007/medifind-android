package com.medifind.app.data.repository

import com.medifind.app.data.remote.LoginRequest
import com.medifind.app.data.remote.LoginResponse
import com.medifind.app.data.remote.ProfileUpdateRequest
import com.medifind.app.data.remote.RegisterRequest
import com.medifind.app.data.remote.RetrofitInstance
import com.medifind.app.data.remote.UserProfileResponse
import com.medifind.app.data.remote.UserResponse
import org.json.JSONObject
import retrofit2.Response

class AuthRepository {

    private fun extractErrorMessage(response: Response<*>, fallback: String): String {
        val rawBody = response.errorBody()?.string()
        if (rawBody.isNullOrBlank()) return fallback
        return try {
            JSONObject(rawBody).optString("error", fallback)
        } catch (e: Exception) {
            fallback
        }
    }

    suspend fun register(name: String, email: String, password: String, role: String): Result<UserResponse> {
        return try {
            val response = RetrofitInstance.api.register(RegisterRequest(name, email, password, role))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(extractErrorMessage(response, "Registration failed")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = RetrofitInstance.api.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(extractErrorMessage(response, "Login failed")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfile(token: String): Result<UserProfileResponse> {
        return try {
            val response = RetrofitInstance.api.getProfile("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(extractErrorMessage(response, "Failed to load profile")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(token: String, request: ProfileUpdateRequest): Result<UserProfileResponse> {
        return try {
            val response = RetrofitInstance.api.updateProfile("Bearer $token", request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(extractErrorMessage(response, "Failed to update profile")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}