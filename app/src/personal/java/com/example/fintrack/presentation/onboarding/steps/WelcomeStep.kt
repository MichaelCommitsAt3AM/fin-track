package com.example.fintrack.presentation.onboarding.steps

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
import com.example.fintrack.presentation.ui.theme.FinTrackGreen

/**
 * Welcome step: Introduces M-Pesa integration with privacy focus.
 */
@Composable
fun WelcomeStep(
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // Icon
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = FinTrackGreen
        )
        
        // Title & Description
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "M-Pesa Integration",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Track your M-Pesa transactions automatically. Your data stays on your device — never uploaded to the cloud.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Privacy features
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PrivacyFeature(
                icon = Icons.Default.Lock,
                title = "Local Storage Only",
                description = "Data never leaves your device"
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
        
        // Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Get Started")
            }
            
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for Now")
            }
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
