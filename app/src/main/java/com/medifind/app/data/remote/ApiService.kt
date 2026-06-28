package com.medifind.app.data.remote

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

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
}