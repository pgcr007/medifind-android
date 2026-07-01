package com.medifind.app.data.remote

data class RegisterRequest(val name: String, val email: String, val password: String, val role: String)
data class LoginRequest(val email: String, val password: String)

data class UserResponse(val id: String, val name: String, val email: String, val role: String)

data class LoginResponse(val token: String, val user: UserResponse)

data class MedicineResponse(val _id: String, val name: String, val genericName: String?, val category: String?)

data class ChatRequest(val message: String, val lat: Double? = null, val lng: Double? = null)

data class ChatResponse(val reply: String, val searchResults: ChatSearchResults? = null)

data class AlternativesResponse(val alternatives: List<MedicineResponse>)



data class PharmacyResponse(
    val _id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val verified: Boolean
)

data class SearchResultPharmacy(
    val pharmacyName: String,
    val address: String,
    val stockQty: Int? = null,
    val price: Double? = null,
    val distanceKm: Double? = null
)

data class ChatSearchResults(
    val medicineId: String,
    val medicineName: String,
    val pharmacies: List<SearchResultPharmacy>
)

data class PharmacyNearbyResponse(
    val _id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val verified: Boolean,
    val distanceKm: Double,
    val stockQty: Int? = null,
    val price: Double? = null
)

data class ReservationRequest(val pharmacyId: String, val medicineId: String)

data class ReservationResponse(
    val _id: String,
    val userId: String,
    val pharmacyId: String,
    val medicineId: String,
    val status: String
)
data class ReminderRequest(
    val medicineId: String,
    val dosageTimes: List<String>,
    val refillIntervalDays: Int
)

data class ReminderUpdateRequest(
    val dosageTimes: List<String>? = null,
    val refillIntervalDays: Int? = null,
    val isActive: Boolean? = null
)

data class ReminderResponse(
    val _id: String,
    val userId: String,
    val medicineId: MedicineResponse,
    val dosageTimes: List<String>,
    val refillIntervalDays: Int,
    val lastRefillDate: String,
    val refillReminderSent: Boolean,
    val isActive: Boolean
)

data class FcmTokenRequest(
    val fcmToken: String
)