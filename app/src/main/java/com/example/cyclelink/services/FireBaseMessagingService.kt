// Fajl: services/MyFirebaseMessagingService.kt
package com.example.cyclelink.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.cyclelink.utils.updateFCMToken
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Ovde stiže notifikacija dok je aplikacija OTVORENA
        Log.d("FCM", "From: ${remoteMessage.from}")

        remoteMessage.notification?.let {
            Log.d("FCM", "Message Notification Body: ${it.body}")
            // TODO: Prikazati notifikaciju ručno, jer sistem to ne radi
            // automatski kada je aplikacija u prvom planu.
        }
    }

    override fun onNewToken(token: String) {
        // Ovaj token je jedinstvena adresa ovog uređaja.
        // Treba ga sačuvati u Firestore dokumentu korisnika!
        Log.d("FCM", "Refreshed token: $token")
        updateFCMToken()
    }
}