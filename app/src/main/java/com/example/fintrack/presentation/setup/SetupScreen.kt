package com.example.fintrack.presentation.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SetupScreen(
    onNavigateToHome: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SetupEvent.NavigateToHome -> onNavigateToHome()
                is SetupEvent.ShowError -> {
                    // Error is already shown in UI via uiState.error
                }
            }
        }
    }

    // Start setup when screen loads
    LaunchedEffect(Unit) {
        viewModel.startSetup()
    }

    SetupScreenContent(
        uiState = uiState,
        onRetry = { viewModel.retrySetup() }
    )
}

@Composable
private fun SetupScreenContent(
    uiState: SetupUiState,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (uiState.error != null) {
                // Error State
                ErrorContent(
                    error = uiState.error,
                    onRetry = onRetry
                )
            } else {
                // Loading State
                LoadingContent(
                    currentStep = uiState.currentStep,
                    progress = uiState.progress
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(
    currentStep: String,
    progress: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // App Icon or Logo (optional)
        Text(
            text = "FinTrack",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Progress Indicator
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(64.dp),
            strokeWidth = 4.dp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Current Step Text
        Text(
            text = currentStep,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        // Progress Percentage
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Setup Failed",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(0.5f)
        ) {
            Text("Retry")
        }
    }
}
