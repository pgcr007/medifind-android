package com.medifind.app.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medifind.app.data.remote.ChatSearchResults
import com.medifind.app.data.repository.ChatRepository
import com.medifind.app.data.repository.LocationHelper
import com.medifind.app.data.repository.TokenManager
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val searchResults: ChatSearchResults? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChatRepository()
    private val tokenManager = TokenManager(application)
    private val locationHelper = LocationHelper(application)

    var messages by mutableStateOf<List<ChatMessage>>(emptyList())
        private set

    var inputText by mutableStateOf("")

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun sendMessage() {
        val text = inputText.trim()
        if (text.isBlank()) return

        val token = tokenManager.getToken()
        if (token == null) {
            errorMessage = "Please log in to use the chat."
            return
        }

        messages = messages + ChatMessage(text, isUser = true)
        inputText = ""
        isLoading = true
        errorMessage = null

        viewModelScope.launch {
            val location = locationHelper.getCurrentLocation()
            val result = repository.sendMessage(token, text, location?.first, location?.second)
            isLoading = false
            result.onSuccess { response ->
                messages = messages + ChatMessage(response.reply, isUser = false, searchResults = response.searchResults)
            }.onFailure { error ->
                errorMessage = error.message ?: "Something went wrong"
            }
        }
    }
}