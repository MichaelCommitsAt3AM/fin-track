package com.example.fintrack.presentation.onboarding.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fintrack.core.domain.model.CategorySuggestion
import com.example.fintrack.presentation.onboarding.MpesaOnboardingViewModel
import com.example.fintrack.presentation.settings.getIconByName
import com.example.fintrack.presentation.ui.theme.FinTrackGreen

import androidx.compose.runtime.LaunchedEffect

/**
 * Step to review and accept auto-generated category suggestions.
 */
@Composable
fun CategorySuggestionsStep(
    viewModel: MpesaOnboardingViewModel,
    onNext: () -> Unit
) {
    val insights by viewModel.insights.collectAsState()
    val suggestions = insights.categorySuggestions

    LaunchedEffect(Unit) {
        viewModel.logCategoryDebugInfo()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            CategorySuggestionsContent(suggestions, viewModel, onNext)
        }
    }
}

@Composable
private fun ColumnScope.CategorySuggestionsContent(
    suggestions: List<CategorySuggestion>,
    viewModel: MpesaOnboardingViewModel,
    onNext: () -> Unit
) {
    Text(
        text = "Auto-Organize",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
    )

    Text(
        text = "We found ${suggestions.size} categories based on your spending patterns. We can create these for you automatically.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    if (suggestions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No category patterns detected yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        suggestions.forEach { suggestion ->
            CategorySuggestionCard(suggestion)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Accept All Button
    Button(
        onClick = { viewModel.acceptCategorySuggestions() },
        modifier = Modifier.fillMaxWidth(),
        enabled = suggestions.isNotEmpty(),
        colors = ButtonDefaults.buttonColors(
            containerColor = FinTrackGreen
        )
    ) {
        Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Create ${suggestions.size} Categories")
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Skip Button
    OutlinedButton(
        onClick = onNext,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Skip for Now")
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "You can always manage categories later in Settings.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
}

@Composable
fun CategorySuggestionCard(suggestion: CategorySuggestion) {
    val icon = getIconByName(suggestion.iconName)
    val color = try {
        Color(android.graphics.Color.parseColor(suggestion.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = suggestion.categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${suggestion.transactionCount} transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "KES ${suggestion.totalAmount.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Checked indicator
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = FinTrackGreen,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
