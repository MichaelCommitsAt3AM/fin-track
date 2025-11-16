package com.example.fintrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fintrack.presentation.navigation.BottomNavItem
import com.example.fintrack.presentation.navigation.NavGraph
import com.example.fintrack.presentation.ui.components.BottomNavBar
import com.example.fintrack.presentation.navigation.AppRoutes
import com.example.fintrack.presentation.ui.theme.FinTrackTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinTrackTheme {
                val navController = rememberNavController()

                // Determine start destination based on current auth state
                val startDestination = remember {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    when {
                        currentUser == null -> AppRoutes.Login.route
                        !currentUser.isEmailVerified -> AppRoutes.VerifyEmail.route
                        else -> BottomNavItem.Home.route
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
                                shape = CircleShape,
                                modifier = Modifier.offset(y = 45.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Transaction"
                                )
                            }
                        }
                    },
                    floatingActionButtonPosition = FabPosition.Center
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
