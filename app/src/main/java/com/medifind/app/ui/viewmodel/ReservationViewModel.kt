package com.medifind.app.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medifind.app.data.remote.ReservationResponse
import com.medifind.app.data.repository.ReservationRepository
import com.medifind.app.data.repository.TokenManager
import kotlinx.coroutines.launch
import com.medifind.app.data.remote.ReservationDetailResponse

sealed class ReservationUiState {
    object Idle : ReservationUiState()
    object Loading : ReservationUiState()
    data class Success(val reservation: ReservationResponse) : ReservationUiState()
    data class Error(val message: String) : ReservationUiState()
}

sealed class MyReservationsUiState {
    object Idle : MyReservationsUiState()
    object Loading : MyReservationsUiState()
    data class Success(val reservations: List<ReservationDetailResponse>) : MyReservationsUiState()
    data class Error(val message: String) : MyReservationsUiState()
}

class ReservationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReservationRepository()
    private val tokenManager = TokenManager(application)

    var uiState by mutableStateOf<ReservationUiState>(ReservationUiState.Idle)
        private set

    fun reserve(pharmacyId: String, medicineId: String) {
        val token = tokenManager.getToken()
        if (token == null) {
            uiState = ReservationUiState.Error("Please log in to reserve a medicine.")
            return
        }

        uiState = ReservationUiState.Loading
        viewModelScope.launch {
            val result = repository.createReservation(token, pharmacyId, medicineId)
            result.onSuccess { reservation ->
                uiState = ReservationUiState.Success(reservation)
            }.onFailure { error ->
                // This is where the "out of stock at reservation time" case from
                // our Phase 1 activity diagram surfaces — the backend returns 409
                // if stock changed between search and reserve.
                uiState = ReservationUiState.Error(error.message ?: "Reservation failed")
            }
        }
    }

    fun resetState() {
        uiState = ReservationUiState.Idle
    }

    // reservationId -> the patient's answer to "Was it in stock?". Held in memory only;
    // answering again just replaces the earlier answer on the backend.
    val stockReports = mutableStateMapOf<String, Boolean>()

    var stockReportError by mutableStateOf<String?>(null)
        private set

    fun submitStockReport(reservation: ReservationDetailResponse, inStock: Boolean) {
        val token = tokenManager.getToken()
        if (token == null) {
            stockReportError = "Please log in to answer."
            return
        }
        stockReportError = null
        viewModelScope.launch {
            repository.submitStockReport(
                token = token,
                pharmacyId = reservation.pharmacyId._id,
                medicineId = reservation.medicineId._id,
                inStock = inStock
            ).onSuccess {
                stockReports[reservation._id] = inStock
            }.onFailure {
                stockReportError = "Couldn't send your answer. Try again."
            }
        }
    }

    var myReservationsState by mutableStateOf<MyReservationsUiState>(MyReservationsUiState.Idle)
        private set

    fun loadMyReservations() {
        val token = tokenManager.getToken()
        if (token == null) {
            myReservationsState = MyReservationsUiState.Error("Please log in to view your reservations.")
            return
        }
        myReservationsState = MyReservationsUiState.Loading
        viewModelScope.launch {
            val result = repository.getMyReservations(token)
            result.onSuccess { list ->
                myReservationsState = MyReservationsUiState.Success(list)
            }.onFailure { error ->
                myReservationsState = MyReservationsUiState.Error(error.message ?: "Failed to load reservations")
            }
        }
    }
}