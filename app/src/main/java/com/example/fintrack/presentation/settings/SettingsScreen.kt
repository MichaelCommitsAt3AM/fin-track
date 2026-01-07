package com.example.fintrack.presentation.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fintrack.presentation.navigation.AppRoutes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.fintrack.R
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToManageCategories: () -> Unit,
    navController: NavController,
    paddingValues: PaddingValues,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    // Observe the biometric state from ViewModel
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()

    // Listen for logout event
    LaunchedEffect(key1 = true) {
        viewModel.settingsEvent.collect { event ->
            when (event) {
                is SettingsEvent.NavigateToLogin -> {
                    onNavigateToLogin()
                }
            }
        }
    }


    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SettingsTopBar(onBackClick = { navController.popBackStack() })
        }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = scaffoldPadding.calculateTopPadding())
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Profile Section
            ProfileHeader()

            Spacer(modifier = Modifier.height(24.dp))

            // Account Section
            SettingsSection(title = "Account") {
                SettingsItem(icon = Icons.Default.Person, title = "Manage Profile", onClick = {navController.navigate(AppRoutes.ManageProfile.route)})
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(icon = Icons.Default.AccountBalance, title = "Linked Accounts", onClick = {})
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Security Section
            SettingsSection(title = "Security") {
                SettingsItem(icon = Icons.Default.Lock, title = "Change Password", onClick = {})
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Biometric Toggle
                SettingsItem(
                    icon = Icons.Default.Fingerprint,
                    title = "Biometric Login",
                    hasToggle = true,
                    isToggleChecked = isBiometricEnabled,
                    onToggleChange = { isChecked ->
                        if (isChecked) {
                            // If turning ON, navigate to Setup Screen
                            navController.navigate(AppRoutes.BiometricSetup.route)
                        } else {
                            // If turning OFF, disable immediately via ViewModel
                            viewModel.disableBiometric()
                        }
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(icon = Icons.Default.VerifiedUser, title = "Two-Factor Authentication", onClick = {})
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Preferences Section
            SettingsSection(title = "App Preferences") {
                SettingsItem(
                    icon = Icons.Default.Category,
                    title = "Manage Categories",
                    onClick = onNavigateToManageCategories
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(
                    icon = Icons.Default.Repeat,
                    title = "Manage Recurring Transactions",
                    onClick = { navController.navigate(AppRoutes.RecurringTransactions.route)}
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(icon = Icons.Default.Notifications, title = "Notifications", onClick = {})
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(icon = Icons.Default.Paid, title = "Currency", trailingText = "USD", onClick = {})
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(icon = Icons.Default.Contrast, title = "Theme", trailingText = "Dark", onClick = {})
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Support Section
            SettingsSection(title = "Support") {
                SettingsItem(icon = Icons.AutoMirrored.Filled.HelpCenter, title = "Help & Support", onClick = {})
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(icon = Icons.Default.Gavel, title = "Terms of Service", onClick = {})
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(icon = Icons.Default.PrivacyTip, title = "Privacy Policy", onClick = {})
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Logout Button
            Button(
                onClick = { viewModel.onLogout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.error
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = "Log Out")
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Log Out", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// --- Reusable Components ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(onBackClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
            titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
    )
}

@Composable
fun ProfileHeader(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    var avatarId by remember { mutableStateOf(1) }

    LaunchedEffect(key1 = currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            try {
                val userDoc = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
                avatarId = userDoc.getLong("avatarId")?.toInt() ?: 1
            } catch (e: Exception) {
                avatarId = 1
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // --- FIXED CRASH HERE ---
        // Replaced getIdentifier with safe helper function
        Image(
            painter = painterResource(id = getAvatarResource(avatarId)),
            contentDescription = "Profile Picture",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
        )
        // ------------------------

        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = currentUser?.displayName ?: "User",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = currentUser?.email ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            content()
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    trailingText: String? = null,
    hasToggle: Boolean = false,
    isToggleChecked: Boolean = false,
    onToggleChange: ((Boolean) -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !hasToggle) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.background), // icon-bg-light/dark
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        // Trailing Content
        if (hasToggle) {
            Switch(
                checked = isToggleChecked,
                onCheckedChange = onToggleChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.scale(0.8f)
            )
        } else {
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Helper Function
fun getAvatarResource(avatarId: Int): Int {
    return when (avatarId) {
        1 -> R.drawable.avatar_default
        2 -> R.drawable.avatar_female
        3 -> R.drawable.avatar_male
        4 -> R.drawable.avatar_grandpa
        5 -> R.drawable.avatar_girl
        else -> R.drawable.avatar_default
    }
}