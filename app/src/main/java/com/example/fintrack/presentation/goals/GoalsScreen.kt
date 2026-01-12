package com.example.fintrack.presentation.goals

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fintrack.presentation.navigation.AppRoutes

// Custom Colors from your Tailwind Config
val GoalGreen = Color(0xFF2ECC71)
val GoalWarning = Color(0xFFF39C12)
val GoalDanger = Color(0xFFE74C3C)

@Composable
fun GoalsScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    savingViewModel: SavingViewModel = hiltViewModel(),
    debtViewModel: DebtViewModel = hiltViewModel(),
    budgetViewModel: BudgetViewModel = hiltViewModel()
) {
        // State for tracking selected tab
        var selectedTab by remember { mutableStateOf("Budgets") }

        // Collect savings and debts from ViewModels
        val savings by savingViewModel.savings.collectAsStateWithLifecycle()
        val debts by debtViewModel.debts.collectAsStateWithLifecycle()
        val budgets by budgetViewModel.budgets.collectAsStateWithLifecycle()

        LazyColumn(
                modifier =
                        Modifier.fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(paddingValues)
                                .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 100.dp, top = 24.dp)
        ) {
                // --- Header ---
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
                                // Dynamic Action Button based on selected tab
                                Button(
                                        onClick = {
                                                when (selectedTab) {
                                                        "Budgets" ->
                                                                navController.navigate(
                                                                        AppRoutes.AddBudget.route
                                                                )
                                                        "Savings" -> {
                                                                navController.navigate(AppRoutes.AddSaving.route)
                                                        }
                                                        "Debts" -> {
                                                            navController.navigate(AppRoutes.AddDebt.route)
                                                        }
                                                }
                                        },
                                        colors =
                                                ButtonDefaults.buttonColors(
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
                                                text =
                                                        when (selectedTab) {
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

                // --- Segmented Control ---
                item {
                        GoalsSegmentedControl(
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it }
                        )
                }

                // --- Content based on selected tab ---
                when (selectedTab) {
                        "Budgets" -> {
                                // --- Monthly Budgets Section ---
                                item {
                                        val currentMonth = java.text.SimpleDateFormat("MMMM", java.util.Locale.getDefault())
                                            .format(java.util.Calendar.getInstance().time)
                                        SectionHeader(
                                                title = "Monthly Budgets",
                                                badge = currentMonth
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))

                                        if (budgets.isEmpty()) {
                                                // Empty state
                                                Column(
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
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
                                                // Display real budgets
                                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                                        budgets.forEach { budget ->
                                                                // For now, we'll show budgets without spending data
                                                                // In the future, you could add a TransactionRepository call to get spending per category
                                                                val progress = 0.0f // Placeholder - would need transaction data to calculate
                                                                val spent = 0.0 // Placeholder
                                                                val remaining = budget.amount - spent
                                                                val isWarning = progress > 0.7f

                                                                BudgetCard(
                                                                        title = budget.categoryName,
                                                                        subtitle = "$${String.format("%.0f", remaining)} left of $${String.format("%.0f", budget.amount)}",
                                                                        amount = "$${String.format("%.0f", spent)}",
                                                                        icon = getCategoryIcon(budget.categoryName),
                                                                        color = getCategoryColor(budget.categoryName),
                                                                        progress = progress,
                                                                        isWarning = isWarning
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }
                        "Savings" -> {
                                // --- Savings Goals Section ---
                                item {
                                        SectionHeader(
                                                title = "Savings Goals",
                                                actionText = if (savings.isNotEmpty()) "View All" else null
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))

                                        if (savings.isEmpty()) {
                                                // Empty state
                                                Column(
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
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
                                                // Display real savings
                                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                        savings.take(3).forEach { saving ->
                                                                val percentage = (saving.currentAmount / saving.targetAmount).toFloat().coerceIn(0f, 1f)
                                                                SavingsCard(
                                                                        modifier = Modifier.fillMaxWidth(),
                                                                        title = saving.title,
                                                                        target = "Target: $${String.format("%,.0f", saving.targetAmount)}",
                                                                        saved = "$${String.format("%,.0f", saving.currentAmount)}",
                                                                        percentage = percentage,
                                                                        icon = iconFromName(saving.iconName),
                                                                        color = Color(0xFF10B981),
                                                                        onClick = { navController.navigate(AppRoutes.ManageSaving.createRoute(saving.id)) }
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }
                        "Debts" -> {
                                // --- Active Debts Section ---
                                item {
                                        SectionHeader(title = "Active Debts", badge = null)
                                        Spacer(modifier = Modifier.height(16.dp))

                                        if (debts.isEmpty()) {
                                                // Empty state
                                                Column(
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
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
                                                // Display real debts
                                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                        debts.forEach { debt ->
                                                                val daysUntilDue = ((debt.dueDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
                                                                DebtCard(
                                                                        title = debt.title,
                                                                        dueDate = if (daysUntilDue > 0) "Due in $daysUntilDue days" else "Overdue",
                                                                        amount = "$${String.format("%,.2f", debt.currentBalance)}",
                                                                        minPay = "Min: $${String.format("%,.2f", debt.minimumPayment)}",
                                                                        icon = iconFromName(debt.iconName),
                                                                        color = if (daysUntilDue < 7) GoalDanger else GoalWarning,
                                                                        onClick = { navController.navigate(AppRoutes.ManageDebt.createRoute(debt.id)) }
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }
}

// ------------------------------------
// UI COMPONENTS
// ------------------------------------

@Composable
fun GoalsSegmentedControl(selectedTab: String, onTabSelected: (String) -> Unit) {
    val tabs = listOf("Budgets", "Savings", "Debts")

    // Use BoxWithConstraints to get exact width for calculations immediately
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp) // Fixed height for a consistent look
            .padding(vertical = 8.dp) // External padding
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                RoundedCornerShape(12.dp)
            )
            .padding(4.dp) // Internal padding (gap between container and highlight)
    ) {
        val segmentWidth = maxWidth / tabs.size
        val selectedIndex = tabs.indexOf(selectedTab)

        // 1. Animated Highlight Pill
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
                .background(GoalGreen) // Your custom Green Color
        )

        // 2. Tab Items (Overlay)
        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEach { tab ->
                val isSelected = tab == selectedTab

                // Animate text color for better contrast
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
                            // Removing ripple for a cleaner sliding look
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
                                                modifier =
                                                        Modifier.padding(
                                                                horizontal = 8.dp,
                                                                vertical = 4.dp
                                                        ),
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
fun SectionDivider() {
        HorizontalDivider(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                // Note: Jetpack Compose Divider doesn't support "dashed" natively easily without
                // custom
                // draw,
                // simplified to solid line for stability.
                )
}

@Composable
fun BudgetCard(
        title: String,
        subtitle: String,
        amount: String,
        icon: ImageVector,
        color: Color,
        progress: Float,
        isWarning: Boolean = false
) {
        Card(
                colors =
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border =
                        androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        ),
                modifier = Modifier.fillMaxWidth()
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
                                                modifier =
                                                        Modifier.size(40.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                        color.copy(alpha = 0.1f)
                                                                ),
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
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
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
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
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
                colors =
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border =
                        androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
        ) {
                Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        Box(
                                modifier =
                                        Modifier.size(40.dp)
                                                .clip(CircleShape)
                                                .background(color.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                        ) { Icon(imageVector = icon, contentDescription = null, tint = color) }

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
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .height(6.dp)
                                                        .clip(CircleShape),
                                        color = color,
                                        trackColor =
                                                MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                        }
                }
        }
}

@Composable
fun EmergencyFundCard(onClick: () -> Unit = {}) {
        Card(
                modifier = Modifier.clickable { onClick() },
                colors =
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border =
                        androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
        ) {
                Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                        Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Box(
                                        modifier =
                                                Modifier.size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                                Color(0xFFA855F7).copy(alpha = 0.1f)
                                                        ), // Purple
                                        contentAlignment = Alignment.Center
                                ) {
                                        Icon(
                                                imageVector = Icons.Default.HealthAndSafety,
                                                contentDescription = null,
                                                tint = Color(0xFFA855F7)
                                        )
                                }

                                Column {
                                        Text(
                                                text = "Emergency Fund",
                                                fontWeight = FontWeight.SemiBold
                                        )
                                        Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                                LinearProgressIndicator(
                                                        progress = { 0.8f },
                                                        modifier =
                                                                Modifier.width(80.dp)
                                                                        .height(6.dp)
                                                                        .clip(CircleShape),
                                                        color = Color(0xFFA855F7),
                                                        trackColor =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceContainerHighest
                                                )
                                                Text(
                                                        text = "$8k / $10k",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                )
                                        }
                                }
                        }
                        IconButton(onClick = { /* navigate */}, modifier = Modifier.size(32.dp)) {
                                Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                modifier = Modifier.fillMaxWidth().clickable { onClick() },
                colors =
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                        // Left Colored Border
                        Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(color))

                        // Content
                        Row(
                                modifier = Modifier.padding(16.dp).weight(1f),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                        ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Box(
                                                modifier =
                                                        Modifier.size(40.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                        color.copy(alpha = 0.1f)
                                                                ),
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
                                                        verticalAlignment =
                                                                Alignment.CenterVertically,
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(4.dp)
                                                ) {
                                                        Icon(
                                                                imageVector =
                                                                        Icons.Default.CalendarToday,
                                                                contentDescription = null,
                                                                tint = color,
                                                                modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                                text = dueDate,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelSmall,
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
