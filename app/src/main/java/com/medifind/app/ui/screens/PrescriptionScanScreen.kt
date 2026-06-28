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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.medifind.app.data.repository.OcrHelper
import com.medifind.app.data.repository.PrescriptionParser
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionScanScreen(
    onMedicineNameSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val ocrHelper = remember { OcrHelper() }

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
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Scan Prescription",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

        if (!hasCameraPermission) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Camera permission is required to scan prescriptions.",
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Permission")
                    }
                }
            }
        } else if (candidateLines.isEmpty()) {
            // Camera preview
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
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
                        CircularProgressIndicator()
                    }
                }
            }

            errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
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
                                    val result = ocrHelper.extractText(bitmap)
                                    isProcessing = false
                                    result.onSuccess { text ->
                                        val lines = PrescriptionParser.extractCandidateLines(text)
                                        if (lines.isEmpty()) {
                                            errorMessage = "No readable text found. Try again with better lighting."
                                        } else {
                                            candidateLines = lines
                                        }
                                    }.onFailure {
                                        errorMessage = "OCR failed: ${it.message}"
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
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text("Capture Prescription")
            }
        } else {
            // Show detected lines for the user to pick from
            Text(
                text = "Select the medicine name detected:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                items(candidateLines) { line ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = { onMedicineNameSelected(line) }
                    ) {
                        Text(text = line, modifier = Modifier.padding(16.dp))
                    }
                }
            }
            TextButton(
                onClick = { candidateLines = emptyList() },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Retake Photo")
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