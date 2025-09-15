package com.example.cyclelink

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cyclelink.screens.*
import com.example.cyclelink.services.LocationService
import com.example.cyclelink.ui.theme.CycleLinkTheme

enum class ThemeSetting {
    System, Light, Dark
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            var themeSetting by remember { mutableStateOf(ThemeSetting.System) }
            val isDarkTheme = when (themeSetting) {
                ThemeSetting.System -> isSystemInDarkTheme()
                ThemeSetting.Light -> false
                ThemeSetting.Dark -> true
            }
            val toggleTheme: () -> Unit = {
                themeSetting = if (isDarkTheme) ThemeSetting.Light else ThemeSetting.Dark
            }

            CycleLinkTheme(darkTheme = isDarkTheme) {
                AppNavigation(
                    themeSetting = themeSetting,
                    toggleTheme = toggleTheme
                )
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        Intent(this, LocationService::class.java).also {
            it.action = LocationService.ACTION_STOP
            startService(it)
        }
    }
}

@Composable
fun AppNavigation(themeSetting: ThemeSetting, toggleTheme: () -> Unit) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash_screen") {
        composable("splash_screen") {
            SplashScreen(navController = navController)
        }
        composable("auth_screen") {
            AuthScreen(navController = navController)
        }
        composable("home_screen") {
            HomeScreen(
                navController = navController,
                currentTheme = themeSetting,
                toggleTheme = toggleTheme
            )
        }
        composable("create_ride_screen") {
            CreateRideScreen(navController = navController, themeSetting = themeSetting)
        }

        composable("profile_screen") {
            ProfileScreen(navController = navController)
        }

        composable(
            route = "user_profile_screen/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            if (userId != null) {
                UserProfileScreen(navController = navController, userId = userId)
            }
        }

        composable("leaderboard_screen") {
            LeaderboardScreen(navController = navController)
        }
    }
}