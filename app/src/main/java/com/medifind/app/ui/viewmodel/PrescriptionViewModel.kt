package com.medifind.app.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medifind.app.data.remote.PrescriptionResponse
import com.medifind.app.data.repository.PrescriptionRepository
import com.medifind.app.data.repository.TokenManager
import kotlinx.coroutines.launch

sealed class PrescriptionUiState {
    object Idle : PrescriptionUiState()
    object Loading : PrescriptionUiState()
    object Success : PrescriptionUiState()
    data class Error(val message: String) : PrescriptionUiState()
}

class PrescriptionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PrescriptionRepository(application.applicationContext)
    private val tokenManager = TokenManager(application)

    var prescriptions by mutableStateOf<List<PrescriptionResponse>>(emptyList())
        private set

    var uiState by mutableStateOf<PrescriptionUiState>(PrescriptionUiState.Idle)
        private set

    fun loadPrescriptions() {
        val token = tokenManager.getToken() ?: run {
            uiState = PrescriptionUiState.Error("Please log in to view your vault.")
            return
        }

        uiState = PrescriptionUiState.Loading
        viewModelScope.launch {
            val result = repository.getMyPrescriptions(token)
            result.onSuccess {
                prescriptions = it
                uiState = PrescriptionUiState.Success
            }.onFailure { error ->
                uiState = PrescriptionUiState.Error(error.message ?: "Failed to load prescriptions")
            }
        }
    }

    fun deletePrescription(id: String, localImageId: String) {
        val token = tokenManager.getToken() ?: return
        viewModelScope.launch {
            val result = repository.deletePrescription(token, id, localImageId)
            result.onSuccess {
                loadPrescriptions()
            }.onFailure { error ->
                uiState = PrescriptionUiState.Error(error.message ?: "Delete failed")
            }
        }
    }

    fun updateNotes(id: String, notes: String) {
        val token = tokenManager.getToken() ?: return
        viewModelScope.launch {
            val result = repository.updatePrescription(token = token, id = id, notes = notes)
            result.onSuccess {
                loadPrescriptions()
            }.onFailure { error ->
                uiState = PrescriptionUiState.Error(error.message ?: "Update failed")
            }
        }
    }

    fun getImageFile(localImageId: String) = repository.getImageFile(localImageId)

    fun resetState() {
        uiState = PrescriptionUiState.Idle
    }
}