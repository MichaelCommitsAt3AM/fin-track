package com.example.fintrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fintrack.presentation.auth.LoginScreen
import com.example.fintrack.presentation.ui.theme.FinTrackTheme // <-- Import your new theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Use your new custom theme here
            FinTrackTheme {
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
                                        popUpTo("login_screen") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home_screen") {
                            androidx.compose.material3.Text("Home Screen - Logged In!")
                        }
                    }
                }
            }
        }
    }
}