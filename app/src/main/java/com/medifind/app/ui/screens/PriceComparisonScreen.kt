package com.medifind.app.ui.screens

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medifind.app.data.repository.LocationHelper
import com.medifind.app.ui.viewmodel.PriceComparisonUiState
import com.medifind.app.ui.viewmodel.PriceComparisonViewModel
import androidx.compose.ui.unit.dp

@androidx.annotation.RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
@Composable
fun PriceComparisonScreen(
    medicineId: String,
    medicineName: String,
    modifier: Modifier = Modifier,
    viewModel: PriceComparisonViewModel = viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val locationHelper = LocationHelper(context)
        val location = locationHelper.getCurrentLocation()
        if (location != null) {
            viewModel.loadComparison(location.first, location.second, medicineId, medicineName)
        } else {
            viewModel.loadComparison(19.0760, 72.8777, medicineId, medicineName)
        }
    }

    val state = viewModel.uiState

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Compare Prices",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        Text(
            text = "\"$medicineName\" and its generic alternatives — cheapest nearby price for each",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        when (state) {
            is PriceComparisonUiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is PriceComparisonUiState.Error -> {
                Text(text = state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            is PriceComparisonUiState.Success -> {
                if (state.rows.isEmpty()) {
                    Text(text = "No comparison data available.", modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                        items(state.rows) { row ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = row.medicineName, style = MaterialTheme.typography.titleSmall)
                                        if (row.isOriginal) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            AssistChip(onClick = {}, label = { Text("Your search", style = MaterialTheme.typography.labelSmall) })
                                        }
                                    }
                                    row.genericName?.let {
                                        Text(text = "Generic: $it", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (row.bestPrice != null) {
                                        Text(text = "₹${row.bestPrice} at ${row.pharmacyName ?: "nearby pharmacy"}", style = MaterialTheme.typography.bodyMedium)
                                        row.distanceKm?.let {
                                            Text(text = "${"%.1f".format(it)} km away", style = MaterialTheme.typography.bodySmall)
                                        }
                                    } else {
                                        Text(text = "Not currently available nearby", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "⚠️ Alternatives share the same active ingredient. Always consult a doctor or pharmacist before switching medicines.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}