package com.medifind.app.ui.screens

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medifind.app.data.remote.UserProfileResponse
import com.medifind.app.data.repository.TokenManager
import com.medifind.app.ui.theme.Amber
import com.medifind.app.ui.theme.BottleGreen
import com.medifind.app.ui.theme.WarmPaper
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
    var showLogoutDialog by remember { mutableStateOf(false) }

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

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log out?") },
            text = { Text("You'll need to sign in again to see your reservations and reminders.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        tokenManager.clearAll()
                        onLogout()
                    }
                ) {
                    Text("Log out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Stay") }
            }
        )
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
                val profile = uiState.profile
                val avatarBitmap = pendingPreviewBitmap ?: decodeBase64ToBitmap(profile.profilePicture)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    ProfileHero(
                        profile = profile,
                        avatar = avatarBitmap,
                        isEditing = isEditing,
                        onAvatarClick = { imagePickerLauncher.launch("image/*") },
                        onEdit = { isEditing = true }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isEditing) {
                        // Profile completeness (based on saved values)
                        val missing = buildList {
                            if (profile.phone.isNullOrBlank()) add("phone")
                            if (profile.address.isNullOrBlank()) add("address")
                            if (profile.dob.isNullOrBlank()) add("date of birth")
                            if (profile.profilePicture.isNullOrBlank()) add("photo")
                        }
                        if (missing.isNotEmpty()) {
                            // name + email are always present, so 2 of 6 are done by default
                            val percent = (6 - missing.size) / 6f
                            ProfileCompletenessCard(percent = percent, missing = missing)
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        ProfileSectionLabel("Account details")
                        Spacer(modifier = Modifier.height(10.dp))

                        ProfileCard {
                            ProfileDetailRow(
                                icon = Icons.Default.Email,
                                label = "Email",
                                value = profile.email
                            )
                            ProfileRowDivider()
                            ProfileDetailRow(
                                icon = Icons.Default.Phone,
                                label = "Phone",
                                value = profile.phone,
                                onAdd = { isEditing = true }
                            )
                            ProfileRowDivider()
                            ProfileDetailRow(
                                icon = Icons.Default.Home,
                                label = "Address",
                                value = profile.address,
                                onAdd = { isEditing = true }
                            )
                            ProfileRowDivider()
                            ProfileDetailRow(
                                icon = Icons.Default.CalendarToday,
                                label = "Date of birth",
                                value = profile.dob?.takeIf { it.isNotBlank() }?.let {
                                    try {
                                        dateFormatterIso.parse(it)?.let { d -> dateFormatterDisplay.format(d) } ?: it
                                    } catch (_: Exception) { it }
                                },
                                onAdd = { isEditing = true }
                            )
                            ProfileRowDivider()
                            ProfileDetailRow(
                                icon = Icons.Default.Badge,
                                label = "Account type",
                                value = roleLabel(profile.role)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedButton(
                            onClick = { showLogoutDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Logout", fontWeight = FontWeight.Medium)
                        }
                    } else {
                        ProfileSectionLabel("Edit details")
                        Spacer(modifier = Modifier.height(10.dp))

                        ProfileCard {
                            Column(modifier = Modifier.padding(16.dp)) {
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
                        }

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
                                    resetFieldsFrom(profile)
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).height(50.dp)
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
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f).height(50.dp)
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

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// ---------- Pieces ----------

private fun roleLabel(role: String?): String = when (role) {
    "pharmacy" -> "Pharmacy Owner"
    "admin" -> "Administrator"
    else -> "Patient"
}

/** Bottle-green identity card: avatar with amber ring, name, email, role pill, edit button. */
@Composable
private fun ProfileHero(
    profile: UserProfileResponse,
    avatar: Bitmap?,
    isEditing: Boolean,
    onAvatarClick: () -> Unit,
    onEdit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(BottleGreen, Color(0xFF1B3128))))
    ) {
        Icon(
            imageVector = Icons.Default.MedicalServices,
            contentDescription = null,
            tint = WarmPaper.copy(alpha = 0.07f),
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-36).dp, y = 32.dp)
        )

        if (!isEditing) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .background(WarmPaper.copy(alpha = 0.14f), CircleShape)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit profile",
                    tint = WarmPaper,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .clip(CircleShape)
                        .border(3.dp, Amber, CircleShape)
                        .background(WarmPaper.copy(alpha = 0.12f))
                        .then(if (isEditing) Modifier.clickable(onClick = onAvatarClick) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatar != null) {
                        Image(
                            bitmap = avatar.asImageBitmap(),
                            contentDescription = "Profile picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().padding(3.dp).clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = profile.name.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.headlineLarge,
                            color = WarmPaper
                        )
                    }
                }
                if (isEditing) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Amber)
                            .clickable(onClick = onAvatarClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Change photo",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = profile.name,
                style = MaterialTheme.typography.headlineSmall,
                color = WarmPaper
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = profile.email,
                style = MaterialTheme.typography.bodyMedium,
                color = WarmPaper.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .border(1.dp, Amber, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 3.dp)
            ) {
                Text(
                    text = roleLabel(profile.role).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Amber,
                    letterSpacing = 1.2.sp
                )
            }
            if (isEditing) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Tap your photo to change it",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmPaper.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ProfileCompletenessCard(percent: Float, missing: List<String>) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PROFILE COMPLETENESS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Text(
                text = "${(percent * 100).toInt()}%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { percent },
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Add your ${joinNatural(missing)} to finish your profile.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun joinNatural(items: List<String>): String = when (items.size) {
    0 -> ""
    1 -> items[0]
    2 -> "${items[0]} and ${items[1]}"
    else -> items.dropLast(1).joinToString(", ") + " and " + items.last()
}

@Composable
private fun ProfileSectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.width(10.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun ProfileCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        content = content
    )
}

@Composable
private fun ProfileRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/** A ledger row. Empty values show a muted placeholder and, if [onAdd] is set, an "Add" action. */
@Composable
private fun ProfileDetailRow(
    icon: ImageVector,
    label: String,
    value: String?,
    onAdd: (() -> Unit)? = null
) {
    val isEmpty = value.isNullOrBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isEmpty && onAdd != null) Modifier.clickable(onClick = onAdd) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            if (isEmpty) {
                Text(
                    text = "Not set",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else {
                Text(
                    text = value.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        if (isEmpty && onAdd != null) {
            Text(
                text = "Add",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
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

// ---------- Image helpers (unchanged) ----------

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