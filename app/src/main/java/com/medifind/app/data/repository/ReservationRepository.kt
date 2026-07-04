package com.medifind.app.data.repository

import com.medifind.app.data.remote.ReservationRequest
import com.medifind.app.data.remote.ReservationResponse
import com.medifind.app.data.remote.RetrofitInstance
import com.medifind.app.data.remote.ReservationDetailResponse

class ReservationRepository {

    suspend fun createReservation(token: String, pharmacyId: String, medicineId: String): Result<ReservationResponse> {
        return try {
            val response = RetrofitInstance.api.createReservation(
                token = "Bearer $token",
                request = ReservationRequest(pharmacyId, medicineId)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Reservation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyReservations(token: String): Result<List<ReservationDetailResponse>> {
        return try {
            val response = RetrofitInstance.api.getMyReservations("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to load reservations"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}