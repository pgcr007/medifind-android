package com.medifind.app.data.repository

import com.medifind.app.data.remote.MedicineResponse
import com.medifind.app.data.remote.RetrofitInstance

class MedicineRepository {

    suspend fun searchMedicines(name: String): Result<List<MedicineResponse>> {
        return try {
            val response = RetrofitInstance.api.searchMedicines(name)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Search failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}