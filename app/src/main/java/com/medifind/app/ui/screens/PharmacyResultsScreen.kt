package com.medifind.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medifind.app.data.remote.PharmacyNearbyResponse
import com.medifind.app.data.repository.LocationHelper
import com.medifind.app.ui.viewmodel.PharmacyResultsUiState
import com.medifind.app.ui.viewmodel.PharmacyResultsViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@androidx.annotation.RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
@Composable
fun PharmacyResultsScreen(
    medicineId: String,
    medicineName: String,
    onPharmacySelected: (PharmacyNearbyResponse) -> Unit,
    onComparePricesClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PharmacyResultsViewModel = viewModel()
) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val locationHelper = LocationHelper(context)
            val location = locationHelper.getCurrentLocation()
            if (location != null) {
                viewModel.loadNearbyPharmacies(location.first, location.second, medicineId)
            } else {
                // Fallback: Mumbai coordinates if location unavailable (e.g., emulator with no location set)
                viewModel.loadNearbyPharmacies(19.0760, 72.8777, medicineId)
            }
        }
    }

    val state = viewModel.uiState
    var sortByPrice by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Pharmacies with \"$medicineName\"",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !sortByPrice,
                onClick = { sortByPrice = false },
                label = { Text("Nearest") }
            )
            FilterChip(
                selected = sortByPrice,
                onClick = { sortByPrice = true },
                label = { Text("Cheapest") }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onComparePricesClick,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text("Compare Prices Across Alternatives")
        }

        when (state) {
            is PharmacyResultsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is PharmacyResultsUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            is PharmacyResultsUiState.Success -> {
                if (state.pharmacies.isEmpty()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "No nearby pharmacies currently stock this medicine.")
                        Spacer(modifier = Modifier.height(16.dp))

                        if (viewModel.isLoadingAlternatives) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else if (viewModel.alternatives.isNotEmpty()) {
                            Text(
                                text = "Generic Alternatives",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            viewModel.alternatives.forEach { alt ->
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = alt.name, style = MaterialTheme.typography.bodyMedium)
                                        alt.genericName?.let {
                                            Text(text = "Generic: $it", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "⚠️ These are suggested alternatives with the same active ingredient. Always consult a doctor or pharmacist before switching medicines.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    val sortedPharmacies = if (sortByPrice) {
                        state.pharmacies.sortedBy { it.price?.toDouble() ?: Double.MAX_VALUE }
                    } else {
                        state.pharmacies
                    }
                    val cheapestId = state.pharmacies
                        .filter { it.price != null }
                        .minByOrNull { it.price!! }
                        ?._id

                    // Map showing all pharmacy markers
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 16.dp),
                        factory = { ctx ->
                            Configuration.getInstance().load(ctx, android.preference.PreferenceManager.getDefaultSharedPreferences(ctx))
                            MapView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(13.0)
                                val first = sortedPharmacies.first()
                                controller.setCenter(GeoPoint(first.latitude, first.longitude))

                                sortedPharmacies.forEach { pharmacy ->
                                    val marker = Marker(this)
                                    marker.position = GeoPoint(pharmacy.latitude, pharmacy.longitude)
                                    marker.title = pharmacy.name
                                    overlays.add(marker)
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                        items(sortedPharmacies) { pharmacy ->
                            PharmacyCard(
                                pharmacy = pharmacy,
                                isCheapest = pharmacy.price != null && pharmacy._id == cheapestId,
                                onClick = { onPharmacySelected(pharmacy) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PharmacyCard(pharmacy: PharmacyNearbyResponse, isCheapest: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = pharmacy.name, style = MaterialTheme.typography.titleMedium)
                if (isCheapest) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text("Best Price", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            Text(text = pharmacy.address, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = "${"%.1f".format(pharmacy.distanceKm)} km away", style = MaterialTheme.typography.bodySmall)
                pharmacy.price?.let {
                    Text(text = "₹$it", style = MaterialTheme.typography.bodyMedium)
                }
            }
            pharmacy.stockQty?.let {
                Text(text = "In stock: $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}