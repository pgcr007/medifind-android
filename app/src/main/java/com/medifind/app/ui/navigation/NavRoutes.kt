package com.medifind.app.ui.navigation

object NavRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val PHARMACY_RESULTS = "pharmacy_results/{medicineId}/{medicineName}"
    const val PHARMACY_DETAIL = "pharmacy_detail"
    const val RESERVATION_CONFIRMATION = "reservation_confirmation/{pharmacyName}/{medicineName}"

    const val PROFILE = "profile"

    const val PRESCRIPTION_SCAN = "prescription_scan"

    const val CHAT = "chat"

    const val REMINDERS = "reminders"
    const val ADD_REMINDER = "add_reminder"

    const val PRESCRIPTION_VAULT = "prescription_vault"           // ADD THIS
    const val PRESCRIPTION_SCAN_VAULT = "prescription_scan_vault" // ADD THIS
    const val PRESCRIPTION_DETAIL = "prescription_detail"         // ADD THIS

    fun pharmacyResults(medicineId: String, medicineName: String) =
        "pharmacy_results/$medicineId/$medicineName"

    fun reservationConfirmation(pharmacyName: String, medicineName: String) =
        "reservation_confirmation/$pharmacyName/$medicineName"
}