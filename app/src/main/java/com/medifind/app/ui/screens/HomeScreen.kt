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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMedicineSelected: (MedicineResponse) -> Unit,
    onProfileClick: () -> Unit,
    onScanClick: () -> Unit,
    onChatClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel()
) {
    val state = viewModel.uiState

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("MediFind AI") },
                actions = {
                    IconButton(onClick = onScanClick) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Scan Prescription")
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