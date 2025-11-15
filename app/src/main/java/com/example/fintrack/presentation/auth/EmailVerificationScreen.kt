package com.example.fintrack.presentation.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fintrack.presentation.ui.theme.FinTrackGreen

@Composable
fun EmailVerificationScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val cooldownSeconds by viewModel.verificationCooldownSeconds.collectAsState()
    val attemptsRemaining by viewModel.verificationAttemptsRemaining.collectAsState()

    // Check verification status when screen loads
    LaunchedEffect(Unit) {
        viewModel.checkEmailVerification()
    }

    // Listen for navigation events
    LaunchedEffect(key1 = true) {
        viewModel.authEvent.collect { event ->
            when (event) {
                is AuthEvent.NavigateToHome -> onNavigateToHome()
                is AuthEvent.NavigateToLogin -> onNavigateToLogin()
                is AuthEvent.NavigateToEmailVerification -> {}
            }
        }
    }

    // Show error toast
    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = "Email Sent",
            tint = FinTrackGreen,
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Verify your Email",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "We've sent a verification link to your email address. Please check your inbox and click the link to verify your account.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Check Status Button
        Button(
            onClick = { viewModel.checkEmailVerification() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FinTrackGreen)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("I've clicked the link")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Resend Button with Rate Limiting
        OutlinedButton(
            onClick = { viewModel.onEvent(AuthUiEvent.ResendVerificationEmail) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = cooldownSeconds == 0L && attemptsRemaining > 0
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            when {
                cooldownSeconds > 0 -> Text("Resend in ${cooldownSeconds}s")
                attemptsRemaining == 0 -> Text("Max attempts reached")
                else -> Text("Resend Email")
            }
        }

        // Rate limit info
        if (attemptsRemaining < 5 && attemptsRemaining > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$attemptsRemaining attempts remaining",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = {
            viewModel.resetRateLimiter()
            viewModel.signOut()
        }) {
            Text("Sign Out", color = MaterialTheme.colorScheme.error)
        }
    }
}
