package com.example.fintrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fintrack.presentation.auth.LoginScreen
import com.example.fintrack.ui.theme.FinTrackTheme // Make sure this imports your theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint // <--- VERY IMPORTANT for Hilt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Replace 'FinTrackTheme' with whatever your actual theme name is
            // (it was likely created automatically in the ui.theme package)
            MaterialTheme {
                Surface {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "login_screen"
                    ) {
                        composable("login_screen") {
                            LoginScreen(
                                onNavigateToHome = {
                                    navController.navigate("home_screen") {
                                        // Clear back stack so user can't back-button to login
                                        popUpTo("login_screen") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home_screen") {
                            // We haven't built HomeScreen yet, so just show a placeholder
                            androidx.compose.material3.Text("Home Screen - Logged In!")
                        }
                    }
                }
            }
        }
    }
}