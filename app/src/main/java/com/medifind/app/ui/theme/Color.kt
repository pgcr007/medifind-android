package com.medifind.app.ui.theme

import androidx.compose.ui.graphics.Color

// --- Apothecary Ledger palette (matches web dashboards) ---

// Core brand colors
val BottleGreen = Color(0xFF2C4A3E)
val WarmPaper = Color(0xFFFAF7F0)
val Amber = Color(0xFFC08A34)
val Rust = Color(0xFFA83B32)

// Light theme derived tones
val BottleGreenLight = Color(0xFFE3ECE7)
val OnBottleGreenLight = Color(0xFF1A2F26)
val AmberLight = Color(0xFFF5E6CC)
val OnAmberLight = Color(0xFF4A3413)
val RustLight = Color(0xFFF5D9D6)
val OnRustLight = Color(0xFF4A1410)
val SurfaceVariantLight = Color(0xFFECE5D8)
val OnSurfaceVariantLight = Color(0xFF57503F)
val OnPaperText = Color(0xFF2C2620)
val OutlineLight = Color(0xFFA89F8C)

// Dark theme derived tones (deep green-black base, warm text)
val DarkBackground = Color(0xFF1A1F1C)
val DarkSurface = Color(0xFF212823)
val OnDarkSurface = Color(0xFFF0ECE3)
val GreenAccentDark = Color(0xFF7FAE97)
val OnGreenAccentDark = Color(0xFF0D1F17)
val AmberAccentDark = Color(0xFFD9A04C)
val OnAmberAccentDark = Color(0xFF3A2A0F)
val RustAccentDark = Color(0xFFD97A6E)
val OnRustAccentDark = Color(0xFF3A100C)
val SurfaceVariantDark = Color(0xFF39423C)
val OnSurfaceVariantDark = Color(0xFFC2C9C1)
val OutlineDark = Color(0xFF8C9389)

// Status colors (used for reservation/reminder badges — kept consistent with dashboards)
val StatusConfirmedGreen = Color(0xFF2E7D32)
val StatusPendingAmber = Amber
val StatusRejectedRust = Rust
val StatusCancelledGray = Color(0xFF757575)