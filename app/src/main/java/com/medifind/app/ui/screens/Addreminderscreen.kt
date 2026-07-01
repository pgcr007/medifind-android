package com.medifind.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medifind.app.data.remote.MedicineResponse
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

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Add Medicine Reminder", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (selectedMedicine == null) {
            OutlinedTextField(
                value = searchViewModel.query,
                onValueChange = {
                    searchViewModel.onQueryChange(it)
                    if (it.length >= 2) searchViewModel.search()
                },
                label = { Text("Search medicine") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            when (searchState) {
                is SearchUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                }
                is SearchUiState.Error -> {
                    Text(
                        text = searchState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                is SearchUiState.Success -> {
                    LazyColumn {
                        items(searchState.results) { medicine ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                onClick = { selectedMedicine = medicine }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = medicine.name, style = MaterialTheme.typography.bodyMedium)
                                    medicine.genericName?.let {
                                        Text(text = "Generic: $it", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        } else {
            Text(text = "Selected: ${selectedMedicine!!.name}", style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = { selectedMedicine = null }) {
                Text("Change medicine")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = refillDays,
                onValueChange = { refillDays = it.filter { c -> c.isDigit() } },
                label = { Text("Refill reminder every (days)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Dosage times (optional, e.g. 08:00)", style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = dosageTimeInput,
                    onValueChange = { dosageTimeInput = it },
                    placeholder = { Text("HH:mm") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {
                    if (dosageTimeInput.matches(Regex("^([01]\\d|2[0-3]):[0-5]\\d$"))) {
                        dosageTimes = dosageTimes + dosageTimeInput
                        dosageTimeInput = ""
                    }
                }) {
                    Text("Add")
                }
            }

            dosageTimes.forEach { time ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = time)
                    TextButton(onClick = { dosageTimes = dosageTimes - time }) {
                        Text("Remove")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val days = refillDays.toIntOrNull() ?: 30
                    reminderViewModel.createReminder(selectedMedicine!!._id, dosageTimes, days)
                    onReminderCreated()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Reminder")
            }
        }
    }
}