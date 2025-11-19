package com.example.fintrack.presentation.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fintrack.presentation.ui.theme.*

@Composable
fun ReportsScreen(navController: NavController) {
    // Dummy Data
    val weeklyData = listOf(0.6f, 0.4f, 0.75f, 0.3f, 0.85f, 0.5f, 0.2f) // Percentage heights
    val categoryData = listOf(
        CategoryReport("Food & Drinks", "Ksh 345.12", 0.55f, Icons.Default.Restaurant, SpendingFood),
        CategoryReport("Shopping", "Ksh 1,204.99", 0.85f, Icons.Default.ShoppingBag, SpendingShopping),
        CategoryReport("Bills & Utilities", "Ksh 450.00", 0.65f, Icons.Default.ReceiptLong, SpendingBills),
        CategoryReport("Transportation", "Ksh 88.50", 0.30f, Icons.Default.DirectionsBus, SpendingTransport)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 32.dp, top = 24.dp)
    ) {
        item { Header() }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { SpendingTrendsChart(weeklyData) }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { MonthlySummaryCard() }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { CategoriesList(categoryData) }
    }
}

@Composable
fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Reports",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        IconButton(
            onClick = { /* TODO: Date Picker */ },
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "Select Date",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SpendingTrendsChart(data: List<Float>) {
    var selectedTab by remember { mutableStateOf("Weekly") }
    val days = listOf("M", "T", "W", "T", "F", "S", "S")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header + Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spending Trends",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Toggle Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(4.dp)
                ) {
                    listOf("Weekly", "Monthly").forEach { text ->
                        val isSelected = selectedTab == text
                        val bgColor = if (isSelected) MaterialTheme.colorScheme.surfaceContainerLow else Color.Transparent
                        val textColor = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(bgColor)
                                .clickable { selectedTab = text }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bar Chart Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp), // Fixed height for chart
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEachIndexed { index, percentage ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        // The Bar
                        Box(
                            modifier = Modifier
                                .width(12.dp) // w-3
                                .fillMaxHeight(percentage) // dynamic height
                                .clip(RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp))
                                .background(MaterialTheme.colorScheme.error) // using Error color for expense (Red)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // The Label
                        Text(
                            text = days.getOrElse(index) { "" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlySummaryCard() {
    val income = 4500.00
    val expense = 2145.50
    val expensePercentage = (expense / (income + expense)).toFloat()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Monthly Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Income Box
                SummaryBox(
                    label = "Income",
                    amount = "Ksh4,500.00",
                    amountColor = FinTrackGreen,
                    modifier = Modifier.weight(1f)
                )
                // Expense Box
                SummaryBox(
                    label = "Expense",
                    amount = "Ksh2,145.50",
                    amountColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(FinTrackGreen) // Background is Income
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(expensePercentage) // Percentage of expense
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.error) // Foreground is Expense
                )
            }
        }
    }
}

@Composable
fun SummaryBox(label: String, amount: String, amountColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

@Composable
fun CategoriesList(categories: List<CategoryReport>) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Spending by Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = { /* TODO */ }) {
                Text("View All", color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            categories.forEach { category ->
                CategoryRow(category)
            }
        }
    }
}

@Composable
fun CategoryRow(category: CategoryReport) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(category.color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.name,
                tint = category.color
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = category.amount,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            // Progress Bar
            LinearProgressIndicator(
                progress = { category.percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = category.color,
                trackColor = MaterialTheme.colorScheme.background,
            )
        }
    }
}

data class CategoryReport(
    val name: String,
    val amount: String,
    val percentage: Float,
    val icon: ImageVector,
    val color: Color
)