package com.medifind.app.data.repository

import com.medifind.app.data.remote.CreateReviewRequest
import com.medifind.app.data.remote.ReviewResponse
import com.medifind.app.data.remote.RetrofitInstance

class ReviewRepository {

    suspend fun getPharmacyReviews(pharmacyId: String): Result<List<ReviewResponse>> {
        return try {
            val response = RetrofitInstance.api.getPharmacyReviews(pharmacyId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to load reviews"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyReview(token: String, pharmacyId: String): Result<ReviewResponse?> {
        return try {
            val response = RetrofitInstance.api.getMyReview("Bearer $token", pharmacyId)
            if (response.isSuccessful) {
                Result.success(response.body())
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to load your review"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitReview(token: String, pharmacyId: String, rating: Int, comment: String): Result<ReviewResponse> {
        return try {
            val response = RetrofitInstance.api.submitReview(
                token = "Bearer $token",
                request = CreateReviewRequest(pharmacyId, rating, comment)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to submit review"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}