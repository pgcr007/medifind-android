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
import kotlinx.coroutines.launch
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
    modifier: Modifier = Modifier,
    viewModel: PharmacyResultsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Pharmacies with \"$medicineName\"",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

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
                    Text(
                        text = "No nearby pharmacies currently stock this medicine.",
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
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
                                val first = state.pharmacies.first()
                                controller.setCenter(GeoPoint(first.latitude, first.longitude))

                                state.pharmacies.forEach { pharmacy ->
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
                        items(state.pharmacies) { pharmacy ->
                            PharmacyCard(pharmacy = pharmacy, onClick = { onPharmacySelected(pharmacy) })
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PharmacyCard(pharmacy: PharmacyNearbyResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = pharmacy.name, style = MaterialTheme.typography.titleMedium)
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