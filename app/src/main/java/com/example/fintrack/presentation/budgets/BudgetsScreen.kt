package com.example.fintrack.presentation.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun BudgetsScreen(
    paddingValues: PaddingValues,
    onNavigateToAddBudget: () -> Unit,
    viewModel: BudgetsViewModel = hiltViewModel()
) {

    val state by viewModel.budgetListState.collectAsState()

    // Dummy data for budgets
    val budgets = listOf(
        BudgetItem(
            category = "Shopping",
            spent = 250.0,
            total = 1000.0,
            icon = Icons.Default.ShoppingBag,
            color = Color(0xFF6366F1) // Indigo
        ),
        BudgetItem(
            category = "Food & Dining",
            spent = 420.0,
            total = 600.0,
            icon = Icons.Default.Restaurant,
            color = Color(0xFFF97316), // Orange
            warning = "You're nearing your budget!"
        ),
        BudgetItem(
            category = "Transportation",
            spent = 255.0,
            total = 200.0,
            icon = Icons.Default.DirectionsBus,
            color = Color(0xFFEF4444), // Red
            warning = "You've exceeded your budget!",
            isOverBudget = true
        ),
        BudgetItem(
            category = "Utilities",
            spent = 125.0,
            total = 300.0,
            icon = Icons.Default.Receipt,
            color = Color(0xFF0EA5E9) // Sky
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 16.dp, top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budgets",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(
                    onClick = { onNavigateToAddBudget() }, // Add this callback
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Budget",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            }
        }

        // Monthly Budgets Section
        item {
            Text(
                text = "Monthly Budgets",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(budgets) { budget ->
            BudgetCard(budget = budget)
        }
    }
}

@Composable
fun BudgetCard(budget: BudgetItem) {
    val progress = (budget.spent / budget.total).toFloat().coerceIn(0f, 1f)
    val remaining = budget.total - budget.spent
    val isNearingBudget = progress >= 0.7 && !budget.isOverBudget

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(budget.color.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = budget.icon,
                            contentDescription = null,
                            tint = budget.color
                        )
                    }
                    Column {
                        Text(
                            text = budget.category,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (budget.isOverBudget) {
                                "Ksh ${String.format("%.0f", -remaining)} over budget!"
                            } else {
                                "Ksh ${String.format("%.0f", remaining)} left of Ksh ${String.format("%.0f", budget.total)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = if (budget.isOverBudget) {
                        "-Ksh ${String.format("%.0f", -remaining)}"
                    } else {
                        "Ksh ${String.format("%.0f", budget.spent)}"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (budget.isOverBudget) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                )
            }

            // Warning/Error message
            if (budget.warning != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (budget.isOverBudget) Icons.Default.Error else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (budget.isOverBudget) Color(0xFFEF4444) else Color(0xFFF59E0B),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = budget.warning,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (budget.isOverBudget) Color(0xFFEF4444) else Color(0xFFF59E0B)
                    )
                }
            }

            // Progress Bar
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    budget.isOverBudget -> Color(0xFFEF4444)
                    isNearingBudget -> Color(0xFFF59E0B)
                    else -> budget.color
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

// Data class
data class BudgetItem(
    val category: String,
    val spent: Double,
    val total: Double,
    val icon: ImageVector,
    val color: Color,
    val warning: String? = null,
    val isOverBudget: Boolean = false
)
