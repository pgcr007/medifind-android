package com.medifind.app.data.repository

import android.content.Context
import com.medifind.app.data.remote.PrescriptionRequest
import com.medifind.app.data.remote.PrescriptionResponse
import com.medifind.app.data.remote.PrescriptionUpdateRequest
import com.medifind.app.data.remote.RetrofitInstance
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class PrescriptionRepository(private val context: Context) {

    // ---- Local image storage (internal storage, device-only) ----

    fun saveImageLocally(imageBytes: ByteArray): String {
        val uuid = UUID.randomUUID().toString()
        val file = File(context.filesDir, "$uuid.jpg")
        FileOutputStream(file).use { it.write(imageBytes) }
        return uuid
    }

    fun getImageFile(localImageId: String): File? {
        val file = File(context.filesDir, "$localImageId.jpg")
        return if (file.exists()) file else null
    }

    fun deleteImageLocally(localImageId: String): Boolean {
        val file = File(context.filesDir, "$localImageId.jpg")
        return if (file.exists()) file.delete() else false
    }

    // ---- Backend API (structured data only) ----

    suspend fun createPrescription(
        token: String,
        localImageId: String,
        extractedText: String? = null,
        medicines: List<String>? = null,
        doctorName: String? = null,
        prescriptionDate: String? = null,
        notes: String? = null
    ): Result<PrescriptionResponse> {
        return try {
            val response = RetrofitInstance.api.createPrescription(
                token = "Bearer $token",
                request = PrescriptionRequest(
                    localImageId, extractedText, medicines, doctorName, prescriptionDate, notes
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to create prescription"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyPrescriptions(token: String): Result<List<PrescriptionResponse>> {
        return try {
            val response = RetrofitInstance.api.getMyPrescriptions(token = "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to load prescriptions"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPrescriptionById(token: String, id: String): Result<PrescriptionResponse> {
        return try {
            val response = RetrofitInstance.api.getPrescriptionById(token = "Bearer $token", id = id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to load prescription"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePrescription(
        token: String,
        id: String,
        extractedText: String? = null,
        medicines: List<String>? = null,
        doctorName: String? = null,
        prescriptionDate: String? = null,
        notes: String? = null
    ): Result<PrescriptionResponse> {
        return try {
            val response = RetrofitInstance.api.updatePrescription(
                token = "Bearer $token",
                id = id,
                request = PrescriptionUpdateRequest(
                    extractedText, medicines, doctorName, prescriptionDate, notes
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to update prescription"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePrescription(token: String, id: String, localImageId: String): Result<Unit> {
        return try {
            val response = RetrofitInstance.api.deletePrescription(token = "Bearer $token", id = id)
            if (response.isSuccessful) {
                deleteImageLocally(localImageId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to delete prescription"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}