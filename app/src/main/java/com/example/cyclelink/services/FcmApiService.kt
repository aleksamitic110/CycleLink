// Fajl: services/FcmApiService.kt
package com.example.cyclelink.services

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

// Data klase koje opisuju šta šaljemo
data class NotificationPayload(
    @SerializedName("to") val to: String,
    @SerializedName("notification") val notification: NotificationData
)
data class NotificationData(
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String
)

// Retrofit interfejs
interface FcmApiService {
    @Headers(
        "Authorization: key=TVOJ_SERVER_KLJUC_IDE_OVDE",
        "Content-Type: application/json"
    )
    @POST("fcm/send")
    suspend fun sendNotification(@Body payload: NotificationPayload): Response<Unit>
}

// Objekat za lako kreiranje servisa
object RetrofitInstance {
    val api: FcmApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://fcm.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FcmApiService::class.java)
    }
}