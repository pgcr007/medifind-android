package com.medifind.app.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medifind.app.data.remote.ReviewResponse
import com.medifind.app.data.repository.ReviewRepository
import com.medifind.app.data.repository.TokenManager
import kotlinx.coroutines.launch

sealed class ReviewsUiState {
    object Loading : ReviewsUiState()
    data class Success(val reviews: List<ReviewResponse>) : ReviewsUiState()
    data class Error(val message: String) : ReviewsUiState()
}

sealed class SubmitReviewUiState {
    object Idle : SubmitReviewUiState()
    object Loading : SubmitReviewUiState()
    object Success : SubmitReviewUiState()
    data class Error(val message: String) : SubmitReviewUiState()
}

class ReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReviewRepository()
    private val tokenManager = TokenManager(application)

    var reviewsState by mutableStateOf<ReviewsUiState>(ReviewsUiState.Loading)
        private set

    var submitState by mutableStateOf<SubmitReviewUiState>(SubmitReviewUiState.Idle)
        private set

    var myReview by mutableStateOf<ReviewResponse?>(null)
        private set

    fun loadReviews(pharmacyId: String) {
        reviewsState = ReviewsUiState.Loading
        viewModelScope.launch {
            val result = repository.getPharmacyReviews(pharmacyId)
            result.onSuccess { list ->
                reviewsState = ReviewsUiState.Success(list)
            }.onFailure { error ->
                reviewsState = ReviewsUiState.Error(error.message ?: "Failed to load reviews")
            }
        }
        loadMyReview(pharmacyId)
    }

    private fun loadMyReview(pharmacyId: String) {
        val token = tokenManager.getToken() ?: return
        viewModelScope.launch {
            repository.getMyReview(token, pharmacyId).onSuccess { review ->
                myReview = review
            }
        }
    }

    fun submitReview(pharmacyId: String, rating: Int, comment: String) {
        val token = tokenManager.getToken()
        if (token == null) {
            submitState = SubmitReviewUiState.Error("Please log in to leave a review.")
            return
        }
        submitState = SubmitReviewUiState.Loading
        viewModelScope.launch {
            val result = repository.submitReview(token, pharmacyId, rating, comment)
            result.onSuccess { review ->
                submitState = SubmitReviewUiState.Success
                myReview = review
                loadReviews(pharmacyId)
            }.onFailure { error ->
                submitState = SubmitReviewUiState.Error(error.message ?: "Failed to submit review")
            }
        }
    }

    fun resetSubmitState() {
        submitState = SubmitReviewUiState.Idle
    }
}