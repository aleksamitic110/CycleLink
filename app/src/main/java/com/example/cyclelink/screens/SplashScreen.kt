package com.example.cyclelink.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import kotlinx.coroutines.delay
import com.example.cyclelink.R
import com.example.cyclelink.utils.updateFCMToken
import com.google.firebase.messaging.messaging
@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(key1 = true) {
        delay(3000L)

        // Da li je user logovan
        val startDestination = if (Firebase.auth.currentUser != null) {
            updateFCMToken()
            Firebase.messaging.subscribeToTopic("new_rides")
            "home_screen"
        } else {
            "auth_screen"
        }

        navController.navigate(startDestination) {
            popUpTo("splash_screen") { inclusive = true }
        }

    }

    // Ucitavanje animacije
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.cycling_animation)
    )
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background // Podrazumevana tema
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(250.dp)
            )
        }
    }
}