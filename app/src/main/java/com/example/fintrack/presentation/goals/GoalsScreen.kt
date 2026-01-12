package com.example.fintrack.presentation.goals

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fintrack.presentation.navigation.AppRoutes

// Custom Colors from your Tailwind Config
val GoalGreen = Color(0xFF2ECC71)
val GoalWarning = Color(0xFFF39C12)
val GoalDanger = Color(0xFFE74C3C)

@Composable
fun GoalsScreen(navController: NavController, paddingValues: PaddingValues) {
        // State for tracking selected tab
        var selectedTab by remember { mutableStateOf("Budgets") }

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
                                                                /* TODO: Navigate to Add Saving */
                                                        }
                                                        "Debts" -> {
                                                                /* TODO: Navigate to Add Debt */
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
                                        SectionHeader(
                                                title = "Monthly Budgets",
                                                badge = "September"
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                                BudgetCard(
                                                        title = "Shopping",
                                                        subtitle = "$750 left of $1,000",
                                                        amount = "$250",
                                                        icon = Icons.Default.ShoppingBag,
                                                        color = Color(0xFF6366F1), // Indigo
                                                        progress = 0.25f
                                                )
                                                BudgetCard(
                                                        title = "Food & Dining",
                                                        subtitle = "$180 left of $600",
                                                        amount = "$420",
                                                        icon = Icons.Default.Restaurant,
                                                        color = Color(0xFFF97316), // Orange
                                                        progress = 0.7f,
                                                        isWarning = true
                                                )
                                        }
                                }
                        }
                        "Savings" -> {
                                // --- Savings Goals Section ---
                                item {
                                        SectionHeader(
                                                title = "Savings Goals",
                                                actionText = "View All"
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Grid Row
                                        Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                        ) {
                                                SavingsCard(
                                                        modifier = Modifier.weight(1f),
                                                        title = "New Laptop",
                                                        target = "Target: $2,000",
                                                        saved = "$1,200",
                                                        percentage = 0.6f,
                                                        icon = Icons.Default.Computer,
                                                        color = Color(0xFF10B981) // Emerald
                                                )
                                                SavingsCard(
                                                        modifier = Modifier.weight(1f),
                                                        title = "Vacation",
                                                        target = "Target: $3,500",
                                                        saved = "$875",
                                                        percentage = 0.25f,
                                                        icon = Icons.Default.BeachAccess,
                                                        color = Color(0xFF3B82F6) // Blue
                                                )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Full Width Savings Card
                                        EmergencyFundCard()
                                }
                        }
                        "Debts" -> {
                                // --- Active Debts Section ---
                                item {
                                        SectionHeader(title = "Active Debts", badge = null)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                DebtCard(
                                                        title = "Student Loan",
                                                        dueDate = "Due Oct 15",
                                                        amount = "$5,400.00",
                                                        minPay = "Min: $250.00",
                                                        icon = Icons.Default.School,
                                                        color = GoalDanger
                                                )
                                                DebtCard(
                                                        title = "Credit Card",
                                                        dueDate = "Due Oct 05",
                                                        amount = "$1,250.00",
                                                        minPay = "Min: $45.00",
                                                        icon = Icons.Default.CreditCard,
                                                        color = GoalWarning
                                                )
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
    val density = LocalDensity.current
    var boxWidthPx by remember { mutableStateOf(0) }

    // 1. Define the animation spec for a "smooth" glide
    val animationSpec = tween<Dp>(
        durationMillis = 300,
        easing = androidx.compose.animation.core.FastOutSlowInEasing
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp) // Fixed height for better touch targets
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                RoundedCornerShape(12.dp)
            )
            .onSizeChanged { boxWidthPx = it.width }
    ) {
        // 2. Calculate the exact width of one segment
        val itemWidth = with(density) { (boxWidthPx / tabs.size).toDp() }
        val selectedIndex = tabs.indexOf(selectedTab)

        // 3. Animate the X offset of the Green Highlight
        val highlightOffset by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = animationSpec,
            label = "highlight_offset"
        )

        // The Sliding Green Highlight Box
        if (boxWidthPx > 0) {
            Box(
                modifier = Modifier
                    .width(itemWidth)
                    .fillMaxHeight()
                    .offset(x = highlightOffset)
                    .padding(4.dp) // Creates the floating effect inside the container
                    .clip(RoundedCornerShape(8.dp))
                    .background(GoalGreen) // <--- The Green Highlight
            )
        }

        // The Text Labels
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = tab == selectedTab

                // 4. Smoothly animate the text color so it fades to white as the box arrives
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 300),
                    label = "text_color"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onTabSelected(tab)
                        },


                            contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor // Applied animated color here
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
        color: Color
) {
        Card(
                modifier = modifier,
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
fun EmergencyFundCard() {
        Card(
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
        color: Color
) {
        Card(
                colors =
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
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
