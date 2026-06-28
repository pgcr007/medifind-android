package com.medifind.app.ui.navigation

object NavRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val PHARMACY_RESULTS = "pharmacy_results/{medicineId}/{medicineName}"
    const val PHARMACY_DETAIL = "pharmacy_detail"
    const val RESERVATION_CONFIRMATION = "reservation_confirmation/{pharmacyName}/{medicineName}"

    fun pharmacyResults(medicineId: String, medicineName: String) =
        "pharmacy_results/$medicineId/$medicineName"

    fun reservationConfirmation(pharmacyName: String, medicineName: String) =
        "reservation_confirmation/$pharmacyName/$medicineName"
}