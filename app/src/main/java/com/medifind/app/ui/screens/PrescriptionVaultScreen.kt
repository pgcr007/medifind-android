package com.medifind.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medifind.app.data.remote.PrescriptionResponse
import com.medifind.app.ui.viewmodel.PrescriptionUiState
import com.medifind.app.ui.viewmodel.PrescriptionViewModel
import android.graphics.BitmapFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionVaultScreen(
    onAddNew: () -> Unit,
    onOpenDetail: (PrescriptionResponse) -> Unit,
    viewModel: PrescriptionViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    val prescriptions = viewModel.prescriptions

    LaunchedEffect(Unit) {
        viewModel.loadPrescriptions()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Prescription Vault") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNew) {
                Text("+")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is PrescriptionUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is PrescriptionUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                is PrescriptionUiState.Success -> {
                    if (prescriptions.isEmpty()) {
                        Text(
                            text = "No prescriptions saved yet. Tap + to scan one.",
                            modifier = Modifier.align(Alignment.Center).padding(16.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                            items(prescriptions) { prescription ->
                                PrescriptionCard(
                                    prescription = prescription,
                                    imageFile = viewModel.getImageFile(prescription.localImageId),
                                    onClick = { onOpenDetail(prescription) },
                                    onDelete = {
                                        viewModel.deletePrescription(
                                            prescription._id,
                                            prescription.localImageId
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
                is PrescriptionUiState.Idle -> {}
            }
        }
    }
}

@Composable
private fun PrescriptionCard(
    prescription: PrescriptionResponse,
    imageFile: java.io.File?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imageFile != null && imageFile.exists()) {
                val bitmap = remember(imageFile.path) {
                    BitmapFactory.decodeFile(imageFile.path)
                }
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Prescription thumbnail",
                        modifier = Modifier.size(56.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("N/A", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prescription.medicines?.joinToString(", ") ?: "Unnamed prescription",
                    style = MaterialTheme.typography.bodyLarge
                )
                prescription.doctorName?.let {
                    if (it.isNotBlank()) {
                        Text(text = "Dr. $it", style = MaterialTheme.typography.bodySmall)
                    }
                }
                prescription.createdAt?.let {
                    Text(text = it.take(10), style = MaterialTheme.typography.labelSmall)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}