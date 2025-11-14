package com.example.fintrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost // <-- Import from androidx
import androidx.navigation.compose.composable // <-- Import from androidx
import androidx.navigation.compose.rememberNavController // <-- Import from androidx
import com.example.fintrack.presentation.auth.LoginScreen
import com.example.fintrack.presentation.auth.RegistrationScreen
import com.example.fintrack.presentation.ui.theme.FinTrackTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinTrackTheme {
                Surface {
                    // 1. Use the standard NavController
                    val navController = rememberNavController()

                    // 2. Use the standard NavHost
                    NavHost(
                        navController = navController,
                        startDestination = "login_screen"
                    ) {

                        // 3. Define transitions for each composable
                        composable(
                            "login_screen",
                            popEnterTransition = {
                                slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(300))
                            },
                            exitTransition = {
                                slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(300))
                            }
                        ) {
                            LoginScreen(
                                onNavigateToHome = {
                                    navController.navigate("home_screen") {
                                        popUpTo("login_screen") { inclusive = true }
                                    }
                                },
                                onNavigateToRegister = {
                                    navController.navigate("registration_screen")
                                }
                            )
                        }

                        composable(
                            "registration_screen",
                            enterTransition = {
                                slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(300))
                            },
                            popExitTransition = {
                                slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(300))
                            }
                        ) {
                            RegistrationScreen(
                                onNavigateToHome = {
                                    navController.navigate("home_screen") {
                                        popUpTo("login_screen") { inclusive = true }
                                    }
                                },
                                onNavigateBackToLogin = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(
                            "home_screen",
                            enterTransition = { fadeIn(animationSpec = tween(500)) }
                        ) {
                            androidx.compose.material3.Text("Home Screen - Logged In!")
                        }
                    }
                }
            }
        }
    }
}