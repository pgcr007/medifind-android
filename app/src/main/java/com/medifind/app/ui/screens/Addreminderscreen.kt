package com.medifind.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medifind.app.data.remote.MedicineResponse
import com.medifind.app.ui.theme.Amber
import com.medifind.app.ui.theme.BottleGreen
import com.medifind.app.ui.theme.OnAmberLight
import com.medifind.app.ui.theme.OnPaperText
import com.medifind.app.ui.theme.OnSurfaceVariantLight
import com.medifind.app.ui.theme.PlexMono
import com.medifind.app.ui.theme.WarmPaper
import com.medifind.app.ui.viewmodel.ReminderUiState
import com.medifind.app.ui.viewmodel.ReminderViewModel
import com.medifind.app.ui.viewmodel.SearchUiState
import com.medifind.app.ui.viewmodel.SearchViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AddReminderScreen(
    onReminderCreated: () -> Unit,
    modifier: Modifier = Modifier,
    reminderViewModel: ReminderViewModel = viewModel(),
    searchViewModel: SearchViewModel = viewModel()
) {
    var selectedMedicine by remember { mutableStateOf<MedicineResponse?>(null) }
    var refillDays by remember { mutableIntStateOf(30) }
    var dosageTimes by remember { mutableStateOf<List<String>>(emptyList()) }
    var showTimePicker by remember { mutableStateOf(false) }

    val searchState = searchViewModel.uiState
    val medicine = selectedMedicine

    LaunchedEffect(reminderViewModel.uiState) {
        if (reminderViewModel.uiState is ReminderUiState.Success) {
            reminderViewModel.resetState()
            onReminderCreated()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp)
    ) {
        ReminderHeader(step = if (medicine == null) 1 else 2)
        Spacer(modifier = Modifier.height(16.dp))

        if (medicine == null) {
            // ---------- STEP 1: choose the medicine ----------
            MedicineSearchHero(
                query = searchViewModel.query,
                onQueryChange = {
                    searchViewModel.onQueryChange(it)
                    if (it.length >= 2) searchViewModel.search()
                },
                onSearch = { searchViewModel.search() }
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (searchState) {
                is SearchUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is SearchUiState.Error -> {
                    Text(text = searchState.message, color = MaterialTheme.colorScheme.error)
                }
                is SearchUiState.Success -> {
                    if (searchState.results.isEmpty()) {
                        Text(
                            text = "No medicines found matching \"${searchViewModel.query}\".",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        SectionLabel("Pick your medicine")
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(searchState.results, key = { it._id }) { result ->
                                MedicinePickRow(medicine = result, onClick = { selectedMedicine = result })
                            }
                        }
                    }
                }
                else -> {
                    Text(
                        text = "Start typing the name of the medicine you take. We'll find it for you.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // ---------- STEP 2: set up the schedule ----------
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SelectedMedicineCard(medicine = medicine, onChange = { selectedMedicine = null })

                // Refill cycle
                Column {
                    SectionLabel("Refill cycle")
                    Spacer(modifier = Modifier.height(10.dp))
                    RefillCard(days = refillDays, onDaysChange = { refillDays = it })
                }

                // Daily doses
                Column {
                    SectionLabel("Daily doses")
                    Spacer(modifier = Modifier.height(10.dp))
                    DosesCard(
                        times = dosageTimes,
                        onAddClick = { showTimePicker = true },
                        onRemove = { dosageTimes = dosageTimes - it },
                        onTogglePreset = { value ->
                            dosageTimes = if (value in dosageTimes) dosageTimes - value
                            else (dosageTimes + value).sorted()
                        }
                    )
                }

                ReminderPreview(
                    medicineName = medicine.name,
                    doses = dosageTimes.size,
                    refillDays = refillDays
                )

                (reminderViewModel.uiState as? ReminderUiState.Error)?.let { error ->
                    Text(
                        text = error.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = { reminderViewModel.createReminder(medicine._id, dosageTimes, refillDays) },
                    enabled = reminderViewModel.uiState !is ReminderUiState.Loading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Amber,
                        contentColor = OnAmberLight
                    ),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (reminderViewModel.uiState is ReminderUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = OnAmberLight,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save reminder", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showTimePicker) {
        ClockPickerDialog(
            initialHour = 8,
            initialMinute = 0,
            onDismiss = { showTimePicker = false },
            onConfirm = { h, m ->
                val value = String.format(Locale.US, "%02d:%02d", h, m)
                if (value !in dosageTimes) dosageTimes = (dosageTimes + value).sorted()
                showTimePicker = false
            }
        )
    }
}

// ---------- Header ----------

@Composable
private fun ReminderHeader(step: Int) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "NEW REMINDER",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            // Two-segment step indicator
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(1, 2).forEach { index ->
                    Box(
                        modifier = Modifier
                            .width(if (index == step) 28.dp else 14.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (index <= step) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Step $step of 2",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (step == 1) "Which medicine?" else "Set the schedule",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
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

// ---------- Step 1 ----------

@Composable
private fun MedicineSearchHero(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(BottleGreen, Color(0xFF1B3128))))
    ) {
        Icon(
            imageVector = Icons.Default.MedicalServices,
            contentDescription = null,
            tint = WarmPaper.copy(alpha = 0.07f),
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopEnd)
                .offset(x = 28.dp, y = (-20).dp)
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
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BottleGreen) },
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
        }
    }
}

@Composable
private fun MedicinePickRow(medicine: MedicineResponse, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Monogram(text = medicine.name, size = 42)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medicine.name,
                    style = MaterialTheme.typography.titleSmall,
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
private fun Monogram(text: String, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.take(1).uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

// ---------- Step 2 ----------

@Composable
private fun SelectedMedicineCard(medicine: MedicineResponse, onChange: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(BottleGreen, Color(0xFF1B3128))))
    ) {
        Icon(
            imageVector = Icons.Default.MedicalServices,
            contentDescription = null,
            tint = WarmPaper.copy(alpha = 0.07f),
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 24.dp, y = 28.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .border(2.dp, Amber, CircleShape)
                    .background(WarmPaper.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = medicine.name.take(1).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = WarmPaper
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "REMINDER FOR",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmPaper.copy(alpha = 0.7f),
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = medicine.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = WarmPaper
                )
                medicine.genericName?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlexMono),
                        color = WarmPaper.copy(alpha = 0.75f)
                    )
                }
            }
            TextButton(onClick = onChange) {
                Text("Change", color = Amber, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LedgerCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        content = content
    )
}

@Composable
private fun RefillCard(days: Int, onDaysChange: (Int) -> Unit) {
    val nextRefill = remember(days) {
        LocalDate.now().plusDays(days.toLong())
            .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
    }

    LedgerCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilledTonalIconButton(
                    onClick = { onDaysChange((days - 1).coerceAtLeast(1)) },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) { Icon(Icons.Default.Remove, contentDescription = "Fewer days") }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = days.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (days == 1) "DAY" else "DAYS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.5.sp
                    )
                }

                FilledTonalIconButton(
                    onClick = { onDaysChange((days + 1).coerceAtMost(365)) },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) { Icon(Icons.Default.Add, contentDescription = "More days") }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                listOf(7, 15, 30, 60, 90).forEach { preset ->
                    FilterChip(
                        selected = days == preset,
                        onClick = { onDaysChange(preset) },
                        label = { Text("$preset") },
                        shape = RoundedCornerShape(50)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "You'll be nudged to refill around $nextRefill.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DosesCard(
    times: List<String>,
    onAddClick: () -> Unit,
    onRemove: (String) -> Unit,
    onTogglePreset: (String) -> Unit
) {
    LedgerCard {
        if (times.isEmpty()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "No dose times yet",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Optional. Add times to get a reminder for each dose, or skip for refill alerts only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            times.forEachIndexed { index, time ->
                DoseRow(index = index + 1, time = time, onRemove = { onRemove(time) })
                if (index != times.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 68.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Quick presets
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("08:00" to "Morning", "14:00" to "Afternoon", "21:00" to "Night").forEach { (value, label) ->
                FilterChip(
                    selected = value in times,
                    onClick = { onTogglePreset(value) },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    shape = RoundedCornerShape(50)
                )
            }
        }

        // Add custom time
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAddClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Amber),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = OnAmberLight, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Add a custom time",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun DoseRow(index: Int, time: String, onRemove: () -> Unit) {
    val (label, icon) = daypart(time)
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "DOSE $index  ·  ${label.uppercase(Locale.getDefault())}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Text(
                text = formatTime12h(time),
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = PlexMono),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove dose",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** One-line plain-language summary of what is about to be saved. */
@Composable
private fun ReminderPreview(medicineName: String, doses: Int, refillDays: Int) {
    val doseText = when (doses) {
        0 -> "refill alerts only"
        1 -> "1 dose a day"
        else -> "$doses doses a day"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Notifications,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "$medicineName: $doseText, refill every $refillDays ${if (refillDays == 1) "day" else "days"}.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/** Part of day for a "HH:mm" string, with a matching icon. */
private fun daypart(hhmm: String): Pair<String, ImageVector> {
    val hour = hhmm.substringBefore(":").toIntOrNull() ?: 0
    return when (hour) {
        in 5..11 -> "Morning" to Icons.Default.WbTwilight
        in 12..16 -> "Afternoon" to Icons.Default.WbSunny
        in 17..20 -> "Evening" to Icons.Default.WbTwilight
        else -> "Night" to Icons.Default.NightsStay
    }
}

/** "14:30" -> "2:30 PM". Falls back to the raw string if it can't be parsed. */
private fun formatTime12h(hhmm: String): String {
    val parts = hhmm.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: return hhmm
    val m = parts.getOrNull(1)?.toIntOrNull() ?: return hhmm
    val suffix = if (h < 12) "AM" else "PM"
    val h12 = when (val x = h % 12) { 0 -> 12; else -> x }
    return String.format(Locale.US, "%d:%02d %s", h12, m, suffix)
}

// ---------- Clock dialog ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClockPickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timeState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )
    var showKeyboard by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SET DOSAGE TIME",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))

                val colors = TimePickerDefaults.colors(
                    clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                    clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                    selectorColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    periodSelectorBorderColor = MaterialTheme.colorScheme.outline,
                    periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface
                )

                if (showKeyboard) {
                    TimeInput(state = timeState, colors = colors)
                } else {
                    TimePicker(state = timeState, colors = colors)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showKeyboard = !showKeyboard }) {
                        Icon(
                            imageVector = if (showKeyboard) Icons.Default.Schedule else Icons.Default.Keyboard,
                            contentDescription = if (showKeyboard) "Use clock" else "Use keyboard",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = { onConfirm(timeState.hour, timeState.minute) }) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}