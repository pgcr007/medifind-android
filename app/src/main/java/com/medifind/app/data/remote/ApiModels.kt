package com.medifind.app.data.remote

data class RegisterRequest(val name: String, val email: String, val password: String, val role: String)
data class LoginRequest(val email: String, val password: String)

data class UserResponse(val id: String, val name: String, val email: String, val role: String)

data class LoginResponse(val token: String, val user: UserResponse)

data class MedicineResponse(val _id: String, val name: String, val genericName: String?, val category: String?)

data class ChatRequest(val message: String, val lat: Double? = null, val lng: Double? = null)

data class ChatResponse(val reply: String, val searchResults: ChatSearchResults? = null)

data class AlternativesResponse(val alternatives: List<MedicineResponse>)

data class ReviewUserResponse(val _id: String, val name: String)

data class OwnerReplyResponse(val text: String? = null, val repliedAt: String? = null)

data class ReviewResponse(
    val _id: String,
    val pharmacyId: String,
    val userId: ReviewUserResponse,
    val rating: Int,
    val comment: String,
    val ownerReply: OwnerReplyResponse? = null,
    val createdAt: String? = null
)

data class CreateReviewRequest(
    val pharmacyId: String,
    val rating: Int,
    val comment: String
)

data class PharmacyResponse(
    val _id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val verified: Boolean,
    val averageRating: Double? = null,
    val reviewCount: Int? = null
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
    val price: Double? = null,
    val averageRating: Double? = null,
    val reviewCount: Int? = null,
    // Added by the trust / emergency backend. All nullable so older responses still parse.
    val phone: String? = null,
    val is24Hours: Boolean? = null,
    val freshness: String? = null,        // fresh | recent | stale | outdated
    val ageHours: Double? = null,
    val lastVerifiedAt: String? = null,
    val disputed: Boolean? = null,
    val stockConfidence: Int? = null,     // 0-100
    val callAhead: Boolean? = null,
    val reliabilityScore: Double? = null,
    val reliabilityReports: Int? = null,
    val priceRank: Int? = null,
    val isLowestPrice: Boolean? = null,
    val emergencyRank: Int? = null,
    val score: Double? = null
)

data class StockReportRequest(val pharmacyId: String, val medicineId: String, val inStock: Boolean)

data class StockReportResponse(val _id: String, val inStock: Boolean)

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

data class UserProfileResponse(
    val _id: String,
    val name: String,
    val email: String,
    val role: String,
    val phone: String? = null,
    val address: String? = null,
    val dob: String? = null,
    val profilePicture: String? = null
)

data class ProfileUpdateRequest(
    val name: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val dob: String? = null,
    val profilePicture: String? = null
)

data class ReservationDetailResponse(
    val _id: String,
    val userId: String,
    val pharmacyId: PharmacyResponse,
    val medicineId: MedicineResponse,
    val status: String
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