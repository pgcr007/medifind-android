package com.medifind.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.medifind.app.data.repository.AuthRepository
import com.medifind.app.data.repository.TokenManager
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val role: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository()
    private val tokenManager = TokenManager(application)

    var uiState by mutableStateOf<AuthUiState>(AuthUiState.Idle)
        private set

    fun login(email: String, password: String) {
        uiState = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.login(email, password)
            result.onSuccess { response ->
                tokenManager.saveToken(response.token)
                tokenManager.saveUserInfo(response.user.name, response.user.email, response.user.role)
                uiState = AuthUiState.Success(response.user.role)
            }.onFailure { error ->
                uiState = AuthUiState.Error(error.message ?: "Login failed")
            }
        }
    }

    fun register(name: String, email: String, password: String, role: String) {
        uiState = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.register(name, email, password, role)
            result.onSuccess {
                uiState = AuthUiState.Success(it.role)
            }.onFailure { error ->
                uiState = AuthUiState.Error(error.message ?: "Registration failed")
            }
        }
    }

    fun resetState() {
        uiState = AuthUiState.Idle
    }
}