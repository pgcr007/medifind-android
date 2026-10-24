package com.medifind.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medifind.app.data.remote.PharmacyNearbyResponse
import com.medifind.app.data.remote.ReviewResponse
import com.medifind.app.ui.theme.Amber
import com.medifind.app.ui.theme.BottleGreen
import com.medifind.app.ui.theme.OnAmberLight
import com.medifind.app.ui.theme.PlexMono
import com.medifind.app.ui.theme.WarmPaper
import com.medifind.app.ui.viewmodel.ReservationUiState
import com.medifind.app.ui.viewmodel.ReservationViewModel
import com.medifind.app.ui.viewmodel.ReviewViewModel
import com.medifind.app.ui.viewmodel.ReviewsUiState
import com.medifind.app.ui.viewmodel.SubmitReviewUiState
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PharmacyDetailScreen(
    pharmacy: PharmacyNearbyResponse,
    medicineId: String,
    medicineName: String,
    onReservationComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReservationViewModel = viewModel(),
    reviewViewModel: ReviewViewModel = viewModel()
) {
    val state = viewModel.uiState
    val context = LocalContext.current

    LaunchedEffect(state) {
        if (state is ReservationUiState.Success) {
            onReservationComplete()
        }
    }

    LaunchedEffect(pharmacy._id) {
        reviewViewModel.loadReviews(pharmacy._id)
    }

    val inStock = (pharmacy.stockQty ?: 0) > 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
        ) {
            DetailHero(pharmacy = pharmacy)

            Spacer(modifier = Modifier.height(14.dp))

            DetailMap(pharmacy = pharmacy, onDirections = {
                openDirections(context, pharmacy.latitude, pharmacy.longitude, pharmacy.name)
            })

            Spacer(modifier = Modifier.height(16.dp))

            MedicineCard(
                pharmacy = pharmacy,
                medicineName = medicineName,
                inStock = inStock
            )

            val phone = pharmacy.phone
            if (!phone.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { dialPharmacy(context, phone) },
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (pharmacy.callAhead == true) "Call to confirm stock first" else "Call pharmacy",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            DetailSectionLabel("Ratings & reviews")
            Spacer(modifier = Modifier.height(14.dp))

            RatingSummary(
                average = pharmacy.averageRating,
                count = pharmacy.reviewCount ?: 0
            )

            Spacer(modifier = Modifier.height(14.dp))

            ReviewFormSection(pharmacyId = pharmacy._id, reviewViewModel = reviewViewModel)

            Spacer(modifier = Modifier.height(20.dp))

            when (val reviewsState = reviewViewModel.reviewsState) {
                is ReviewsUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is ReviewsUiState.Error -> {
                    Text(
                        text = reviewsState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                is ReviewsUiState.Success -> {
                    if (reviewsState.reviews.isEmpty()) {
                        Text(
                            text = "No reviews yet. Be the first to review this pharmacy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        reviewsState.reviews.forEach { review ->
                            ReviewCard(review)
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Sticky reserve bar, always visible while scrolling reviews
        ReserveBar(
            price = pharmacy.price,
            inStock = inStock,
            loading = state is ReservationUiState.Loading,
            errorMessage = (state as? ReservationUiState.Error)?.message,
            onReserve = { viewModel.reserve(pharmacy._id, medicineId) }
        )
    }
}

// ---------- Hero ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailHero(pharmacy: PharmacyNearbyResponse) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(BottleGreen, Color(0xFF1B3128))))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "PHARMACY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Amber,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = pharmacy.name,
                style = MaterialTheme.typography.headlineSmall,
                color = WarmPaper
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = pharmacy.address,
                style = MaterialTheme.typography.bodyMedium,
                color = WarmPaper.copy(alpha = 0.78f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroPill(text = "${"%.1f".format(pharmacy.distanceKm)} km away")
                if ((pharmacy.reviewCount ?: 0) > 0 && pharmacy.averageRating != null) {
                    HeroPill(
                        text = "${"%.1f".format(pharmacy.averageRating)} (${pharmacy.reviewCount})",
                        icon = Icons.Filled.Star
                    )
                }
                if (pharmacy.is24Hours == true) HeroPill(text = "Open 24 hrs", icon = Icons.Filled.AccessTime)
                if (pharmacy.verified) HeroPill(text = "Verified", icon = Icons.Filled.Verified)
            }
        }
    }
}

@Composable
private fun HeroPill(text: String, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, WarmPaper.copy(alpha = 0.3f), RoundedCornerShape(50))
            .background(WarmPaper.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Amber, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = WarmPaper
        )
    }
}

// ---------- Map ----------

@Composable
private fun DetailMap(pharmacy: PharmacyNearbyResponse, onDirections: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
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
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onDirections)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Directions,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Directions",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

// ---------- Medicine card ----------

@Composable
private fun MedicineCard(
    pharmacy: PharmacyNearbyResponse,
    medicineName: String,
    inStock: Boolean
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(16.dp)
    ) {
        Text(
            text = "MEDICINE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = medicineName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "AVAILABILITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(
                                if (inStock) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (inStock) "${pharmacy.stockQty} units" else "Out of stock",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = PlexMono),
                        fontWeight = FontWeight.SemiBold,
                        color = if (inStock) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.error
                    )
                }
                if (inStock && (pharmacy.stockQty ?: 0) <= 5) {
                    Text(
                        text = "Running low. Reserve soon.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            pharmacy.price?.let { price ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "PRICE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "₹$price",
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PlexMono),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        DetailTrustPanel(pharmacy = pharmacy, modifier = Modifier.padding(top = 14.dp))
    }
}

