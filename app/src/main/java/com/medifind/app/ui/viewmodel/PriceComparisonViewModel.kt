package com.medifind.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medifind.app.data.repository.MedicineRepository
import com.medifind.app.data.repository.PharmacyRepository
import kotlinx.coroutines.launch

data class PriceComparisonRow(
    val medicineId: String,
    val medicineName: String,
    val genericName: String?,
    val bestPrice: Double?,
    val pharmacyName: String?,
    val distanceKm: Double?,
    val isOriginal: Boolean
)

sealed class PriceComparisonUiState {
    object Loading : PriceComparisonUiState()
    data class Success(val rows: List<PriceComparisonRow>) : PriceComparisonUiState()
    data class Error(val message: String) : PriceComparisonUiState()
}

class PriceComparisonViewModel : ViewModel() {

    private val medicineRepository = MedicineRepository()
    private val pharmacyRepository = PharmacyRepository()

    var uiState by mutableStateOf<PriceComparisonUiState>(PriceComparisonUiState.Loading)
        private set

    fun loadComparison(lat: Double, lng: Double, medicineId: String, medicineName: String) {
        uiState = PriceComparisonUiState.Loading
        viewModelScope.launch {
            val alternatives = medicineRepository.getAlternatives(medicineId).getOrNull() ?: emptyList()

            val medicinesToCompare = mutableListOf(Triple(medicineId, medicineName, null as String?))
            alternatives.forEach { alt ->
                medicinesToCompare.add(Triple(alt._id, alt.name, alt.genericName))
            }

            val rows = medicinesToCompare.map { (id, name, generic) ->
                val pharmacies = pharmacyRepository.getNearbyPharmacies(lat, lng, id).getOrNull() ?: emptyList()
                val best = pharmacies.filter { it.price != null }.minByOrNull { it.price!! }
                PriceComparisonRow(
                    medicineId = id,
                    medicineName = name,
                    genericName = generic,
                    bestPrice = best?.price,
                    pharmacyName = best?.name,
                    distanceKm = best?.distanceKm,
                    isOriginal = id == medicineId
                )
            }

            uiState = PriceComparisonUiState.Success(rows.sortedBy { it.bestPrice ?: Double.MAX_VALUE })
        }
    }
}