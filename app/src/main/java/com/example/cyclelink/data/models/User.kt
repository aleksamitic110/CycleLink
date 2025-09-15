package com.example.cyclelink.data.models
import com.google.firebase.firestore.GeoPoint
data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val profilePictureUrl: String = "",
    val isRiding: Boolean = false,
    val currentLocation: GeoPoint? = null,
    val firstName: String = "",
    val lastName: String = "",
    val bio: String = "",
    val points: Long = 0,
    val isVerified: Boolean = false,
    val fcmToken: String = ""
) {
    constructor() : this("", "", "", "", false, null, "", "", "", 0L, false, "")
}