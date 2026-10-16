package com.medifind.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medifind.app.data.remote.MedicineResponse
import com.medifind.app.ui.navigation.PendingSearchQueryHolder
import com.medifind.app.ui.theme.PlexMono
import com.medifind.app.ui.viewmodel.SearchUiState
import com.medifind.app.ui.viewmodel.SearchViewModel
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMedicineSelected: (MedicineResponse, Boolean) -> Unit,
    onProfileClick: () -> Unit,
    onScanClick: () -> Unit,
    onBarcodeScanClick: () -> Unit,
    onChatClick: () -> Unit,
    onRemindersClick: () -> Unit,
    onVaultClick: () -> Unit,
    onMyReservationsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel()
) {
    val state = viewModel.uiState

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var emergencyMode by remember { mutableStateOf(false) }
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
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MediFind AI",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(onClick = onMyReservationsClick) {
                        Icon(
                            Icons.Default.Assignment,
                            contentDescription = "My Reservations",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Find Your Medicine",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = viewModel.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Search medicine name") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { startVoiceSearch() }) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Voice Search",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth(),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { viewModel.search() }
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.search() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Search", style = MaterialTheme.typography.titleSmall)
            }

            Spacer(modifier = Modifier.height(8.dp))
            FilterChip(
                selected = emergencyMode,
                onClick = { emergencyMode = !emergencyMode },
                label = { Text("Emergency mode: 24-hour pharmacies only") },
                leadingIcon = {
                    Icon(
                        Icons.Default.LocalHospital,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )

            voiceError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            val quickActions = listOf(
                QuickAction("Reminders", Icons.Default.Notifications, onRemindersClick),
                QuickAction("Rx Vault", Icons.Default.MedicalServices, onVaultClick),
                QuickAction("Scan Rx", Icons.Default.CameraAlt, onScanClick),
                QuickAction("Barcode", Icons.Default.QrCodeScanner, onBarcodeScanClick),
                QuickAction("Ask AI", Icons.Default.Chat, onChatClick)
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(quickActions) { action ->
                    QuickActionCard(action)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (state) {
                is SearchUiState.Idle -> {
                    Text(
                        text = if (emergencyMode) {
                            "Emergency mode is on. Search for the medicine you need and we'll show 24-hour pharmacies first."
                        } else {
                            "Search for a medicine to see availability nearby."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is SearchUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is SearchUiState.Error -> {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
                is SearchUiState.Success -> {
                    if (state.results.isEmpty()) {
                        Text(
                            text = "No medicines found matching \"${viewModel.query}\".",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.results) { medicine ->
                                MedicineResultCard(medicine = medicine, onClick = { onMedicineSelected(medicine, emergencyMode) })
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun QuickActionCard(action: QuickAction) {
    Column(
        modifier = Modifier
            .width(84.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable { action.onClick() }
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.label,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = action.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1
        )
    }
}

@Composable
fun MedicineResultCard(medicine: MedicineResponse, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = medicine.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            medicine.genericName?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Generic: $it",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlexMono),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            medicine.category?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
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