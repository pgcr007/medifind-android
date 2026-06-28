package com.medifind.app.data.remote

data class RegisterRequest(val name: String, val email: String, val password: String, val role: String)
data class LoginRequest(val email: String, val password: String)

data class UserResponse(val id: String, val name: String, val email: String, val role: String)

data class LoginResponse(val token: String, val user: UserResponse)

data class MedicineResponse(val _id: String, val name: String, val genericName: String?, val category: String?)

data class ChatRequest(val message: String)


data class ChatResponse(val reply: String)

data class PharmacyResponse(
    val _id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val verified: Boolean
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