package com.medifind.app.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medifind.app.data.remote.ReminderResponse
import com.medifind.app.data.repository.ReminderRepository
import com.medifind.app.data.repository.ReminderScheduler
import com.medifind.app.data.repository.TokenManager
import kotlinx.coroutines.launch

sealed class ReminderUiState {
    object Idle : ReminderUiState()
    object Loading : ReminderUiState()
    object Success : ReminderUiState()
    data class Error(val message: String) : ReminderUiState()
}

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReminderRepository()
    private val tokenManager = TokenManager(application)

    var reminders by mutableStateOf<List<ReminderResponse>>(emptyList())
        private set

    var uiState by mutableStateOf<ReminderUiState>(ReminderUiState.Idle)
        private set

    fun loadReminders() {
        val token = tokenManager.getToken() ?: run {
            uiState = ReminderUiState.Error("Please log in to view reminders.")
            return
        }

        uiState = ReminderUiState.Loading
        viewModelScope.launch {
            val result = repository.getMyReminders(token)
            result.onSuccess {
                reminders = it
                uiState = ReminderUiState.Success
            }.onFailure { error ->
                uiState = ReminderUiState.Error(error.message ?: "Failed to load reminders")
            }
        }
    }

    fun createReminder(medicineId: String, dosageTimes: List<String>, refillIntervalDays: Int) {
        val token = tokenManager.getToken() ?: run {
            uiState = ReminderUiState.Error("Please log in to create a reminder.")
            return
        }

        uiState = ReminderUiState.Loading
        viewModelScope.launch {
            val result = repository.createReminder(token, medicineId, dosageTimes, refillIntervalDays)
            result.onSuccess { reminder ->
                ReminderScheduler.scheduleDosageAlarms(
                    context = getApplication(),
                    reminderId = reminder._id,
                    medicineName = reminder.medicineId.name,
                    dosageTimes = reminder.dosageTimes
                )
                loadReminders()
            }.onFailure { error ->
                uiState = ReminderUiState.Error(error.message ?: "Failed to create reminder")
            }
        }
    }

    fun toggleActive(id: String, isActive: Boolean) {
        val token = tokenManager.getToken() ?: return
        viewModelScope.launch {
            val result = repository.updateReminder(token, id, isActive = isActive)
            result.onSuccess { updated ->
                // If pausing, cancel alarms; if resuming, reschedule them
                if (!isActive) {
                    ReminderScheduler.cancelDosageAlarms(
                        getApplication(), updated._id, updated.dosageTimes
                    )
                } else {
                    ReminderScheduler.scheduleDosageAlarms(
                        context = getApplication(),
                        reminderId = updated._id,
                        medicineName = updated.medicineId.name,
                        dosageTimes = updated.dosageTimes
                    )
                }
                loadReminders()
            }.onFailure { error ->
                uiState = ReminderUiState.Error(error.message ?: "Update failed")
            }
        }
    }

    fun deleteReminder(id: String) {
        val token = tokenManager.getToken() ?: return
        viewModelScope.launch {
            // Capture reminder before deletion so we can cancel its alarms
            val toDelete = reminders.find { it._id == id }
            val result = repository.deleteReminder(token, id)
            result.onSuccess {
                toDelete?.let {
                    ReminderScheduler.cancelDosageAlarms(
                        getApplication(), it._id, it.dosageTimes
                    )
                }
                loadReminders()
            }.onFailure { error ->
                uiState = ReminderUiState.Error(error.message ?: "Delete failed")
            }
        }
    }

    fun resetState() {
        uiState = ReminderUiState.Idle
    }
}