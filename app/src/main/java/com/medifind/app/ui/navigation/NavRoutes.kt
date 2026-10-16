package com.medifind.app.ui.navigation
import android.net.Uri

object NavRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val PHARMACY_RESULTS = "pharmacy_results/{medicineId}/{medicineName}?emergency={emergency}"

    const val PRICE_COMPARISON = "price_comparison/{medicineId}/{medicineName}"

    const val PHARMACY_DETAIL = "pharmacy_detail"
    const val RESERVATION_CONFIRMATION = "reservation_confirmation/{pharmacyName}/{medicineName}"

    const val PROFILE = "profile"

    const val PRESCRIPTION_SCAN = "prescription_scan"

    const val CHAT = "chat"

    const val REMINDERS = "reminders"
    const val ADD_REMINDER = "add_reminder"

    const val MY_RESERVATIONS = "my_reservations"

    const val PRESCRIPTION_VAULT = "prescription_vault"           // ADD THIS
    const val PRESCRIPTION_SCAN_VAULT = "prescription_scan_vault" // ADD THIS
    const val PRESCRIPTION_DETAIL = "prescription_detail"         // ADD THIS

    // add alongside PRESCRIPTION_SCAN_VAULT
    const val BARCODE_SCAN = "barcode_scan"

    fun pharmacyResults(medicineId: String, medicineName: String, emergency: Boolean = false) =
        "pharmacy_results/$medicineId/${Uri.encode(medicineName)}?emergency=$emergency"

    fun priceComparison(medicineId: String, medicineName: String) =
        "price_comparison/$medicineId/${Uri.encode(medicineName)}"

    fun reservationConfirmation(pharmacyName: String, medicineName: String) =
        "reservation_confirmation/${Uri.encode(pharmacyName)}/${Uri.encode(medicineName)}"
}