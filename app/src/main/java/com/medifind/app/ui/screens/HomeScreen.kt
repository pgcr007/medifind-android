package com.medifind.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medifind.app.data.remote.MedicineResponse
import com.medifind.app.ui.viewmodel.SearchUiState
import com.medifind.app.ui.viewmodel.SearchViewModel
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.QrCodeScanner
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.medifind.app.ui.navigation.PendingSearchQueryHolder
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMedicineSelected: (MedicineResponse) -> Unit,
    onProfileClick: () -> Unit,
    onScanClick: () -> Unit,
    onBarcodeScanClick: () -> Unit,
    onChatClick: () -> Unit,
    onRemindersClick: () -> Unit,
    onVaultClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel()
) {
    val state = viewModel.uiState

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isListening by remember { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf<String?>(null) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                isListening = true
                voiceError = null
                val result = kotlinx.coroutines.withTimeoutOrNull(10_000) { listenForSpeech(context) }
                    ?: Result.failure(Exception("Listening timed out. Try again."))
                isListening = false
                result.onSuccess { text ->
                    viewModel.onQueryChange(text)
                    viewModel.search()
                }.onFailure {
                    voiceError = it.message ?: "Voice search failed. Try again."
                }
            }
        } else {
            voiceError = "Microphone permission is required for voice search."
        }
    }

    fun startVoiceSearch() {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasMicPermission) {
            scope.launch {
                isListening = true
                voiceError = null
                val result = kotlinx.coroutines.withTimeoutOrNull(10_000) { listenForSpeech(context) } ?: Result.failure(Exception("Listening timed out. Try again."))
                isListening = false
                result.onSuccess { text ->
                    viewModel.onQueryChange(text)
                    viewModel.search()
                }.onFailure {
                    voiceError = it.message ?: "Voice search failed. Try again."
                }
            }
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(Unit) {
        PendingSearchQueryHolder.query?.let { pending ->
            PendingSearchQueryHolder.query = null
            viewModel.onQueryChange(pending)
            viewModel.search()
        }
    }

    if (isListening) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Listening...") },
            text = {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("MediFind AI") },
                actions = {
                    IconButton(onClick = onRemindersClick) {
                        Icon(Icons.Default.Notifications, contentDescription = "Reminders")
                    }
                    IconButton(onClick = onVaultClick) {
                        Icon(Icons.Default.MedicalServices, contentDescription = "Prescription Vault")
                    }
                    IconButton(onClick = onScanClick) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Scan Prescription")
                    }
                    IconButton(onClick = onBarcodeScanClick) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
                    }
                    IconButton(onClick = { startVoiceSearch() }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice Search")
                    }
                    IconButton(onClick = onChatClick) {
                        Icon(Icons.Default.Chat, contentDescription = "Chat Assistant")
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(text = "Find Your Medicine", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = viewModel.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Search medicine name") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { viewModel.search() }
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.search() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Search")
            }

            voiceError?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (state) {
                is SearchUiState.Idle -> {
                    Text(
                        text = "Search for a medicine to see availability nearby.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is SearchUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is SearchUiState.Error -> {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
                is SearchUiState.Success -> {
                    if (state.results.isEmpty()) {
                        Text(text = "No medicines found matching \"${viewModel.query}\".")
                    } else {
                        LazyColumn {
                            items(state.results) { medicine ->
                                MedicineResultCard(medicine = medicine, onClick = { onMedicineSelected(medicine) })
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MedicineResultCard(medicine: MedicineResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = medicine.name, style = MaterialTheme.typography.titleMedium)
            medicine.genericName?.let {
                Text(text = "Generic: $it", style = MaterialTheme.typography.bodySmall)
            }
            medicine.category?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private suspend fun listenForSpeech(context: android.content.Context): Result<String> =
    suspendCancellableCoroutine { cont ->
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            cont.resumeWith(Result.success(Result.failure(Exception("Speech recognition isn't available on this device."))))
            return@suspendCancellableCoroutine
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        var resumed = false

        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                if (!resumed) {
                    resumed = true
                    cont.resumeWith(Result.success(Result.failure(Exception(speechErrorMessage(error)))))
                }
                recognizer.destroy()
            }

            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!resumed) {
                    resumed = true
                    if (text != null) {
                        cont.resumeWith(Result.success(Result.success(text)))
                    } else {
                        cont.resumeWith(Result.success(Result.failure(Exception("Couldn't understand. Try again."))))
                    }
                }
                recognizer.destroy()
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        cont.invokeOnCancellation { recognizer.destroy() }
        recognizer.startListening(intent)
    }

private fun speechErrorMessage(error: Int): String = when (error) {
    SpeechRecognizer.ERROR_NO_MATCH -> "Couldn't understand. Try again."
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Try again."
    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network error. Check your connection."
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
    else -> "Voice search failed. Try again."
}