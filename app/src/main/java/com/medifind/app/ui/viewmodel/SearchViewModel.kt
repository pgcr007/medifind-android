package com.medifind.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medifind.app.data.remote.MedicineResponse
import com.medifind.app.data.repository.MedicineRepository
import kotlinx.coroutines.launch

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val results: List<MedicineResponse>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class SearchViewModel : ViewModel() {

    private val repository = MedicineRepository()

    var uiState by mutableStateOf<SearchUiState>(SearchUiState.Idle)
        private set

    var query by mutableStateOf("")
        private set

    fun onQueryChange(newQuery: String) {
        query = newQuery
    }

    fun search() {
        if (query.isBlank()) return
        uiState = SearchUiState.Loading
        viewModelScope.launch {
            val result = repository.searchMedicines(query)
            result.onSuccess { results ->
                uiState = SearchUiState.Success(results)
            }.onFailure { error ->
                uiState = SearchUiState.Error(error.message ?: "Something went wrong")
            }
        }
    }
}