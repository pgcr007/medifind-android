package com.medifind.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medifind.app.data.remote.ReminderResponse
import com.medifind.app.ui.theme.Amber
import com.medifind.app.ui.theme.BottleGreen
import com.medifind.app.ui.theme.OnAmberLight
import com.medifind.app.ui.theme.PlexMono
import com.medifind.app.ui.theme.WarmPaper
import com.medifind.app.ui.viewmodel.ReminderUiState
import com.medifind.app.ui.viewmodel.ReminderViewModel
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(
    onAddReminder: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReminderViewModel = viewModel()
) {
    // Request POST_NOTIFICATIONS permission on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* proceed regardless of result — alarms still schedule, just no notification shown if denied */ }

        LaunchedEffect(Unit) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadReminders()
    }

    val state = viewModel.uiState
    var pendingDelete by remember { mutableStateOf<ReminderResponse?>(null) }

    pendingDelete?.let { reminder ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete reminder?") },
            text = { Text("${reminder.medicineId.name} will stop sending dose and refill reminders.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteReminder(reminder._id)
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Keep") }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MEDICINE CABINET",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.5.sp
                        )
                        Text("Reminders", style = MaterialTheme.typography.headlineSmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddReminder,
                containerColor = Amber,
                contentColor = OnAmberLight,
                shape = RoundedCornerShape(18.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New reminder", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (state) {
                is ReminderUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                        EmptyReminders(onAddReminder = onAddReminder)
                    } else {
                        val reminders = viewModel.reminders
                        val nextDose = remember(reminders) { computeNextDose(reminders) }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item(key = "summary") {
                                SummaryCard(reminders = reminders, nextDose = nextDose)
                            }
                            item(key = "label") {
                                Spacer(modifier = Modifier.height(4.dp))
                                ReminderSectionLabel("Your medicines")
                            }
                            items(reminders, key = { it._id }) { reminder ->
                                ReminderCard(
                                    reminder = reminder,
                                    onToggle = { viewModel.toggleActive(reminder._id, !reminder.isActive) },
                                    onDelete = { pendingDelete = reminder }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- Summary hero ----------

@Composable
private fun SummaryCard(reminders: List<ReminderResponse>, nextDose: NextDose?) {
    val active = reminders.filter { it.isActive }
    val dosesPerDay = active.sumOf { it.dosageTimes.size }
    val refillsSoon = active.count { (refillDaysLeft(it) ?: Int.MAX_VALUE) <= 3 }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(BottleGreen, Color(0xFF1B3128))))
    ) {
        Icon(
            imageVector = Icons.Default.Medication,
            contentDescription = null,
            tint = WarmPaper.copy(alpha = 0.07f),
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.TopEnd)
                .offset(x = 36.dp, y = (-20).dp)
        )

        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "NEXT DOSE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Amber,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (nextDose != null) {
                Text(
                    text = formatDoseTime(nextDose.time),
                    style = MaterialTheme.typography.displaySmall.copy(fontFamily = PlexMono),
                    fontWeight = FontWeight.Bold,
                    color = WarmPaper
                )
                Text(
                    text = "${nextDose.medicineName}  ·  ${formatMinutesUntil(nextDose.minutesUntil)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmPaper.copy(alpha = 0.85f)
                )
            } else {
                Text(
                    text = "No doses scheduled",
                    style = MaterialTheme.typography.headlineSmall,
                    color = WarmPaper
                )
                Text(
                    text = if (active.isEmpty()) "All your reminders are paused."
                    else "Add dose times to get a nudge for each dose.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmPaper.copy(alpha = 0.85f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = WarmPaper.copy(alpha = 0.18f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryStat(value = "${active.size}", label = "Active", modifier = Modifier.weight(1f))
                SummaryStat(value = "$dosesPerDay", label = "Doses a day", modifier = Modifier.weight(1f))
                SummaryStat(
                    value = "$refillsSoon",
                    label = "Refills soon",
                    highlight = refillsSoon > 0,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PlexMono),
            fontWeight = FontWeight.Bold,
            color = if (highlight) Amber else WarmPaper
        )
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = WarmPaper.copy(alpha = 0.7f),
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun ReminderSectionLabel(text: String) {
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

// ---------- Reminder card ----------

@Composable
fun ReminderCard(
    reminder: ReminderResponse,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val daysLeft = refillDaysLeft(reminder)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    ) {
        // Status spine: green when active, grey when paused
        Box(
            modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
                .background(
                    if (reminder.isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
                )
        )

        Column(modifier = Modifier.weight(1f).padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)) {
            // Title row (dimmed when paused)
            Row(
                modifier = Modifier.fillMaxWidth().alpha(if (reminder.isActive) 1f else 0.6f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = reminder.medicineId.name.take(1).uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.medicineId.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    reminder.medicineId.genericName?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlexMono),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete reminder",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                    )
                }
            }

            Column(modifier = Modifier.alpha(if (reminder.isActive) 1f else 0.6f)) {
                Spacer(modifier = Modifier.height(10.dp))

                // Dose times
                if (reminder.dosageTimes.isNotEmpty()) {
                    DoseTimeRow(times = reminder.dosageTimes.sorted())
                } else {
                    Text(
                        text = "No dose times. Refill alerts only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Refill cycle
                RefillMeter(daysLeft = daysLeft, interval = reminder.refillIntervalDays)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(isActive = reminder.isActive)
                Switch(
                    checked = reminder.isActive,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DoseTimeRow(times: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        times.forEach { time ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = doseIcon(time),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatDoseTime(time),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = PlexMono),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/** Refill progress: how much of the cycle is used, with a plain-language countdown. */
@Composable
private fun RefillMeter(daysLeft: Int?, interval: Int) {
    val used = if (daysLeft == null || interval <= 0) 0f
    else ((interval - daysLeft).toFloat() / interval).coerceIn(0f, 1f)

    val (text, color) = when {
        daysLeft == null -> "Refill every $interval days" to MaterialTheme.colorScheme.onSurfaceVariant
        daysLeft < 0 -> "Refill overdue by ${-daysLeft} ${if (-daysLeft == 1) "day" else "days"}" to MaterialTheme.colorScheme.error
        daysLeft == 0 -> "Refill due today" to MaterialTheme.colorScheme.error
        daysLeft <= 3 -> "Refill in $daysLeft ${if (daysLeft == 1) "day" else "days"}" to MaterialTheme.colorScheme.secondary
        else -> "Refill in $daysLeft days" to MaterialTheme.colorScheme.primary
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Text(
                text = "every $interval d",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = PlexMono),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { used },
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
        )
    }
}

@Composable
private fun StatusPill(isActive: Boolean) {
    val bg = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(bg))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isActive) "Active" else "Paused",
            style = MaterialTheme.typography.labelSmall,
            color = bg,
            fontWeight = FontWeight.Medium
        )
    }
}

// ---------- Empty state ----------

@Composable
private fun EmptyReminders(onAddReminder: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Medication,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(46.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Your cabinet is empty",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Add a reminder to get a nudge for every dose and a heads-up before you run out.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onAddReminder,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = OnAmberLight),
            modifier = Modifier.height(50.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add your first reminder", fontWeight = FontWeight.Bold)
        }
    }
}

// ---------- Helpers ----------

private data class NextDose(val medicineName: String, val time: String, val minutesUntil: Int)

/** The soonest upcoming dose across all active reminders, wrapping past midnight. */
private fun computeNextDose(reminders: List<ReminderResponse>): NextDose? {
    val now = LocalTime.now()
    val nowMinutes = now.hour * 60 + now.minute
    var best: NextDose? = null
    reminders.filter { it.isActive }.forEach { reminder ->
        reminder.dosageTimes.forEach { time ->
            val parts = time.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: return@forEach
            val m = parts.getOrNull(1)?.toIntOrNull() ?: return@forEach
            var diff = h * 60 + m - nowMinutes
            if (diff < 0) diff += 24 * 60
            val current = best
            if (current == null || diff < current.minutesUntil) {
                best = NextDose(reminder.medicineId.name, time, diff)
            }
        }
    }
    return best
}

/** Days until the next refill, or null if the stored date can't be read. */
private fun refillDaysLeft(reminder: ReminderResponse): Int? = try {
    val last = Instant.parse(reminder.lastRefillDate)
    val elapsed = Duration.between(last, Instant.now()).toDays().toInt()
    reminder.refillIntervalDays - elapsed
} catch (_: Exception) {
    null
}

private fun formatMinutesUntil(minutes: Int): String = when {
    minutes <= 0 -> "now"
    minutes < 60 -> "in $minutes min"
    minutes % 60 == 0 -> "in ${minutes / 60} h"
    else -> "in ${minutes / 60} h ${minutes % 60} min"
}

/** "14:30" -> "2:30 PM". Falls back to the raw string if it can't be parsed. */
private fun formatDoseTime(hhmm: String): String {
    val parts = hhmm.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: return hhmm
    val m = parts.getOrNull(1)?.toIntOrNull() ?: return hhmm
    val suffix = if (h < 12) "AM" else "PM"
    val h12 = when (val x = h % 12) { 0 -> 12; else -> x }
    return String.format(Locale.US, "%d:%02d %s", h12, m, suffix)
}

private fun doseIcon(hhmm: String): ImageVector {
    val hour = hhmm.substringBefore(":").toIntOrNull() ?: 0
    return when (hour) {
        in 5..11 -> Icons.Default.WbTwilight
        in 12..16 -> Icons.Default.WbSunny
        in 17..20 -> Icons.Default.WbTwilight
        else -> Icons.Default.NightsStay
    }
}