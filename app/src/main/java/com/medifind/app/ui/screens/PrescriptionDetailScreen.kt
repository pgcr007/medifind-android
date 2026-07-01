package com.medifind.app.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medifind.app.data.remote.PrescriptionResponse
import com.medifind.app.ui.viewmodel.PrescriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionDetailScreen(
    prescription: PrescriptionResponse,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: PrescriptionViewModel = viewModel()
) {
    var notes by remember { mutableStateOf(prescription.notes ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val imageFile = remember { viewModel.getImageFile(prescription.localImageId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prescription Details") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (imageFile != null && imageFile.exists()) {
                val bitmap = remember(imageFile.path) { BitmapFactory.decodeFile(imageFile.path) }
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Prescription image",
                        modifier = Modifier.fillMaxWidth().height(240.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("Image not available on this device")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Medicines", style = MaterialTheme.typography.titleSmall)
            Text(prescription.medicines?.joinToString(", ") ?: "—")

            Spacer(modifier = Modifier.height(12.dp))

            prescription.doctorName?.takeIf { it.isNotBlank() }?.let {
                Text("Doctor", style = MaterialTheme.typography.titleSmall)
                Text(it)
                Spacer(modifier = Modifier.height(12.dp))
            }

            prescription.createdAt?.let {
                Text("Saved on", style = MaterialTheme.typography.titleSmall)
                Text(it.take(10))
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text("Extracted Text", style = MaterialTheme.typography.titleSmall)
            Text(prescription.extractedText ?: "—", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.updateNotes(prescription._id, notes) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Notes")
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Prescription")
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Prescription") },
            text = { Text("This will permanently delete this prescription from your vault, including the saved image on this device.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deletePrescription(prescription._id, prescription.localImageId)
                    onDeleted()
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}