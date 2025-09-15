package com.example.cyclelink.utils

import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging

fun updateFCMToken() {
    val auth = Firebase.auth
    val firestore = Firebase.firestore

    val userId = auth.currentUser?.uid ?: return

    Firebase.messaging.token.addOnCompleteListener { task ->
        if (!task.isSuccessful) {
           return@addOnCompleteListener
        }
       val token = task.result
        if (token != null) {
            firestore.collection("users").document(userId)
                .update("fcmToken", token)
        }
    }
}