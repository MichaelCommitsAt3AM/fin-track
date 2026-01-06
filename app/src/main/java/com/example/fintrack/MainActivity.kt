package com.example.fintrack

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fintrack.core.data.local.LocalAuthManager
import com.example.fintrack.core.ui.components.BottomNavBar
import com.example.fintrack.presentation.auth.BiometricLoginScreen
import com.example.fintrack.presentation.navigation.AppRoutes
import com.example.fintrack.presentation.navigation.NavGraph
import com.example.fintrack.presentation.ui.theme.FinTrackTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() { // Changed to FragmentActivity for BiometricPrompt

    @Inject
    lateinit var localAuthManager: LocalAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Check biometric status synchronously before content is set to prevent UI flash
        // In a production app, you might use a splash screen instead of runBlocking
        val isBiometricEnabled = runBlocking { localAuthManager.isBiometricEnabled.first() }

        setContent {
            FinTrackTheme {
                // State to control the app lock.
                // If biometric is enabled in settings, we start locked.
                var isAppLocked by remember { mutableStateOf(isBiometricEnabled) }

                if (isAppLocked) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        BiometricLoginScreen(
                            onSuccess = {
                                isAppLocked = false
                            },
                            onUsePin = {
                                // TODO: Switch to PIN input screen state here
                                // For now, you might want to handle this or keep biometric prompt open
                            }
                        )
                    }
                } else {
                    // --- Main Application Content ---
                    val navController = rememberNavController()

                    // Determine start destination based on current auth state
                    // This runs only after the local app lock is cleared
                    val startDestination = remember {
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        when {
                            currentUser == null -> AppRoutes.Login.route
                            !currentUser.isEmailVerified -> AppRoutes.VerifyEmail.route
                            else -> AppRoutes.Home.route
                        }
                    }

                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    val mainTabRoutes = listOf(
                        AppRoutes.Home.route,
                        AppRoutes.Reports.route,
                        AppRoutes.Budgets.route,
                        AppRoutes.SettingsGraph.route,
                        AppRoutes.Settings.route
                    )

                    val showBottomNav = currentRoute in mainTabRoutes

                    Scaffold(
                        bottomBar = {
                            if (showBottomNav) {
                                BottomNavBar(navController = navController)
                            }
                        },
                        floatingActionButton = {
                            if (showBottomNav) {
                                FloatingActionButton(
                                    onClick = { navController.navigate(AppRoutes.AddTransaction.route) },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    shape = CircleShape
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Transaction"
                                    )
                                }
                            }
                        },
                        floatingActionButtonPosition = FabPosition.End
                    ) { paddingValues ->
                        NavGraph(
                            navController = navController,
                            paddingValues = paddingValues,
                            startDestination = startDestination
                        )
                    }
                }
            }
        }
    }
}