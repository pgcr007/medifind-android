package com.medifind.app.data.repository

import com.medifind.app.data.remote.LoginRequest
import com.medifind.app.data.remote.LoginResponse
import com.medifind.app.data.remote.RegisterRequest
import com.medifind.app.data.remote.RetrofitInstance
import com.medifind.app.data.remote.UserResponse

class AuthRepository {

    suspend fun register(name: String, email: String, password: String, role: String): Result<UserResponse> {
        return try {
            val response = RetrofitInstance.api.register(RegisterRequest(name, email, password, role))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Registration failed"))
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
                Result.failure(Exception(response.errorBody()?.string() ?: "Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}