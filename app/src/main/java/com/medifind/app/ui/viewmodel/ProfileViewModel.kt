package com.medifind.app.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medifind.app.data.remote.ProfileUpdateRequest
import com.medifind.app.data.remote.UserProfileResponse
import com.medifind.app.data.repository.AuthRepository
import com.medifind.app.data.repository.TokenManager
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    object Idle : ProfileUiState()
    object Loading : ProfileUiState()
    data class Success(val profile: UserProfileResponse) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

sealed class ProfileSaveState {
    object Idle : ProfileSaveState()
    object Saving : ProfileSaveState()
    object Saved : ProfileSaveState()
    data class Error(val message: String) : ProfileSaveState()
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository()
    private val tokenManager = TokenManager(application)

    var uiState by mutableStateOf<ProfileUiState>(ProfileUiState.Idle)
        private set

    var saveState by mutableStateOf<ProfileSaveState>(ProfileSaveState.Idle)
        private set

    fun loadProfile() {
        val token = tokenManager.getToken()
        if (token == null) {
            uiState = ProfileUiState.Error("Please log in again.")
            return
        }
        uiState = ProfileUiState.Loading
        viewModelScope.launch {
            val result = repository.getProfile(token)
            result.onSuccess { uiState = ProfileUiState.Success(it) }
                .onFailure { uiState = ProfileUiState.Error(it.message ?: "Failed to load profile") }
        }
    }

    fun saveProfile(
        name: String,
        phone: String,
        address: String,
        dob: String?,
        profilePictureBase64: String?
    ) {
        val token = tokenManager.getToken()
        if (token == null) {
            saveState = ProfileSaveState.Error("Please log in again.")
            return
        }
        saveState = ProfileSaveState.Saving
        viewModelScope.launch {
            val result = repository.updateProfile(
                token,
                ProfileUpdateRequest(
                    name = name,
                    phone = phone.ifBlank { null },
                    address = address.ifBlank { null },
                    dob = dob,
                    profilePicture = profilePictureBase64
                )
            )
            result.onSuccess { updated ->
                saveState = ProfileSaveState.Saved
                uiState = ProfileUiState.Success(updated)
                // Keep the cached name/email/role in sync so Home/other screens
                // that read from TokenManager directly stay consistent.
                tokenManager.saveUserInfo(updated.name, updated.email, updated.role)
            }.onFailure {
                saveState = ProfileSaveState.Error(it.message ?: "Failed to save profile")
            }
        }
    }

    fun resetSaveState() {
        saveState = ProfileSaveState.Idle
    }
}