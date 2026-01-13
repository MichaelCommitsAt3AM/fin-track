package com.example.fintrack.presentation.settings.security

import android.app.Activity
import com.example.fintrack.core.util.AppLogger
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fintrack.R
import com.example.fintrack.presentation.navigation.AppRoutes
import com.example.fintrack.presentation.settings.SettingsSection
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSignInMethodsScreen(
    onNavigateBack: () -> Unit,
    navController: NavController,
    viewModel: ManageSignInMethodsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // --- Google Sign-In Launcher ---
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                // Google Sign In was successful
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { idToken ->
                    // Pass ID Token AND Email for strict verification in ViewModel
                    viewModel.linkGoogle(idToken, account.email)
                }
            } catch (e: ApiException) {
                AppLogger.w("ManageSignIn", "Google sign in failed", e)
                // Optionally handle specific Google API errors here
            }
        }
    }

    // --- Actions ---

    // Trigger Google Sign-In Flow
    val onLinkGoogleClick = {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id)) // Ensure this resource exists
            .requestEmail()
            // Hint the current email to Google to encourage selecting the matching account
            .apply {
                if (uiState.email.isNotEmpty() && uiState.email != "No email linked") {
                    setAccountName(uiState.email)
                }
            }
            .build()

        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        // Sign out of Google first to allow account selection if multiple exist
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    // Logic for "Edit Email": If user has no password, they must set one first.
    val onEditEmailClick = {
        if (!uiState.isEmailPasswordLinked) {
            navController.navigate(AppRoutes.SetPassword.route)
        } else {
            // TODO: Navigate to Edit Email Screen (future implementation)
        }
    }

    // Logic for "Unlink Google": If user has no password, they must set one first.
    val onUnlinkGoogleClick = {
        if (!uiState.isEmailPasswordLinked) {
            navController.navigate(AppRoutes.SetPassword.route)
        } else {
            viewModel.unlinkGoogle()
        }
    }

    // --- Effects ---

    // Show Snackbar for errors
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    // --- UI Content ---
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Sign-in Methods",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = "Manage your sign-in options. You can use any linked method to access your FinTrack account securely.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- Email & Password Section ---
                SettingsSection(title = "Email & Password") {
                    // Email Item
                    SignInMethodItem(
                        icon = Icons.Default.Email,
                        title = "Email Address",
                        subtitle = uiState.email,
                        actionButton = {
                            OutlinedButton(
                                onClick = { onEditEmailClick() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp),
                                enabled = !uiState.isLoading
                            ) {
                                Text("Edit", fontSize = 12.sp)
                            }
                        }
                    )

                    // Show Password item only if it exists (linked)
                    if (uiState.isEmailPasswordLinked) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SignInMethodItem(
                            icon = Icons.Default.Key,
                            title = "Password",
                            subtitle = "••••••••••••",
                            actionButton = {
                                OutlinedButton(
                                    onClick = { /* TODO: Navigate to Change Password */ },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp),
                                    enabled = !uiState.isLoading
                                ) {
                                    Text("Change", fontSize = 12.sp)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Connected Accounts Section ---
                SettingsSection(title = "Connected Accounts") {
                    // Google Item
                    SignInMethodItem(
                        icon = Icons.Default.Public,
                        title = "Google",
                        subtitle = if (uiState.isGoogleLinked) null else "Not connected",
                        subtitleContent = if (uiState.isGoogleLinked) {
                            {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Connected",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else null,
                        actionButton = {
                            if (uiState.isGoogleLinked) {
                                TextButton(
                                    onClick = { onUnlinkGoogleClick() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp),
                                    enabled = !uiState.isLoading
                                ) {
                                    Text("Unlink", fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = { onLinkGoogleClick() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(0.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp),
                                    enabled = !uiState.isLoading
                                ) {
                                    Text("Link", fontSize = 12.sp)
                                }
                            }
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Apple Item (Placeholder for future)
                    /*
                    SignInMethodItem(
                        icon = Icons.Default.PhoneIphone,
                        title = "Apple",
                        subtitle = if (uiState.isAppleLinked) null else "Not connected",
                        subtitleContent = if (uiState.isAppleLinked) {
                            {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Connected",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else null,
                        actionButton = {
                            if (uiState.isAppleLinked) {
                                TextButton(
                                    onClick = { /* TODO: Unlink Apple */ },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp),
                                    enabled = !uiState.isLoading
                                ) {
                                    Text("Unlink", fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.linkApple() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(0.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp),
                                    enabled = !uiState.isLoading
                                ) {
                                    Text("Link", fontSize = 12.sp)
                                }
                            }
                        }
                    )
                    */
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- Security Note ---
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "For security, FinTrack may verify your identity before allowing you to link or unlink accounts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            // --- Global Loading Overlay ---
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

// --- Reusable Component ---

@Composable
fun SignInMethodItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    subtitleContent: (@Composable () -> Unit)? = null,
    actionButton: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
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

            // Text Content
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitleContent != null) {
                    subtitleContent()
                } else if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Action Button
        actionButton()
    }
}