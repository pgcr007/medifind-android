package com.medifind.app.ui.screens

import android.Manifest
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medifind.app.data.remote.PharmacyNearbyResponse
import com.medifind.app.data.repository.LocationHelper
import com.medifind.app.ui.theme.Amber
import com.medifind.app.ui.theme.OnAmberLight
import com.medifind.app.ui.theme.PlexMono
import com.medifind.app.ui.viewmodel.PharmacyResultsUiState
import com.medifind.app.ui.viewmodel.PharmacyResultsViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

@androidx.annotation.RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
@Composable
fun PharmacyResultsScreen(
    medicineId: String,
    medicineName: String,
    onPharmacySelected: (PharmacyNearbyResponse) -> Unit,
    onComparePricesClick: () -> Unit,
    emergency: Boolean = false,
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
                viewModel.loadNearbyPharmacies(location.first, location.second, medicineId, emergency)
            } else {
                viewModel.loadNearbyPharmacies(19.0760, 72.8777, medicineId, emergency)
            }
        }
    }

    val state = viewModel.uiState
    // best = server order (distance + stock trust in emergency mode), nearest, cheapest
    var sortMode by remember { mutableStateOf(if (emergency) "best" else "nearest") }
    var showMap by remember { mutableStateOf(true) }

    val pharmacyCount = (state as? PharmacyResultsUiState.Success)?.pharmacies?.size
    val hasResults = (pharmacyCount ?: 0) > 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ResultsHeader(
            medicineName = medicineName,
            emergency = emergency,
            count = pharmacyCount,
            showMapToggle = hasResults,
            mapVisible = showMap,
            onToggleMap = { showMap = !showMap }
        )

        if (emergency) {
            Spacer(modifier = Modifier.height(12.dp))
            EmergencyBanner()
        }

        Spacer(modifier = Modifier.height(14.dp))

        val sortOptions = buildList {
            if (emergency) add("best" to "Best match")
            add("nearest" to "Nearest")
            add("cheapest" to "Cheapest")
        }
        SortSegments(
            options = sortOptions,
            selected = sortMode,
            onSelect = { sortMode = it },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))
        CompareBanner(onClick = onComparePricesClick, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(12.dp))

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
                    EmptyResults(emergency = emergency, viewModel = viewModel)
                } else {
                    val sortedPharmacies = when (sortMode) {
                        "cheapest" -> state.pharmacies.sortedBy { it.price?.toDouble() ?: Double.MAX_VALUE }
                        "nearest" -> state.pharmacies.sortedBy { it.distanceKm }
                        else -> state.pharmacies // server order: "best" in emergency mode
                    }
                    val cheapestId = state.pharmacies
                        .filter { it.price != null }
                        .minByOrNull { it.price!! }
                        ?._id
                    val nearestId = state.pharmacies.minByOrNull { it.distanceKm }?._id

                    AnimatedVisibility(visible = showMap) {
                        Column {
                            MapCard(
                                pharmacies = sortedPharmacies,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sortedPharmacies, key = { it._id }) { pharmacy ->
                            PharmacyCard(
                                pharmacy = pharmacy,
                                isCheapest = pharmacy.price != null && pharmacy._id == cheapestId,
                                isNearest = pharmacy._id == nearestId,
                                emergency = emergency,
                                onClick = { onPharmacySelected(pharmacy) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------- Header & controls ----------

@Composable
private fun ResultsHeader(
    medicineName: String,
    emergency: Boolean,
    count: Int?,
    showMapToggle: Boolean,
    mapVisible: Boolean,
    onToggleMap: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (emergency) "EMERGENCY SEARCH" else "NEARBY PHARMACIES",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (emergency) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = medicineName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (count != null) {
                Text(
                    text = when (count) {
                        0 -> "No pharmacies found"
                        1 -> "1 pharmacy has it in stock"
                        else -> "$count pharmacies have it in stock"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showMapToggle) {
            FilledTonalIconToggleButton(
                checked = mapVisible,
                onCheckedChange = { onToggleMap() },
                colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Default.Map, contentDescription = if (mapVisible) "Hide map" else "Show map")
            }
        }
    }
}

@Composable
private fun EmergencyBanner() {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.LocalHospital,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Showing 24-hour pharmacies only, ranked by distance and how recently their stock was confirmed. Call ahead when a pharmacy is marked.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

/** Pill-style segmented control for the sort options. */
@Composable
private fun SortSegments(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (key, label) ->
            val isSelected = key == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(key) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CompareBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.width(6.dp).fillMaxHeight().background(Amber))
        Row(
            modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Sell, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Compare prices across alternatives",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "See cheaper generics with the same ingredient",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MapCard(pharmacies: List<PharmacyNearbyResponse>, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                Configuration.getInstance().load(ctx, android.preference.PreferenceManager.getDefaultSharedPreferences(ctx))
                MapView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                    controller.setZoom(13.0)
                    val first = pharmacies.first()
                    controller.setCenter(GeoPoint(first.latitude, first.longitude))

                    pharmacies.forEach { pharmacy ->
                        val marker = Marker(this)
                        marker.position = GeoPoint(pharmacy.latitude, pharmacy.longitude)
                        marker.title = pharmacy.name
                        overlays.add(marker)
                    }
                }
            }
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = "${pharmacies.size} on map",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ---------- Empty state ----------

@Composable
private fun EmptyResults(emergency: Boolean, viewModel: PharmacyResultsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = if (emergency) {
                "No 24-hour pharmacy currently lists this medicine. Check the alternatives below, or turn off Emergency mode to see all pharmacies."
            } else {
                "No nearby pharmacies currently stock this medicine."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.isLoadingAlternatives) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary)
        } else if (viewModel.alternatives.isNotEmpty()) {
            Text(
                text = "GENERIC ALTERNATIVES",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            viewModel.alternatives.forEach { alt ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Monogram(alt.name)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = alt.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
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
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(12.dp)
            ) {
                Text(
                    text = "These are suggested alternatives with the same active ingredient. Always consult a doctor or pharmacist before switching medicines.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

// ---------- Pharmacy card ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PharmacyCard(
    pharmacy: PharmacyNearbyResponse,
    isCheapest: Boolean,
    onClick: () -> Unit,
    emergency: Boolean = false,
    isNearest: Boolean = false
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        // Top: badge, name + pills, price
        Row(verticalAlignment = Alignment.Top) {
            if (emergency && pharmacy.emergencyRank != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${pharmacy.emergencyRank}",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = PlexMono),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            } else {
                Monogram(pharmacy.name)
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pharmacy.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = pharmacy.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val hasPills = isCheapest || isNearest || pharmacy.is24Hours == true || pharmacy.verified
                if (hasPills) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isCheapest) Pill("Best price", Amber, OnAmberLight)
                        if (isNearest) Pill(
                            "Nearest",
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (pharmacy.is24Hours == true) Pill(
                            "24 hrs",
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        if (pharmacy.verified) Pill(
                            "Verified",
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            icon = Icons.Default.Verified
                        )
                    }
                }
            }

            pharmacy.price?.let { price ->
                Spacer(modifier = Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹$price",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = PlexMono),
                        fontWeight = FontWeight.Bold,
                        color = if (isCheapest) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(12.dp))

        // Stats
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell(
                icon = Icons.Default.Place,
                label = "Distance",
                value = "${"%.1f".format(pharmacy.distanceKm)} km",
                modifier = Modifier.weight(1f)
            )
            StatCell(
                icon = Icons.Default.Inventory2,
                label = "In stock",
                value = pharmacy.stockQty?.let { "$it units" } ?: "N/A",
                modifier = Modifier.weight(1f)
            )
            StatCell(
                icon = Icons.Default.Star,
                label = "Rating",
                value = if ((pharmacy.reviewCount ?: 0) > 0 && pharmacy.averageRating != null)
                    "${"%.1f".format(pharmacy.averageRating)} (${pharmacy.reviewCount})"
                else "No reviews",
                iconTint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }

        TrustStrip(pharmacy = pharmacy, modifier = Modifier.padding(top = 12.dp))

        val phone = pharmacy.phone
        if (emergency && !phone.isNullOrBlank()) {
            val context = LocalContext.current
            Spacer(modifier = Modifier.height(12.dp))
            FilledTonalButton(
                onClick = { dialPharmacy(context, phone) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (pharmacy.callAhead == true) "Call to confirm stock" else "Call pharmacy",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun Monogram(name: String) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(1).uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun Pill(
    text: String,
    container: Color,
    content: Color,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = content
        )
    }
}

@Composable
private fun StatCell(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PlexMono),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Compact freshness + confidence panel. Renders nothing when the backend sent no trust fields. */
@Composable
private fun TrustStrip(pharmacy: PharmacyNearbyResponse, modifier: Modifier = Modifier) {
    val freshness = pharmacy.freshness ?: return

    val (label, color) = when (freshness) {
        "fresh" -> "Fresh" to MaterialTheme.colorScheme.primary
        "recent" -> "Recent" to MaterialTheme.colorScheme.onSurfaceVariant
        "stale" -> "Stale" to MaterialTheme.colorScheme.secondary
        else -> "Outdated" to MaterialTheme.colorScheme.error
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$label · confirmed ${formatStockAge(pharmacy.ageHours)}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlexMono),
                color = color,
                fontWeight = FontWeight.Medium
            )
        }

        pharmacy.stockConfidence?.let { confidence ->
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "CONFIDENCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                LinearProgressIndicator(
                    progress = { (confidence.coerceIn(0, 100)) / 100f },
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "$confidence%",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = PlexMono),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (pharmacy.disputed == true) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Some patients reported this as missing",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (pharmacy.callAhead == true) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Call ahead to confirm before travelling",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}