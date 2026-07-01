package com.medifind.app.data.remote

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/reminders")
    suspend fun createReminder(
        @Header("Authorization") token: String,
        @Body request: ReminderRequest
    ): Response<ReminderResponse>

    @GET("api/reminders")
    suspend fun getMyReminders(@Header("Authorization") token: String): Response<List<ReminderResponse>>

    @PUT("api/reminders/{id}")
    suspend fun updateReminder(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body request: ReminderUpdateRequest
    ): Response<ReminderResponse>

    @DELETE("api/reminders/{id}")
    suspend fun deleteReminder(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Unit>

    @PUT("api/auth/fcm-token")
    suspend fun updateFcmToken(
        @Header("Authorization") token: String,
        @Body request: FcmTokenRequest
    ): Response<Unit>



    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<UserResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/medicines/search")
    suspend fun searchMedicines(@Query("name") name: String): Response<List<MedicineResponse>>

    @GET("api/pharmacies/nearby")
    suspend fun getNearbyPharmacies(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("medicineId") medicineId: String? = null
    ): Response<List<PharmacyNearbyResponse>>

    @GET("api/pharmacies/{id}")
    suspend fun getPharmacyById(@Path("id") id: String): Response<PharmacyResponse>


    @GET("api/medicines/{id}/alternatives")
    suspend fun getAlternatives(@Path("id") medicineId: String): Response<AlternativesResponse>

    @POST("api/reservations")
    suspend fun createReservation(
        @Header("Authorization") token: String,
        @Body request: ReservationRequest
    ): Response<ReservationResponse>

    @GET("api/reservations/me")
    suspend fun getMyReservations(@Header("Authorization") token: String): Response<List<ReservationResponse>>

    @POST("api/chat")
    suspend fun sendChatMessage(
        @Header("Authorization") token: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>

    @POST("api/prescriptions")
    suspend fun createPrescription(
        @Header("Authorization") token: String,
        @Body request: PrescriptionRequest
    ): Response<PrescriptionResponse>

    @GET("api/prescriptions")
    suspend fun getMyPrescriptions(@Header("Authorization") token: String): Response<List<PrescriptionResponse>>

    @GET("api/prescriptions/{id}")
    suspend fun getPrescriptionById(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<PrescriptionResponse>

    @PUT("api/prescriptions/{id}")
    suspend fun updatePrescription(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body request: PrescriptionUpdateRequest
    ): Response<PrescriptionResponse>

    @DELETE("api/prescriptions/{id}")
    suspend fun deletePrescription(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Unit>
}