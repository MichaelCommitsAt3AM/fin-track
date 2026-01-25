package com.fintrack.app.presentation.goals

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fintrack.app.presentation.goals.budgets.BudgetsViewModel
import com.fintrack.app.presentation.goals.debt.DebtViewModel
import com.fintrack.app.presentation.goals.saving.SavingViewModel
import com.fintrack.app.presentation.navigation.AppRoutes
import java.text.SimpleDateFormat
import java.util.*

// Custom Colors
val GoalGreen = Color(0xFF2ECC71)
val GoalWarning = Color(0xFFF39C12)
val GoalDanger = Color(0xFFE74C3C)

@Composable
fun GoalsScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    savingViewModel: SavingViewModel = hiltViewModel(),
    debtViewModel: DebtViewModel = hiltViewModel(),
    budgetViewModel: BudgetsViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf("Budgets") }

    val budgets by budgetViewModel.budgets.collectAsStateWithLifecycle()
    val savings by savingViewModel.savings.collectAsStateWithLifecycle()
    val debts by debtViewModel.debts.collectAsStateWithLifecycle()

    val budgetCurrency by budgetViewModel.currencyPreference.collectAsStateWithLifecycle()
    val savingCurrency by savingViewModel.currencyPreference.collectAsStateWithLifecycle()
    val debtCurrency by debtViewModel.currencyPreference.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 100.dp, top = 24.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Goals",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Button(
                    onClick = {
                        when (selectedTab) {
                            "Budgets" -> navController.navigate(AppRoutes.AddBudget.createRoute())
                            "Savings" -> navController.navigate(AppRoutes.AddSaving.route)
                            "Debts" -> navController.navigate(AppRoutes.AddDebt.route)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoalGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (selectedTab) {
                            "Budgets" -> "Add Budget"
                            "Savings" -> "Add Saving"
                            "Debts" -> "Add Debt"
                            else -> "Add"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Segmented Control
        item {
            GoalsSegmentedControl(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }

        // Content based on selected tab
        when (selectedTab) {
            "Budgets" -> {
                item {
                    val currentMonth = SimpleDateFormat("MMMM", Locale.getDefault())
                        .format(Calendar.getInstance().time)
                    SectionHeader(
                        title = "Monthly Budgets",
                        badge = currentMonth
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (budgets.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No budgets set",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Set a budget to track your spending",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            budgets.forEach { budgetItem ->
                                val budget = budgetItem.budget
                                val spent = budgetItem.spent
                                val progress = budgetItem.progress
                                val remaining = budget.amount - spent
                                val isWarning = progress > 0.7f
                                val currencySymbol = budgetCurrency.symbol

                                BudgetCard(
                                    title = budget.categoryName,
                                    subtitle = "$currencySymbol${String.format("%.0f", remaining)} left of $currencySymbol${String.format("%.0f", budget.amount)}",
                                    amount = "$currencySymbol${String.format("%.0f", spent)}",
                                    icon = getCategoryIcon(budget.categoryName),
                                    color = getCategoryColor(budget.categoryName),
                                    progress = progress,
                                    isWarning = isWarning,
                                    onClick = {
                                        navController.navigate(
                                            AppRoutes.ManageBudget.createRoute(
                                                budget.categoryName,
                                                budget.month,
                                                budget.year
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
            "Savings" -> {
                item {
                    SectionHeader(
                        title = "Savings Goals"
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (savings.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No savings yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Start saving by creating your first goal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            savings.forEach { saving ->
                                val uiSaving = saving.toUiModel()
                                val currencySymbol = savingCurrency.symbol

                                SavingsCard(
                                    title = uiSaving.title,
                                    target = "Target: $currencySymbol${String.format("%.0f", uiSaving.targetAmount)} by ${formatDate(uiSaving.targetDate)}",
                                    saved = "$currencySymbol${String.format("%.0f", uiSaving.currentAmount)}",
                                    percentage = (uiSaving.currentAmount / uiSaving.targetAmount).toFloat(),
                                    icon = uiSaving.icon,
                                    color = uiSaving.color,
                                    onClick = {
                                        navController.navigate(AppRoutes.ManageSaving.createRoute(uiSaving.id))
                                    }
                                )
                            }
                        }
                    }
                }
            }
            "Debts" -> {
                item {
                    SectionHeader(title = "Active Debts", badge = null)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (debts.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No debts tracked",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Add a debt to start tracking payments",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            debts.forEach { debt ->
                                val uiDebt = debt.toUiModel()
                                val currencySymbol = debtCurrency.symbol

                                DebtCard(
                                    title = uiDebt.title,
                                    dueDate = "Due: ${formatDate(uiDebt.dueDate)}",
                                    amount = "$currencySymbol${String.format("%.0f", uiDebt.currentBalance)}",
                                    minPay = "Min: $currencySymbol${String.format("%.0f", uiDebt.minimumPayment)}",
                                    icon = uiDebt.icon,
                                    color = uiDebt.color,
                                    onClick = {
                                        navController.navigate(AppRoutes.ManageDebt.createRoute(uiDebt.id))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoalsSegmentedControl(selectedTab: String, onTabSelected: (String) -> Unit) {
    val tabs = listOf("Budgets", "Savings", "Debts")

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                RoundedCornerShape(12.dp)
            )
            .padding(4.dp)
    ) {
        val segmentWidth = maxWidth / tabs.size
        val selectedIndex = tabs.indexOf(selectedTab)

        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "indicatorOffset"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(GoalGreen)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEach { tab ->
                val isSelected = tab == selectedTab

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 300),
                    label = "textColor"
                )

                Box(
                    modifier = Modifier
                        .width(segmentWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, badge: String? = null, actionText: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (badge != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (actionText != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelLarge,
                color = GoalGreen,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {}
            )
        }
    }
}

@Composable
fun BudgetCard(
    title: String,
    subtitle: String,
    amount: String,
    icon: ImageVector,
    color: Color,
    progress: Float,
    isWarning: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = amount,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (isWarning) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = GoalWarning,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Nearing limit",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoalWarning
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (isWarning) GoalWarning else color,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
    }
}

@Composable
fun SavingsCard(
    modifier: Modifier = Modifier,
    title: String,
    target: String,
    saved: String,
    percentage: Float,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color)
            }

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = target,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoalGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = saved,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { percentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }
        }
    }
}

@Composable
fun DebtCard(
    title: String,
    dueDate: String,
    amount: String,
    minPay: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .background(color))

            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color
                        )
                    }
                    Column {
                        Text(text = title, fontWeight = FontWeight.SemiBold)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = dueDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = color,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = amount,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = minPay,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
