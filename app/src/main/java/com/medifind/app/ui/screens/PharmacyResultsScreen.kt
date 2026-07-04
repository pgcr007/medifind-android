package com.medifind.app.ui.screens

import android.Manifest
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medifind.app.data.remote.PharmacyNearbyResponse
import com.medifind.app.data.repository.LocationHelper
import com.medifind.app.ui.theme.PlexMono
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
                viewModel.loadNearbyPharmacies(19.0760, 72.8777, medicineId)
            }
        }
    }

    val state = viewModel.uiState
    var sortByPrice by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "Pharmacies with \"$medicineName\"",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !sortByPrice,
                onClick = { sortByPrice = false },
                label = { Text("Nearest") },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            FilterChip(
                selected = sortByPrice,
                onClick = { sortByPrice = true },
                label = { Text("Cheapest") },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onComparePricesClick,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text("Compare Prices Across Alternatives", color = MaterialTheme.colorScheme.secondary)
        }

        when (state) {
            is PharmacyResultsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                        Text(
                            text = "No nearby pharmacies currently stock this medicine.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (viewModel.isLoadingAlternatives) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary)
                        } else if (viewModel.alternatives.isNotEmpty()) {
                            Text(
                                text = "Generic Alternatives",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            viewModel.alternatives.forEach { alt ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = alt.name, style = MaterialTheme.typography.bodyMedium)
                                        alt.genericName?.let {
                                            Text(
                                                text = "Generic: $it",
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlexMono),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "These are suggested alternatives with the same active ingredient. Always consult a doctor or pharmacist before switching medicines.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
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

                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        factory = { ctx ->
                            Configuration.getInstance().load(ctx, android.preference.PreferenceManager.getDefaultSharedPreferences(ctx))
                            MapView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
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

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(sortedPharmacies) { pharmacy ->
                            PharmacyCard(
                                pharmacy = pharmacy,
                                isCheapest = pharmacy.price != null && pharmacy._id == cheapestId,
                                onClick = { onPharmacySelected(pharmacy) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun PharmacyCard(pharmacy: PharmacyNearbyResponse, isCheapest: Boolean, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pharmacy.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isCheapest) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text("Best Price", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = pharmacy.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${"%.1f".format(pharmacy.distanceKm)} km away",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                pharmacy.price?.let {
                    Text(
                        text = "₹$it",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PlexMono),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            pharmacy.stockQty?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "In stock: $it",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlexMono),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}