/** Freshness + confidence panel. Renders nothing when the backend sent no trust fields. */
@Composable
private fun DetailTrustPanel(pharmacy: PharmacyNearbyResponse, modifier: Modifier = Modifier) {
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
                    progress = { confidence.coerceIn(0, 100) / 100f },
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

// ---------- Reserve bar ----------

@Composable
private fun ReserveBar(
    price: Double?,
    inStock: Boolean,
    loading: Boolean,
    errorMessage: String?,
    onReserve: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (price != null) {
                    Column {
                        Text(
                            text = "PRICE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "₹$price",
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = PlexMono),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Button(
                    onClick = onReserve,
                    enabled = !loading && inStock,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Amber,
                        contentColor = OnAmberLight,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = OnAmberLight,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (inStock) "Reserve this medicine" else "Out of stock",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ---------- Reviews ----------

@Composable
private fun DetailSectionLabel(text: String) {
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

@Composable
private fun RatingSummary(average: Double?, count: Int) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (count > 0 && average != null) {
            Text(
                text = "%.1f".format(average),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Row {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (index < Math.round(average).toInt()) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = if (count == 1) "Based on 1 review" else "Based on $count reviews",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column {
                Text(
                    text = "No ratings yet",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Visited this pharmacy? Tell others how it went.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReviewFormSection(pharmacyId: String, reviewViewModel: ReviewViewModel) {
    val existingReview = reviewViewModel.myReview
    var rating by remember(existingReview) { mutableStateOf(existingReview?.rating ?: 0) }
    var comment by remember(existingReview) { mutableStateOf(existingReview?.comment ?: "") }
    val submitState = reviewViewModel.submitState

    val ratingLabel = listOf("Tap a star to rate", "Poor", "Fair", "Good", "Very good", "Excellent")
        .getOrElse(rating) { "" }

    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(16.dp)
    ) {
        Text(
            text = if (existingReview != null) "Update your review" else "Leave a review",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            for (i in 1..5) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Rate $i star",
                    tint = if (i <= rating) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable { rating = i }
                        .padding(3.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = ratingLabel,
                style = MaterialTheme.typography.labelMedium,
                color = if (rating > 0) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (rating > 0) FontWeight.Bold else FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            placeholder = { Text("Share your experience (optional)") },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (submitState is SubmitReviewUiState.Error) {
            Text(
                text = submitState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = { reviewViewModel.submitReview(pharmacyId, rating, comment) },
            enabled = rating > 0 && submitState !is SubmitReviewUiState.Loading,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (submitState is SubmitReviewUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (existingReview != null) "Update review" else "Submit review",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ReviewCard(review: ReviewResponse) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = review.userId.name.take(1).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = review.userId.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (index < review.rating) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        if (review.comment.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        review.ownerReply?.text?.let { replyText ->
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                )
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "REPLY FROM PHARMACY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = replyText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Opens the user's maps app at the pharmacy. geo: URIs need no extra permission.
private fun openDirections(context: Context, lat: Double, lng: Double, name: String) {
    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(name)})")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        // No maps app installed; nothing useful to do.
    }
}