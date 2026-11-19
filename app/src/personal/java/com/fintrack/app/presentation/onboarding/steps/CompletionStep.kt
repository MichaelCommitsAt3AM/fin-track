package com.fintrack.app.presentation.onboarding.steps

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fintrack.app.presentation.ui.theme.FinTrackGreen

/**
 * Completion step: Confirms successful onboarding setup.
 */
@Composable
fun CompletionStep(
    onComplete: () -> Unit
) {
    OnboardingStepLayout(
        horizontalAlignment = Alignment.CenterHorizontally,
        bottomBar = {
            Button(
                onClick = onComplete,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Start Using FinTrack", fontWeight = FontWeight.Bold)
            }
        }
    ) {
        Spacer(modifier = Modifier.height(64.dp))

        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = FinTrackGreen
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Setup Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your M-Pesa transactions are now being tracked automatically.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
