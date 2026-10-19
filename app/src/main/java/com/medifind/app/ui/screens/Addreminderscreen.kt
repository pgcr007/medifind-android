package com.medifind.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medifind.app.data.remote.MedicineResponse
import com.medifind.app.ui.theme.PlexMono
import com.medifind.app.ui.viewmodel.ReminderUiState
import com.medifind.app.ui.viewmodel.ReminderViewModel
import com.medifind.app.ui.viewmodel.SearchUiState
import com.medifind.app.ui.viewmodel.SearchViewModel
import java.util.Locale

@Composable
fun AddReminderScreen(
    onReminderCreated: () -> Unit,
    modifier: Modifier = Modifier,
    reminderViewModel: ReminderViewModel = viewModel(),
    searchViewModel: SearchViewModel = viewModel()
) {
    var selectedMedicine by remember { mutableStateOf<MedicineResponse?>(null) }
    var refillDays by remember { mutableStateOf("30") }
    var pickedTime by remember { mutableStateOf<String?>(null) }   // "HH:mm" or null
    var showTimePicker by remember { mutableStateOf(false) }
    var dosageTimes by remember { mutableStateOf<List<String>>(emptyList()) }

    val searchState = searchViewModel.uiState

    LaunchedEffect(reminderViewModel.uiState) {
        if (reminderViewModel.uiState is ReminderUiState.Success) {
            reminderViewModel.resetState()
            onReminderCreated()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Add Medicine Reminder",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (selectedMedicine == null) {
            OutlinedTextField(
                value = searchViewModel.query,
                onValueChange = {
                    searchViewModel.onQueryChange(it)
                    if (it.length >= 2) searchViewModel.search()
                },
                label = { Text("Search medicine") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            when (searchState) {
                is SearchUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is SearchUiState.Error -> {
                    Text(
                        text = searchState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                is SearchUiState.Success -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(searchState.results) { medicine ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                onClick = { selectedMedicine = medicine },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = medicine.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    medicine.genericName?.let {
                                        Text(
                                            text = "Generic: $it",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlexMono),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        } else {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedMedicine!!.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    TextButton(onClick = { selectedMedicine = null }) {
                        Text("Change", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = refillDays,
                onValueChange = { refillDays = it.filter { c -> c.isDigit() } },
                label = { Text("Refill reminder every (days)") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Dosage times (optional)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Tappable time box -> opens the clock
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            if (pickedTime != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { showTimePicker = true }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = pickedTime?.let { formatTime12h(it) } ?: "Tap to set time",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (pickedTime != null) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        pickedTime?.let { t ->
                            if (t !in dosageTimes) dosageTimes = (dosageTimes + t).sorted()
                            pickedTime = null
                        }
                    },
                    enabled = pickedTime != null,
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }

            // Quick presets so common schedules take one tap
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("08:00" to "Morning", "14:00" to "Afternoon", "21:00" to "Night").forEach { (value, label) ->
                    AssistChip(
                        onClick = {
                            if (value !in dosageTimes) dosageTimes = (dosageTimes + value).sorted()
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            if (showTimePicker) {
                val initial = pickedTime?.split(":")
                ClockPickerDialog(
                    initialHour = initial?.getOrNull(0)?.toIntOrNull() ?: 8,
                    initialMinute = initial?.getOrNull(1)?.toIntOrNull() ?: 0,
                    onDismiss = { showTimePicker = false },
                    onConfirm = { h, m ->
                        pickedTime = String.format(Locale.US, "%02d:%02d", h, m)
                        showTimePicker = false
                    }
                )
            }

            if (dosageTimes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRowSimple {
                    dosageTimes.forEach { time ->
                        AssistChip(
                            onClick = { dosageTimes = dosageTimes - time },
                            label = {
                                Text(
                                    formatTime12h(time),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlexMono)
                                )
                            },
                            trailingIcon = {
                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                trailingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            if (reminderViewModel.uiState is ReminderUiState.Error) {
                Text(
                    text = (reminderViewModel.uiState as ReminderUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = {
                    val days = refillDays.toIntOrNull() ?: 30
                    reminderViewModel.createReminder(selectedMedicine!!._id, dosageTimes, days)
                },
                enabled = reminderViewModel.uiState !is ReminderUiState.Loading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (reminderViewModel.uiState is ReminderUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save Reminder", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Minimal wrapping row so dosage-time chips flow onto multiple lines instead of
 * being clipped or forced into a horizontally-scrolling Row. Avoids pulling in
 * the full accompanist FlowRow dependency for one small use case.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowRowSimple(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
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