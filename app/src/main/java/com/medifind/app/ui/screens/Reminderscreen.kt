package com.medifind.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medifind.app.data.remote.ReminderResponse
import com.medifind.app.ui.viewmodel.ReminderUiState
import com.medifind.app.ui.viewmodel.ReminderViewModel

@Composable
fun ReminderScreen(
    onAddReminder: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReminderViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadReminders()
    }

    val state = viewModel.uiState

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Medicine Reminders", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onAddReminder) {
                Icon(Icons.Default.Add, contentDescription = "Add reminder")
            }
        }

        when (state) {
            is ReminderUiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ReminderUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            else -> {
                if (viewModel.reminders.isEmpty()) {
                    Text(
                        text = "No reminders set yet. Tap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                        items(viewModel.reminders) { reminder ->
                            ReminderCard(
                                reminder = reminder,
                                onToggle = { viewModel.toggleActive(reminder._id, !reminder.isActive) },
                                onDelete = { viewModel.deleteReminder(reminder._id) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderCard(
    reminder: ReminderResponse,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = reminder.medicineId.name, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete reminder")
                }
            }

            if (reminder.dosageTimes.isNotEmpty()) {
                Text(
                    text = "Dosage times: ${reminder.dosageTimes.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = "Refill every ${reminder.refillIntervalDays} days",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (reminder.isActive) "Active" else "Paused",
                    style = MaterialTheme.typography.bodySmall
                )
                Switch(checked = reminder.isActive, onCheckedChange = { onToggle() })
            }
        }
    }
}