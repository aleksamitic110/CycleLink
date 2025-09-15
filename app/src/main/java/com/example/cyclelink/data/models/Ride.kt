package com.example.cyclelink.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class Ride(
    val title: String = "",
    val description: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorImageUrl: String = "",
    @field:JvmField val startTime: Timestamp = Timestamp.now(),
    @field:JvmField val createdAt: Timestamp = Timestamp.now(),
    val status: String = "planned",
    val waypoints: List<GeoPoint> = emptyList(),
    val participants: List<String> = emptyList(),
    val difficulty: String = "Srednja",
    val distance: Double = 0.0,
    val waypointTitles: Map<String, String> = emptyMap(),
    val overviewPolyline: String = ""
) {
    constructor() : this(
        "", "", "", "", "", Timestamp.now(), Timestamp.now(),
        "planned", emptyList(), emptyList(), "Srednja", 0.0, emptyMap(), ""
    )
}