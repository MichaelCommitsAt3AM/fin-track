package com.example.fintrack.presentation.onboarding.steps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fintrack.presentation.onboarding.MpesaOnboardingViewModel
import com.example.fintrack.presentation.ui.theme.FinTrackGreen

/**
 * Insights Step: Displays analysis of transaction patterns.
 */
@Composable
fun InsightsStep(
    viewModel: MpesaOnboardingViewModel,
    onNext: () -> Unit
) {
    val insights by viewModel.insights.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Your Patterns",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "We found some interesting habits in your ${insights.totalTransactions} transactions.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Frequent Merchants Section
        if (insights.frequentMerchants.isNotEmpty()) {
            Text(
                text = "Top Places You Spend",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            insights.frequentMerchants.forEach { merchant ->
                InsightCard(
                    icon = Icons.Default.ShoppingCart,
                    title = merchant.merchantName,
                    subtitle = "${merchant.transactionCount} transactions",
                    amount = "KES ${merchant.totalAmount.toInt()}",
                    badge = merchant.suggestedCategory
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Recurring Bills Section
        if (insights.recurringPaybills.isNotEmpty()) {
            Text(
                text = "Recurring Bills",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            insights.recurringPaybills.forEach { bill ->
                InsightCard(
                    icon = Icons.Default.Repeat,
                    title = bill.merchantName ?: "Paybill ${bill.paybillNumber}",
                    subtitle = "Found ${bill.frequency} times",
                    amount = "~ KES ${bill.averageAmount.toInt()}",
                    badge = bill.suggestedCategory
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        if (insights.frequentMerchants.isEmpty() && insights.recurringPaybills.isEmpty()) {
            // Empty state
            Text(
                text = "No strong patterns detected yet. As you use M-Pesa more, we'll learn!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next")
        }
    }
}

@Composable
private fun InsightCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    amount: String,
    badge: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FinTrackGreen,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (badge != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            Text(
                text = amount,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
