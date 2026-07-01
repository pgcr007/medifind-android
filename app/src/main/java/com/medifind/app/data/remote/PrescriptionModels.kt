package com.medifind.app.data.remote

data class PrescriptionRequest(
    val localImageId: String,
    val extractedText: String? = null,
    val medicines: List<String>? = null,
    val doctorName: String? = null,
    val prescriptionDate: String? = null,
    val notes: String? = null
)

data class PrescriptionUpdateRequest(
    val extractedText: String? = null,
    val medicines: List<String>? = null,
    val doctorName: String? = null,
    val prescriptionDate: String? = null,
    val notes: String? = null
)

data class PrescriptionResponse(
    val _id: String,
    val userId: String,
    val localImageId: String,
    val extractedText: String?,
    val medicines: List<String>?,
    val doctorName: String?,
    val prescriptionDate: String?,
    val notes: String?,
    val createdAt: String?
)