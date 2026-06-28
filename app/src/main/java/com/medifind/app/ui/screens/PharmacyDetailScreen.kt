package com.medifind.app.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medifind.app.data.remote.PharmacyNearbyResponse
import com.medifind.app.ui.viewmodel.ReservationUiState
import com.medifind.app.ui.viewmodel.ReservationViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun PharmacyDetailScreen(
    pharmacy: PharmacyNearbyResponse,
    medicineId: String,
    medicineName: String,
    onReservationComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReservationViewModel = viewModel()
) {
    val state = viewModel.uiState

    LaunchedEffect(state) {
        if (state is ReservationUiState.Success) {
            onReservationComplete()
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = pharmacy.name, style = MaterialTheme.typography.headlineSmall)
        Text(text = pharmacy.address, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "${"%.1f".format(pharmacy.distanceKm)} km away", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(16.dp))

        AndroidView(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            factory = { ctx ->
                Configuration.getInstance().load(ctx, android.preference.PreferenceManager.getDefaultSharedPreferences(ctx))
                MapView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(16.0)
                    val point = GeoPoint(pharmacy.latitude, pharmacy.longitude)
                    controller.setCenter(point)
                    val marker = Marker(this)
                    marker.position = point
                    marker.title = pharmacy.name
                    overlays.add(marker)
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = medicineName, style = MaterialTheme.typography.titleMedium)
                pharmacy.stockQty?.let {
                    Text(
                        text = if (it > 0) "In stock: $it units" else "Out of stock",
                        color = if (it > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
                pharmacy.price?.let {
                    Text(text = "Price: ₹$it", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (state is ReservationUiState.Error) {
            Text(text = state.message, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = { viewModel.reserve(pharmacy._id, medicineId) },
            enabled = state !is ReservationUiState.Loading && (pharmacy.stockQty ?: 0) > 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state is ReservationUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Reserve This Medicine")
            }
        }
    }
}