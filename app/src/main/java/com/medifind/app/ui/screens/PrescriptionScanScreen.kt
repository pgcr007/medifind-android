package com.medifind.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.medifind.app.data.repository.OcrHelper
import com.medifind.app.data.repository.PrescriptionParser
import com.medifind.app.data.repository.PrescriptionRepository
import com.medifind.app.data.repository.TokenManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream

enum class ScanMode { SEARCH, VAULT, BARCODE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionScanScreen(
    mode: ScanMode = ScanMode.SEARCH,
    onMedicineNameSelected: (String) -> Unit = {},
    onSavedToVault: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val ocrHelper = remember { OcrHelper() }
    val prescriptionRepository = remember { PrescriptionRepository(context.applicationContext) }
    val tokenManager = remember { TokenManager(context) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var candidateLines by remember { mutableStateOf<List<String>>(emptyList()) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var fullExtractedText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val selectedMedicines = remember { mutableStateListOf<String>() }
    var doctorName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = when (mode) {
                ScanMode.VAULT -> "Scan Prescription for Vault"
                ScanMode.BARCODE -> "Scan Barcode"
                ScanMode.SEARCH -> "Scan Prescription"
            },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(16.dp)
        )

        if (!hasCameraPermission) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (mode == ScanMode.BARCODE) Icons.Default.QrCodeScanner else Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Camera permission is required to scan.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
        } else if (candidateLines.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val capture = ImageCapture.Builder().build()
                            imageCapture = capture
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    capture
                                )
                            } catch (e: Exception) {
                                errorMessage = "Could not start camera: ${e.message}"
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    }
                )

                if (isProcessing) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Button(
                onClick = {
                    val capture = imageCapture ?: return@Button

                    isProcessing = true
                    errorMessage = null

                    capture.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                                val bitmap = imageProxyToBitmap(image)
                                image.close()
                                scope.launch {
                                    if (mode == ScanMode.BARCODE) {
                                        val result = scanBarcode(bitmap)
                                        isProcessing = false
                                        result.onSuccess { value ->
                                            onMedicineNameSelected(value)
                                        }.onFailure {
                                            errorMessage = it.message ?: "No barcode detected. Try again."
                                        }
                                    } else {
                                        val result = ocrHelper.extractText(bitmap)
                                        isProcessing = false
                                        result.onSuccess { text ->
                                            val lines = PrescriptionParser.extractCandidateLines(text)
                                            if (lines.isEmpty()) {
                                                errorMessage = "No readable text found. Try again with better lighting."
                                            } else {
                                                capturedBitmap = bitmap
                                                fullExtractedText = text
                                                candidateLines = lines
                                            }
                                        }.onFailure {
                                            errorMessage = "OCR failed: ${it.message}"
                                        }
                                    }
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                isProcessing = false
                                errorMessage = "Capture failed: ${exception.message}"
                            }
                        }
                    )
                },
                enabled = !isProcessing,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp)
            ) {
                Text(
                    if (mode == ScanMode.BARCODE) "Scan Barcode" else "Capture Prescription",
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else if (mode == ScanMode.SEARCH) {
            Text(
                text = "Select the medicine name detected:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(candidateLines) { line ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onMedicineNameSelected(line) }
                    ) {
                        Text(
                            text = line,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            TextButton(
                onClick = { candidateLines = emptyList() },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Retake Photo", color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    Text(
                        text = "Select medicines to save:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(candidateLines) { line ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = selectedMedicines.contains(line),
                            onCheckedChange = { checked ->
                                if (checked) selectedMedicines.add(line)
                                else selectedMedicines.remove(line)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(text = line, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = doctorName,
                        onValueChange = { doctorName = it },
                        label = { Text("Doctor Name (optional)") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                OutlinedButton(
                    onClick = {
                        candidateLines = emptyList()
                        selectedMedicines.clear()
                        capturedBitmap = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Retake Photo")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val bitmap = capturedBitmap ?: return@Button
                        val token = tokenManager.getToken()
                        if (token == null) {
                            errorMessage = "Please log in to save to your vault."
                            return@Button
                        }

                        isSaving = true
                        errorMessage = null
                        scope.launch {
                            val stream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                            val imageBytes = stream.toByteArray()
                            val localImageId = prescriptionRepository.saveImageLocally(imageBytes)

                            val result = prescriptionRepository.createPrescription(
                                token = token,
                                localImageId = localImageId,
                                extractedText = fullExtractedText,
                                medicines = selectedMedicines.toList(),
                                doctorName = doctorName.ifBlank { null },
                                notes = notes.ifBlank { null }
                            )
                            isSaving = false
                            result.onSuccess {
                                onSavedToVault()
                            }.onFailure {
                                errorMessage = "Failed to save: ${it.message}"
                                prescriptionRepository.deleteImageLocally(localImageId)
                            }
                        }
                    },
                    enabled = !isSaving && selectedMedicines.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save to Vault", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun imageProxyToBitmap(image: androidx.camera.core.ImageProxy): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

private suspend fun scanBarcode(bitmap: Bitmap): Result<String> =
    suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        val scanner = BarcodeScanning.getClient()
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val value = barcodes.firstOrNull()?.rawValue
                if (value != null) {
                    cont.resumeWith(Result.success(Result.success(value)))
                } else {
                    cont.resumeWith(Result.success(Result.failure(Exception("No barcode detected. Try again."))))
                }
            }
            .addOnFailureListener { e ->
                cont.resumeWith(Result.success(Result.failure(e)))
            }
    }