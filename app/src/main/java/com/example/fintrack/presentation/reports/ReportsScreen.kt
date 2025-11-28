package com.example.fintrack.presentation.reports

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fintrack.presentation.ui.theme.*

@Composable
fun ReportsScreen(
    navController: NavController,
    paddingValues: PaddingValues,
) {
    // Dummy Data
    val categoryData = listOf(
        CategoryReport("Food & Drinks", "Ksh 345.12", 0.30f, Icons.Default.Restaurant, SpendingFood),
        CategoryReport("Shopping", "Ksh 450.99", 0.25f, Icons.Default.ShoppingBag, SpendingShopping),
        CategoryReport("Bills & Utilities", "Ksh 350.00", 0.25f, Icons.Default.ReceiptLong, SpendingBills),
        CategoryReport("Transportation", "Ksh 234.50", 0.20f, Icons.Default.DirectionsBus, SpendingTransport)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 32.dp, top = 24.dp)
    ) {
        item { Header() }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { SpendingBreakdownPieChart(categoryData) }
        item { Spacer(modifier = Modifier.height(24.dp)) }
//        item { SpendingTrendsChart(weeklyData) } // COMMENTED OUT FOR LATER
//        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { MonthlySummaryCard() }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { CategoriesList(categoryData) }
    }
}

@Composable
fun SpendingBreakdownPieChart(categories: List<CategoryReport>) {
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200)
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Spending Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Pie Chart
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasSize = size.minDimension
                    val strokeWidth = 40f

                    var currentAngle = -90f

                    categories.forEach { category ->
                        val sweepAngle = 360f * category.percentage * animationProgress.value

                        // Draw arc
                        drawArc(
                            color = category.color,
                            startAngle = currentAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidth),
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                            size = Size(canvasSize - strokeWidth, canvasSize - strokeWidth)
                        )

                        currentAngle += sweepAngle
                    }
                }

                // Center Text
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Ksh 1,380.61",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Legend
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEach { category ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(category.color)
                            )
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = "${(category.percentage * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
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
                    .background(FinTrackGreen)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(expensePercentage)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.error)
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
