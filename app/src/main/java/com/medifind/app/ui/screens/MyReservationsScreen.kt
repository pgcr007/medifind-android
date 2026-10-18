package com.medifind.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medifind.app.data.remote.ReservationDetailResponse
import com.medifind.app.ui.theme.StatusCancelledGray
import com.medifind.app.ui.theme.StatusConfirmedGreen
import com.medifind.app.ui.theme.StatusPendingAmber
import com.medifind.app.ui.theme.StatusRejectedRust
import com.medifind.app.ui.viewmodel.MyReservationsUiState
import com.medifind.app.ui.viewmodel.ReservationViewModel
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReservationsScreen(
    modifier: Modifier = Modifier,
    viewModel: ReservationViewModel = viewModel()
) {
    val state = viewModel.myReservationsState

    LaunchedEffect(Unit) {
        viewModel.loadMyReservations()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("My Reservations") }) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (state) {
                is MyReservationsUiState.Idle, is MyReservationsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is MyReservationsUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is MyReservationsUiState.Success -> {
                    if (state.reservations.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Your ledger is empty",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Medicines you reserve will be recorded here with the date and time.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Backend already returns newest first, so groupBy keeps that order.
                        val grouped = state.reservations.groupBy {
                            dayGroupLabel(parseReservedAt(it.createdAt))
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            viewModel.stockReportError?.let { message ->
                                item(key = "stock-report-error") {
                                    Text(
                                        text = message,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            grouped.forEach { (label, list) ->
                                item(key = "header-$label") { DayHeader(label) }
                                items(list, key = { it._id }) { reservation ->
                                    ReservationCard(
                                        reservation = reservation,
                                        reportedInStock = viewModel.stockReports[reservation._id],
                                        onReport = { inStock ->
                                            viewModel.submitStockReport(reservation, inStock)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- Date / time helpers ----------

private data class ReservedAt(val instant: Instant, val local: ZonedDateTime) {
    private fun fmt(pattern: String): String =
        local.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))

    val weekdayShort: String get() = fmt("EEE").uppercase(Locale.getDefault())
    val day: String get() = local.dayOfMonth.toString()
    val monthShort: String get() = fmt("MMM").uppercase(Locale.getDefault())
    val fullDate: String get() = fmt("EEE, d MMM yyyy")
    val time: String get() = fmt("h:mm a")

    val relative: String
        get() {
            val d = Duration.between(instant, Instant.now())
            val minutes = d.toMinutes()
            val hours = d.toHours()
            val days = d.toDays()
            return when {
                minutes < 1 -> "Just now"
                minutes < 60 -> "$minutes min ago"
                hours < 24 -> "$hours hr ago"
                days == 1L -> "1 day ago"
                days < 7 -> "$days days ago"
                else -> ""
            }
        }
}

private fun parseReservedAt(iso: String?): ReservedAt? {
    if (iso.isNullOrBlank()) return null
    return try {
        val instant = Instant.parse(iso)
        ReservedAt(instant, instant.atZone(ZoneId.systemDefault()))
    } catch (e: Exception) {
        null
    }
}

private fun dayGroupLabel(reservedAt: ReservedAt?): String {
    if (reservedAt == null) return "Earlier"
    val date = reservedAt.local.toLocalDate()
    val today = LocalDate.now()
    return when {
        date == today -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))
    }
}

private fun statusColor(status: String): Color = when (status) {
    "confirmed" -> StatusConfirmedGreen
    "pending" -> StatusPendingAmber
    "rejected" -> StatusRejectedRust
    "cancelled" -> StatusCancelledGray
    else -> Color.Gray
}

// ---------- UI pieces ----------

@Composable
private fun DayHeader(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun ReservationCard(
    reservation: ReservationDetailResponse,
    reportedInStock: Boolean?,
    onReport: (Boolean) -> Unit
) {
    val reservedAt = parseReservedAt(reservation.createdAt)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Status-coloured spine on the left edge, like a ledger tab
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(statusColor(reservation.status))
            )

            Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    if (reservedAt != null) {
                        DateTab(reservedAt)
                        Spacer(modifier = Modifier.width(14.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = reservation.medicineId.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusBadge(status = reservation.status)
                        }
                        reservation.medicineId.genericName?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = reservation.pharmacyId.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = reservation.pharmacyId.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (reservedAt != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ReservedStamp(reservedAt)
                }

                // After the pharmacy confirms, ask the patient whether the medicine was really
                // there. These answers feed the reliability score other patients see.
                if (reservation.status == "confirmed") {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(10.dp))
                    if (reportedInStock == null) {
                        Text(
                            text = "Was it in stock when you went?",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onReport(true) }) { Text("Yes") }
                            OutlinedButton(onClick = { onReport(false) }) { Text("No") }
                        }
                    } else {
                        Text(
                            text = "Thanks. You reported: ${if (reportedInStock) "in stock" else "not in stock"}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { onReport(!reportedInStock) }) { Text("Change answer") }
                    }
                }
            }
        }
    }
}

/** Calendar-leaf style block: weekday, big day number, month. */
@Composable
private fun DateTab(reservedAt: ReservedAt) {
    Column(
        modifier = Modifier
            .width(54.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = reservedAt.weekdayShort,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
        )
        Text(
            text = reservedAt.day,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = reservedAt.monthShort,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
        )
    }
}

/** "Reserved on" strip showing the full date, exact time and how long ago. */
@Composable
private fun ReservedStamp(reservedAt: ReservedAt) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "RESERVED ON",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = reservedAt.fullDate,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = reservedAt.time,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (reservedAt.relative.isNotEmpty()) {
                Text(
                    text = reservedAt.relative,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val bg = statusColor(status)
    val label = when (status) {
        "confirmed" -> "Confirmed"
        "pending" -> "Pending"
        "rejected" -> "Rejected"
        "cancelled" -> "Cancelled"
        else -> status.replaceFirstChar { it.uppercase() }
    }
    Box(
        modifier = Modifier
            .background(color = bg, shape = RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}