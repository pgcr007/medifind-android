package com.medifind.app.data.repository

import com.medifind.app.data.remote.PharmacyNearbyResponse
import com.medifind.app.data.remote.PharmacyResponse
import com.medifind.app.data.remote.RetrofitInstance

class PharmacyRepository {

    // emergencyOnly=true asks the backend for 24-hour pharmacies only. sort is one of
    // distance | price | confidence | best; null lets the backend choose its default.
    suspend fun getNearbyPharmacies(
        lat: Double,
        lng: Double,
        medicineId: String?,
        emergencyOnly: Boolean = false,
        sort: String? = null
    ): Result<List<PharmacyNearbyResponse>> {
        return try {
            val response = RetrofitInstance.api.getNearbyPharmacies(
                lat = lat,
                lng = lng,
                medicineId = medicineId,
                emergencyOnly = if (emergencyOnly) true else null,
                sort = sort
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Could not load pharmacies"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPharmacyById(id: String): Result<PharmacyResponse> {
        return try {
            val response = RetrofitInstance.api.getPharmacyById(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Pharmacy not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}