package com.medifind.app.ui.screens

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medifind.app.data.repository.LocationHelper
import com.medifind.app.ui.theme.PlexMono
import com.medifind.app.ui.viewmodel.PriceComparisonUiState
import com.medifind.app.ui.viewmodel.PriceComparisonViewModel

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "Compare Prices",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )
        Text(
            text = "\"$medicineName\" and its generic alternatives — cheapest nearby price for each",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        when (state) {
            is PriceComparisonUiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is PriceComparisonUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            is PriceComparisonUiState.Success -> {
                if (state.rows.isEmpty()) {
                    Text(
                        text = "No comparison data available.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(horizontal = 16.dp).weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.rows) { row ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (row.isOriginal)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = row.medicineName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (row.isOriginal)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        )
                                        if (row.isOriginal) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            AssistChip(
                                                onClick = {},
                                                label = { Text("Your search", style = MaterialTheme.typography.labelSmall) },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    labelColor = MaterialTheme.colorScheme.onPrimary
                                                )
                                            )
                                        }
                                    }
                                    row.genericName?.let {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Generic: $it",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlexMono),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    if (row.bestPrice != null) {
                                        Text(
                                            text = "₹${row.bestPrice} at ${row.pharmacyName ?: "nearby pharmacy"}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PlexMono),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        row.distanceKm?.let {
                                            Text(
                                                text = "${"%.1f".format(it)} km away",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Not currently available nearby",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text(
                            text = "Alternatives share the same active ingredient. Always consult a doctor or pharmacist before switching medicines.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}