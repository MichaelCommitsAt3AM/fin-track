package com.fintrack.app.presentation.onboarding.steps

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fintrack.app.core.domain.model.LookbackPeriod
import com.fintrack.app.core.domain.model.onboarding.SyncProgress
import com.fintrack.app.presentation.onboarding.MpesaOnboardingViewModel
import com.fintrack.app.presentation.ui.theme.FinTrackGreen

/**
 * Syncing step: Displays progress while scanning M-Pesa SMS messages.
 */
@Composable
fun SyncingStep(
    viewModel: MpesaOnboardingViewModel
) {
    val syncProgress by viewModel.syncProgress.collectAsState()
    val lookbackPeriod by viewModel.selectedLookbackPeriod.collectAsState()
    
    // Start sync when step is first displayed
    LaunchedEffect(Unit) {
        viewModel.startInitialSync(lookbackPeriod)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (syncProgress.isComplete) "Scan Complete!" else "Scanning Transactions",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = when (lookbackPeriod) {
                LookbackPeriod.ONE_MONTH -> "Looking back 1 month"
                LookbackPeriod.THREE_MONTHS -> "Looking back 3 months"
                LookbackPeriod.SIX_MONTHS -> "Looking back 6 months"
                LookbackPeriod.ONE_YEAR -> "Looking back 1 year"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Progress indicator
        if (!syncProgress.isComplete) {
            CircularProgressIndicator(
                modifier = Modifier.size(100.dp),
                strokeWidth = 8.dp,
                color = FinTrackGreen
            )
        } else {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = FinTrackGreen
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Status text
        Text(
            text = syncProgress.status,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = if (syncProgress.isComplete) 
                FinTrackGreen 
            else 
                MaterialTheme.colorScheme.onSurface
        )
        
        // Transaction count
        if (syncProgress.parsedTransactions > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "${syncProgress.parsedTransactions} transactions found",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = FinTrackGreen
            )
        }
        
        // Info card
        if (!syncProgress.isComplete) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "This may take a few moments",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "We're analyzing your M-Pesa message history",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
