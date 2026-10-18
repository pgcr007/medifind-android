package com.medifind.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medifind.app.data.remote.MedicineResponse
import com.medifind.app.ui.navigation.PendingSearchQueryHolder
import com.medifind.app.ui.theme.Amber
import com.medifind.app.ui.theme.BottleGreen
import com.medifind.app.ui.theme.OnAmberLight
import com.medifind.app.ui.theme.OnPaperText
import com.medifind.app.ui.theme.OnSurfaceVariantLight
import com.medifind.app.ui.theme.PlexMono
import com.medifind.app.ui.theme.Rust
import com.medifind.app.ui.theme.WarmPaper
import com.medifind.app.ui.viewmodel.SearchUiState
import com.medifind.app.ui.viewmodel.SearchViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.LocalTime
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
    val greeting = remember { greetingForNow() }

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

    val quickActions = listOf(
        QuickAction("Reminders", Icons.Default.Notifications, onRemindersClick),
        QuickAction("Rx Vault", Icons.Default.MedicalServices, onVaultClick),
        QuickAction("Scan Rx", Icons.Default.CameraAlt, onScanClick),
        QuickAction("Barcode", Icons.Default.QrCodeScanner, onBarcodeScanClick)
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.LocalPharmacy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "MediFind AI",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
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
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Greeting
            item(key = "greeting") {
                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Find your medicine",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Live stock from pharmacies near you.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Hero search card
            item(key = "hero") {
                HomeHero(
                    query = viewModel.query,
                    onQueryChange = viewModel::onQueryChange,
                    onSearch = { viewModel.search() },
                    onVoice = { startVoiceSearch() },
                    emergencyMode = emergencyMode,
                    onToggleEmergency = { emergencyMode = !emergencyMode }
                )
            }

            voiceError?.let { message ->
                item(key = "voice-error") {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Quick actions
            item(key = "quick-header") { HomeSectionLabel("Quick actions") }
            item(key = "quick-tiles") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val containers = listOf(
                        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
                        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
                        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer,
                        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    quickActions.forEachIndexed { index, action ->
                        HomeQuickTile(
                            action = action,
                            container = containers[index].first,
                            onContainer = containers[index].second,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            item(key = "ai-banner") { HomeAiBanner(onClick = onChatClick) }

            // Search state
            when (state) {
                is SearchUiState.Idle -> {
                    item(key = "idle") {
                        HomeIdleHint(
                            emergencyMode = emergencyMode,
                            onSuggestion = { suggestion ->
                                viewModel.onQueryChange(suggestion)
                                viewModel.search()
                            }
                        )
                    }
                }
                is SearchUiState.Loading -> {
                    item(key = "loading") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                is SearchUiState.Error -> {
                    item(key = "error") {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is SearchUiState.Success -> {
                    if (state.results.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                text = "No medicines found matching \"${viewModel.query}\".",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        item(key = "results-header") { HomeSectionLabel("Results") }
                        items(state.results, key = { it._id }) { medicine ->
                            MedicineResultCard(
                                medicine = medicine,
                                onClick = { onMedicineSelected(medicine, emergencyMode) }
                            )
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

private fun greetingForNow(): String {
    val hour = LocalTime.now().hour
    return when {
        hour < 12 -> "GOOD MORNING"
        hour < 17 -> "GOOD AFTERNOON"
        else -> "GOOD EVENING"
    }
}

// ---------- Pieces ----------

@Composable
private fun HomeSectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.width(10.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

/** Bottle-green hero: search field, search button and the emergency toggle in one card. */
@Composable
private fun HomeHero(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onVoice: () -> Unit,
    emergencyMode: Boolean,
    onToggleEmergency: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(BottleGreen, Color(0xFF1B3128))))
    ) {
        // Faint watermark, purely decorative
        Icon(
            imageVector = Icons.Default.MedicalServices,
            contentDescription = null,
            tint = WarmPaper.copy(alpha = 0.07f),
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.TopEnd)
                .offset(x = 36.dp, y = (-24).dp)
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SEARCH THE SHELVES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = WarmPaper.copy(alpha = 0.7f),
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search medicine name") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = BottleGreen)
                },
                trailingIcon = {
                    IconButton(onClick = onVoice) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Voice Search",
                            tint = Amber
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OnPaperText,
                    unfocusedTextColor = OnPaperText,
                    focusedContainerColor = WarmPaper,
                    unfocusedContainerColor = WarmPaper,
                    focusedBorderColor = Amber,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = BottleGreen,
                    focusedPlaceholderColor = OnSurfaceVariantLight,
                    unfocusedPlaceholderColor = OnSurfaceVariantLight
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onSearch,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Amber,
                    contentColor = OnAmberLight
                ),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(
                    "Search",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            HomeEmergencyToggle(enabled = emergencyMode, onToggle = onToggleEmergency)
        }
    }
}

@Composable
private fun HomeEmergencyToggle(enabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) Rust else Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.LocalHospital,
            contentDescription = null,
            tint = WarmPaper,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Emergency mode",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = WarmPaper
            )
            Text(
                text = if (enabled) "Showing 24-hour pharmacies first" else "Only show 24-hour pharmacies",
                style = MaterialTheme.typography.labelSmall,
                color = WarmPaper.copy(alpha = 0.75f)
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WarmPaper,
                checkedTrackColor = WarmPaper.copy(alpha = 0.35f),
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = WarmPaper,
                uncheckedTrackColor = Color.White.copy(alpha = 0.15f),
                uncheckedBorderColor = WarmPaper.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun HomeQuickTile(
    action: QuickAction,
    container: Color,
    onContainer: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable { action.onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(container),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = onContainer,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = action.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

/** Full-width "Ask AI" banner so the chatbot is always visible, not hidden in a scroller. */
@Composable
private fun HomeAiBanner(onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
                .background(Amber)
        )
        Row(
            modifier = Modifier.weight(1f).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Amber),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = OnAmberLight,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ask MediFind AI",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Dosage, alternatives, nearby stock. Ask anything.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HomeIdleHint(
    emergencyMode: Boolean,
    onSuggestion: (String) -> Unit
) {
    val suggestions = listOf("Dolo 650", "Crocin", "Paracetamol", "Azithromycin", "Cetirizine")
    Column {
        HomeSectionLabel("Popular searches")
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                        .clickable { onSuggestion(suggestion) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
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
}

@Composable
fun MedicineResultCard(medicine: MedicineResponse, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Monogram badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = medicine.name.take(1).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medicine.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                medicine.genericName?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = "Generic: $it",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlexMono),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                medicine.category?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------- Voice search (unchanged) ----------

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