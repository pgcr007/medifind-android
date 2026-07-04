package com.medifind.app.ui.screens

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medifind.app.data.remote.UserProfileResponse
import com.medifind.app.data.repository.TokenManager
import com.medifind.app.ui.viewmodel.ProfileSaveState
import com.medifind.app.ui.viewmodel.ProfileUiState
import com.medifind.app.ui.viewmodel.ProfileViewModel
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    val uiState = viewModel.uiState
    val saveState = viewModel.saveState

    var isEditing by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var dobIso by remember { mutableStateOf<String?>(null) }
    var pendingImageBase64 by remember { mutableStateOf<String?>(null) }
    var pendingPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    fun resetFieldsFrom(profile: UserProfileResponse) {
        name = profile.name
        phone = profile.phone ?: ""
        address = profile.address ?: ""
        dobIso = profile.dob
        pendingImageBase64 = null
        pendingPreviewBitmap = null
    }

    LaunchedEffect(uiState) {
        if (uiState is ProfileUiState.Success && !isEditing) {
            resetFieldsFrom(uiState.profile)
        }
    }

    LaunchedEffect(saveState) {
        if (saveState is ProfileSaveState.Saved) {
            isEditing = false
            viewModel.resetSaveState()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val original = BitmapFactory.decodeStream(stream)
                if (original != null) {
                    val resized = resizeBitmap(original, maxDimension = 300)
                    pendingPreviewBitmap = resized
                    pendingImageBase64 = bitmapToBase64(resized)
                }
            }
        }
    }

    val dateFormatterDisplay = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val dateFormatterIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    fun showDatePicker() {
        val calendar = Calendar.getInstance()
        dobIso?.let {
            try {
                dateFormatterIso.parse(it)?.let { parsed -> calendar.time = parsed }
            } catch (_: Exception) {}
        }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val picked = Calendar.getInstance()
                picked.set(year, month, day)
                dobIso = dateFormatterIso.format(picked.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (uiState) {
            is ProfileUiState.Loading, is ProfileUiState.Idle -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is ProfileUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is ProfileUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Box(contentAlignment = Alignment.BottomEnd) {
                        val avatarBitmap = pendingPreviewBitmap ?: decodeBase64ToBitmap(uiState.profile.profilePicture)
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .then(
                                    if (isEditing) Modifier.clickable { imagePickerLauncher.launch("image/*") }
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarBitmap != null) {
                                Image(
                                    bitmap = avatarBitmap.asImageBitmap(),
                                    contentDescription = "Profile picture",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Text(
                                    text = uiState.profile.name.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        if (isEditing) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary)
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PhotoCamera,
                                    contentDescription = "Change photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isEditing) {
                        Text(
                            text = uiState.profile.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.profile.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Account Details",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!isEditing) {
                                    IconButton(onClick = { isEditing = true }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit profile",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            if (isEditing) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    EditField(label = "Name", value = name, onValueChange = { name = it }, icon = Icons.Default.Badge)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    EditField(label = "Phone", value = phone, onValueChange = { phone = it }, icon = Icons.Default.Phone)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    EditField(label = "Address", value = address, onValueChange = { address = it }, icon = Icons.Default.Home)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    val dobDisplay = dobIso?.let {
                                        try {
                                            dateFormatterIso.parse(it)?.let { d -> dateFormatterDisplay.format(d) }
                                        } catch (_: Exception) { it }
                                    } ?: ""

                                    Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker() }) {
                                        OutlinedTextField(
                                            value = dobDisplay,
                                            onValueChange = {},
                                            enabled = false,
                                            label = { Text("Date of Birth") },
                                            leadingIcon = {
                                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                                disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            } else {
                                DetailRow(icon = Icons.Default.Email, label = "Email", value = uiState.profile.email)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                DetailRow(
                                    icon = Icons.Default.Phone,
                                    label = "Phone",
                                    value = uiState.profile.phone?.takeIf { it.isNotBlank() } ?: "Not set"
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                DetailRow(
                                    icon = Icons.Default.Home,
                                    label = "Address",
                                    value = uiState.profile.address?.takeIf { it.isNotBlank() } ?: "Not set"
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                DetailRow(
                                    icon = Icons.Default.CalendarToday,
                                    label = "Date of Birth",
                                    value = uiState.profile.dob?.let {
                                        try {
                                            dateFormatterIso.parse(it)?.let { d -> dateFormatterDisplay.format(d) } ?: it
                                        } catch (_: Exception) { it }
                                    } ?: "Not set"
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                DetailRow(
                                    icon = Icons.Default.Badge,
                                    label = "Account Type",
                                    value = when (uiState.profile.role) {
                                        "pharmacy" -> "Pharmacy Owner"
                                        "admin" -> "Administrator"
                                        else -> "Patient"
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }

                    if (isEditing) {
                        Spacer(modifier = Modifier.height(16.dp))

                        if (saveState is ProfileSaveState.Error) {
                            Text(
                                text = saveState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    isEditing = false
                                    resetFieldsFrom(uiState.profile)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = {
                                    viewModel.saveProfile(
                                        name = name,
                                        phone = phone,
                                        address = address,
                                        dob = dobIso,
                                        profilePictureBase64 = pendingImageBase64
                                    )
                                },
                                enabled = saveState !is ProfileSaveState.Saving && name.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                if (saveState is ProfileSaveState.Saving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Save", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (!isEditing) {
                        OutlinedButton(
                            onClick = {
                                tokenManager.clearAll()
                                onLogout()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Logout", fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun EditField(label: String, value: String, onValueChange: (String) -> Unit, icon: ImageVector) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= maxDimension && height <= maxDimension) return bitmap

    val ratio = width.toFloat() / height.toFloat()
    val (newWidth, newHeight) = if (width > height) {
        maxDimension to (maxDimension / ratio).toInt()
    } else {
        (maxDimension * ratio).toInt() to maxDimension
    }
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}

private fun bitmapToBase64(bitmap: Bitmap): String {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
}

private fun decodeBase64ToBitmap(base64: String?): Bitmap? {
    if (base64.isNullOrBlank()) return null
    return try {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    }
}