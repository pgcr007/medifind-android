package com.medifind.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medifind.app.data.remote.MedicineResponse
import com.medifind.app.ui.theme.PlexMono
import com.medifind.app.ui.viewmodel.ReminderUiState
import com.medifind.app.ui.viewmodel.ReminderViewModel
import com.medifind.app.ui.viewmodel.SearchUiState
import com.medifind.app.ui.viewmodel.SearchViewModel

@Composable
fun AddReminderScreen(
    onReminderCreated: () -> Unit,
    modifier: Modifier = Modifier,
    reminderViewModel: ReminderViewModel = viewModel(),
    searchViewModel: SearchViewModel = viewModel()
) {
    var selectedMedicine by remember { mutableStateOf<MedicineResponse?>(null) }
    var refillDays by remember { mutableStateOf("30") }
    var dosageTimeInput by remember { mutableStateOf("") }
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
                text = "Dosage times (optional, e.g. 08:00)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = dosageTimeInput,
                    onValueChange = { dosageTimeInput = it },
                    placeholder = { Text("HH:mm") },
                    leadingIcon = {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (dosageTimeInput.matches(Regex("^([01]\\d|2[0-3]):[0-5]\\d$"))) {
                            dosageTimes = dosageTimes + dosageTimeInput
                            dosageTimeInput = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Add")
                }
            }

            if (dosageTimes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRowSimple {
                    dosageTimes.forEach { time ->
                        AssistChip(
                            onClick = { dosageTimes = dosageTimes - time },
                            label = {
                                Text(time, style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlexMono))
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