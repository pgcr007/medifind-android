package com.medifind.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.medifind.app.data.remote.PharmacyNearbyResponse
import com.medifind.app.ui.theme.PlexMono
import kotlin.math.roundToInt

// "12 min ago", "3 h ago", "2 days ago". ageHours comes from the backend's trust fields.
fun formatStockAge(ageHours: Double?): String {
    if (ageHours == null) return "never confirmed"
    return when {
        ageHours < 1.0 / 6 -> "just now"
        ageHours < 1 -> "${(ageHours * 60).roundToInt()} min ago"
        ageHours < 24 -> "${ageHours.roundToInt()} h ago"
        else -> {
            val days = (ageHours / 24).roundToInt()
            "$days ${if (days == 1) "day" else "days"} ago"
        }
    }
}

// Shows how current and reliable a pharmacy's stock data is. Renders nothing when the
// backend sent no trust fields (for example a search without a medicine selected).
@Composable
fun TrustBadge(pharmacy: PharmacyNearbyResponse, modifier: Modifier = Modifier) {
    val freshness = pharmacy.freshness ?: return

    val (label, color) = when (freshness) {
        "fresh" -> "Fresh" to MaterialTheme.colorScheme.primary
        "recent" -> "Recent" to MaterialTheme.colorScheme.onSurfaceVariant
        "stale" -> "Stale" to MaterialTheme.colorScheme.tertiary
        else -> "Outdated" to MaterialTheme.colorScheme.error
    }

    Column(modifier = modifier) {
        Text(
            text = "$label · confirmed ${formatStockAge(pharmacy.ageHours)}",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlexMono),
            color = color,
            fontWeight = FontWeight.Medium
        )
        pharmacy.stockConfidence?.let {
            Text(
                text = "Stock confidence: $it%",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlexMono),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (pharmacy.disputed == true) {
            Text(
                text = "Some patients reported this as missing",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (pharmacy.callAhead == true) {
            Text(
                text = "Call ahead to confirm before travelling",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// Opens the phone dialer with the number filled in. ACTION_DIAL needs no CALL_PHONE
// permission because the user still presses the call button themselves.
fun dialPharmacy(context: Context, phone: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone.trim(), null))
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Device has no dialer (for example a tablet); nothing useful to do.
    }
}