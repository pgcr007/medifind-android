package com.medifind.app.data.repository

import com.medifind.app.data.remote.ReminderRequest
import com.medifind.app.data.remote.ReminderResponse
import com.medifind.app.data.remote.ReminderUpdateRequest
import com.medifind.app.data.remote.RetrofitInstance

class ReminderRepository {

    suspend fun createReminder(
        token: String,
        medicineId: String,
        dosageTimes: List<String>,
        refillIntervalDays: Int
    ): Result<ReminderResponse> {
        return try {
            val response = RetrofitInstance.api.createReminder(
                token = "Bearer $token",
                request = ReminderRequest(medicineId, dosageTimes, refillIntervalDays)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to create reminder"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyReminders(token: String): Result<List<ReminderResponse>> {
        return try {
            val response = RetrofitInstance.api.getMyReminders(token = "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to load reminders"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateReminder(
        token: String,
        id: String,
        dosageTimes: List<String>? = null,
        refillIntervalDays: Int? = null,
        isActive: Boolean? = null
    ): Result<ReminderResponse> {
        return try {
            val response = RetrofitInstance.api.updateReminder(
                token = "Bearer $token",
                id = id,
                request = ReminderUpdateRequest(dosageTimes, refillIntervalDays, isActive)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to update reminder"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteReminder(token: String, id: String): Result<Unit> {
        return try {
            val response = RetrofitInstance.api.deleteReminder(token = "Bearer $token", id = id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to delete reminder"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}