package com.fintrack.app.presentation.onboarding.steps

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fintrack.app.presentation.ui.theme.FinTrackGreen

/**
 * Welcome step: Introduces M-Pesa integration with privacy focus.
 */
@Composable
fun WelcomeStep(
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    OnboardingStepLayout(
        horizontalAlignment = Alignment.CenterHorizontally,
        bottomBar = {
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Get Started", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text("Skip for Now")
            }
        }
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = FinTrackGreen
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "M-Pesa Integration",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Track your M-Pesa transactions automatically. Your data stays on your device unless you turn on encrypted Cloud backup.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PrivacyFeature(
                icon = Icons.Default.Lock,
                title = "Local Storage Only",
                description = "Stays on your device unless you opt in to backup"
            )
            PrivacyFeature(
                icon = Icons.Default.Shield,
                title = "Separate Database",
                description = "M-Pesa data isolated and secure"
            )
            PrivacyFeature(
                icon = Icons.Default.AutoAwesome,
                title = "Smart Detection",
                description = "Auto-categorize transactions"
            )
        }
    }
}

@Composable
private fun PrivacyFeature(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = FinTrackGreen
        )
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
