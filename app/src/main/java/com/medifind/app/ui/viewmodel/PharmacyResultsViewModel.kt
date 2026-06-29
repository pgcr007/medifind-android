package com.medifind.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medifind.app.data.remote.MedicineResponse
import com.medifind.app.data.remote.PharmacyNearbyResponse
import com.medifind.app.data.repository.MedicineRepository
import com.medifind.app.data.repository.PharmacyRepository
import kotlinx.coroutines.launch

sealed class PharmacyResultsUiState {
    object Loading : PharmacyResultsUiState()
    data class Success(val pharmacies: List<PharmacyNearbyResponse>) : PharmacyResultsUiState()
    data class Error(val message: String) : PharmacyResultsUiState()
}

class PharmacyResultsViewModel : ViewModel() {

    private val repository = PharmacyRepository()
    private val medicineRepository = MedicineRepository()

    var uiState by mutableStateOf<PharmacyResultsUiState>(PharmacyResultsUiState.Loading)
        private set

    var alternatives by mutableStateOf<List<MedicineResponse>>(emptyList())
        private set

    var isLoadingAlternatives by mutableStateOf(false)
        private set

    fun loadNearbyPharmacies(lat: Double, lng: Double, medicineId: String) {
        uiState = PharmacyResultsUiState.Loading
        viewModelScope.launch {
            val result = repository.getNearbyPharmacies(lat, lng, medicineId)
            result.onSuccess { pharmacies ->
                uiState = PharmacyResultsUiState.Success(pharmacies)
                if (pharmacies.isEmpty()) {
                    loadAlternatives(medicineId)
                }
            }.onFailure { error ->
                uiState = PharmacyResultsUiState.Error(error.message ?: "Could not load pharmacies")
            }
        }
    }

    private fun loadAlternatives(medicineId: String) {
        isLoadingAlternatives = true
        viewModelScope.launch {
            val result = medicineRepository.getAlternatives(medicineId)
            isLoadingAlternatives = false
            result.onSuccess { meds ->
                alternatives = meds
            }
        }
    }
}