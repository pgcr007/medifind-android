package com.medifind.app.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medifind.app.data.remote.ReservationDetailResponse
import com.medifind.app.ui.viewmodel.MyReservationsUiState
import com.medifind.app.ui.viewmodel.ReservationViewModel

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
                        Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("You haven't reserved any medicines yet.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            viewModel.stockReportError?.let { message ->
                                item {
                                    Text(
                                        text = message,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            items(state.reservations) { reservation ->
                                ReservationCard(
                                    reservation = reservation,
                                    reportedInStock = viewModel.stockReports[reservation._id],
                                    onReport = { inStock -> viewModel.submitStockReport(reservation, inStock) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReservationCard(
    reservation: ReservationDetailResponse,
    reportedInStock: Boolean?,
    onReport: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reservation.medicineId.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(status = reservation.status)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = reservation.pharmacyId.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = reservation.pharmacyId.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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

@Composable
private fun StatusBadge(status: String) {
    val (bg, label) = when (status) {
        "confirmed" -> Color(0xFF2E7D32) to "Confirmed"
        "pending" -> Color(0xFFC08A34) to "Pending"
        "rejected" -> Color(0xFFA83B32) to "Rejected"
        "cancelled" -> Color(0xFF757575) to "Cancelled"
        else -> Color.Gray to status.replaceFirstChar { it.uppercase() }
    }
    Box(
        modifier = Modifier
            .background(color = bg, shape = RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}