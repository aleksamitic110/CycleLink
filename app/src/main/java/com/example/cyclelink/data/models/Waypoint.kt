package com.example.cyclelink.data.models

import com.google.android.gms.maps.model.LatLng

data class Waypoint(
    val location: LatLng,
    var title: String = "Tačka",
    var description: String = ""
)
