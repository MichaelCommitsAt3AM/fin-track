package com.example.fintrack

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fintrack.core.data.local.LocalAuthManager
import com.example.fintrack.core.ui.components.BottomNavBar
import com.example.fintrack.presentation.MainViewModel
import com.example.fintrack.presentation.auth.BiometricLoginScreen
import com.example.fintrack.presentation.auth.pin.PinLoginScreen
import com.example.fintrack.presentation.common.components.OfflineBanner
import com.example.fintrack.presentation.navigation.AppRoutes
import com.example.fintrack.presentation.navigation.NavGraph
import com.example.fintrack.presentation.ui.theme.FinTrackTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() { // Changed to FragmentActivity for BiometricPrompt

    @Inject lateinit var localAuthManager: LocalAuthManager
    
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Check biometric status synchronously before content is set to prevent UI flash
        // In a production app, you might use a splash screen instead of runBlocking
        val isBiometricEnabled = runBlocking { localAuthManager.isBiometricEnabled.first() }

        setContent {
            val themePreference by localAuthManager.themePreference.collectAsState(initial = "Dark")

            val useDarkTheme =
                    when (themePreference) {
                        "Light" -> false
                        "Dark" -> true
                        else -> isSystemInDarkTheme()
                    }

            FinTrackTheme(darkTheme = useDarkTheme) {
                // State to control the app lock.
                // If biometric is enabled in settings, we start locked.
                var isAppLocked by remember { mutableStateOf(isBiometricEnabled) }
                var showPinLock by remember { mutableStateOf(false) }

                if (isAppLocked) {
                    Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                    ) {
                        if (showPinLock) {
                            PinLoginScreen(
                                    onPinVerified = { isAppLocked = false },
                                    onUseBiometrics = { showPinLock = false },
                                    onForgotPin = {
                                        // Unlock to allow navigation to recovery flows
                                        isAppLocked = false
                                    },
                                    isBiometricAvailable = true
                            )
                        } else {
                            BiometricLoginScreen(
                                    onSuccess = { isAppLocked = false },
                                    onUsePin = { showPinLock = true }
                            )
                        }
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

                    val mainTabRoutes =
                            listOf(
                                    AppRoutes.Home.route,
                                    AppRoutes.Reports.route,
                                    AppRoutes.Goals.route,
                                    AppRoutes.TransactionList.route
                            )

                    val showBottomNav = currentRoute in mainTabRoutes

                    // Offline banner state management
                    val isOnline by mainViewModel.isOnline.collectAsState()
                    val currentUser by mainViewModel.currentUser.collectAsState()
                    var showOfflineBanner by remember { mutableStateOf(true) }

                    // Reset banner visibility when coming back online
                    LaunchedEffect(isOnline) {
                        if (isOnline) {
                            showOfflineBanner = true
                        }
                    }

                    // Trigger sync when network reconnects
                    LaunchedEffect(isOnline) {
                        if (isOnline && currentRoute in mainTabRoutes) {
                            // Trigger background sync when back online
                            val workManager = androidx.work.WorkManager.getInstance(applicationContext)
                            val syncWork = androidx.work.OneTimeWorkRequestBuilder<com.example.fintrack.core.worker.TransactionSyncWorker>()
                                .setConstraints(com.example.fintrack.core.worker.TransactionSyncWorker.getConstraints())
                                .build()
                            
                            workManager.enqueueUniqueWork(
                                com.example.fintrack.core.worker.TransactionSyncWorker.WORK_NAME,
                                androidx.work.ExistingWorkPolicy.REPLACE,
                                syncWork
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Drawer state for profile drawer
                        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                        val scope = rememberCoroutineScope()

                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            gesturesEnabled = drawerState.isOpen,
                            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
                            drawerContent = {
                                com.example.fintrack.presentation.home.ProfileDrawerContent(
                                    user = currentUser,
                                    onCloseDrawer = {
                                        scope.launch { drawerState.close() }
                                    },
                                    onNavigateToProfile = {
                                        scope.launch { drawerState.close() }
                                        navController.navigate(AppRoutes.Settings.route)
                                    },
                                    onNavigateToSettings = {
                                        scope.launch { drawerState.close() }
                                        navController.navigate(AppRoutes.Settings.route)
                                    }
                                )
                            }
                        ) {
                            Scaffold(
                                    bottomBar = {
                                        if (showBottomNav) {
                                            BottomNavBar(navController = navController)
                                        }
                                    },
                                    floatingActionButton = {
                                        if (showBottomNav) {
                                            FloatingActionButton(
                                                    onClick = {
                                                        navController.navigate(
                                                                AppRoutes.AddTransaction.route
                                                        )
                                                    },
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
                                        startDestination = startDestination,
                                        onOpenDrawer = { scope.launch { drawerState.open() } }
                                )
                            }
                        }

                        // Global Offline Banner
                        AnimatedVisibility(
                                visible = !isOnline && showOfflineBanner,
                                enter = slideInVertically(initialOffsetY = { -it }),
                                exit = slideOutVertically(targetOffsetY = { -it }),
                                modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .fillMaxWidth()
                                        .padding(WindowInsets.statusBars.asPaddingValues())
                        ) {
                            OfflineBanner(
                                    onDismiss = { showOfflineBanner = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
