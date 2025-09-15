package com.example.cyclelink.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.cyclelink.R
import com.google.android.gms.location.*
import com.google.firebase.auth.auth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import java.util.concurrent.TimeUnit
import com.example.cyclelink.utils.ActiveRidersRepo


class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    private val notifiedRiders = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocationInFirestore(location.latitude, location.longitude)
                    checkForNearbyRiders(location)
                }
            }
        }
    }

    private fun checkForNearbyRiders(myLocation: Location) {
        val myUid = auth.currentUser?.uid ?: return

        ActiveRidersRepo.activeRiders.forEach { rider ->
            if (rider.uid != myUid && rider.currentLocation != null && !notifiedRiders.contains(rider.uid)) {
                val results = FloatArray(1)
                Location.distanceBetween(
                    myLocation.latitude, myLocation.longitude,
                    rider.currentLocation.latitude, rider.currentLocation.longitude,
                    results
                )
                val distanceInMeters = results[0]

                // Ako je na manje od 500 metara saljem notifikaciju
                if (distanceInMeters < 500) {
                    sendProximityNotification(rider.username)
                    notifiedRiders.add(rider.uid) // Lista da ne saljemo vise notifikacija istom korisniku
                }
            }
        }
    }

    private fun sendProximityNotification(riderName: String) {
        // Provera za dozvole za notifikacije
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        // Kreiram notifikaciju
        val notification = NotificationCompat.Builder(this, "proximity_channel") // Novi kanal
            .setContentTitle("Biciklista u blizini!")
            .setContentText("$riderName je na manje od 500m od vas.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startLocationUpdates()
            ACTION_STOP -> stopLocationUpdates()
        }
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("CycleLink je aktivan")
            .setContentText("Vaša lokacija se deli sa drugima.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        // Radi u pozadini
        startForeground(1, notification)

        auth.currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).update("isRiding", true)
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            TimeUnit.SECONDS.toMillis(15) // Ažuriranje na svakih 15 sekundi
        ).build()

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        auth.currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid)
                .update(mapOf(
                    "isRiding" to false,
                    "currentLocation" to null
                ))
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateLocationInFirestore(latitude: Double, longitude: Double) {
        auth.currentUser?.uid?.let { uid ->
            val geoPoint = GeoPoint(latitude, longitude)
            firestore.collection("users").document(uid)
                .update("currentLocation", geoPoint)
                .addOnFailureListener { e ->
                    Log.e("LocationService", "Greška pri ažuriranju lokacije", e)
                }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Kanal za Foreground Service
        val serviceChannel = NotificationChannel(
            "location_channel",
            "Deljenje Lokacije",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Stalna notifikacija dok CycleLink deli lokaciju"
        }
        notificationManager.createNotificationChannel(serviceChannel)

        // Kanal za Notifikacije o Blizini
        val proximityChannel = NotificationChannel(
            "proximity_channel",
            "Upozorenja o Blizini",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifikacije kada je drugi biciklista u blizini"
        }
        notificationManager.createNotificationChannel(proximityChannel)
    }
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }
}