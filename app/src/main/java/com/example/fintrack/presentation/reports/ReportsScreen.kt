package com.example.fintrack.presentation.reports

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// -------------------------------------------------------------------------
// Helper for consistent borders
// -------------------------------------------------------------------------
@Composable
fun getCardBorder(): BorderStroke {
    return BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

@Composable
fun ReportsScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. Header
        item {
            ReportHeader(
                currentMonth = state.selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                onPrevClick = { viewModel.previousMonth() },
                onNextClick = { viewModel.nextMonth() }
            )
        }

        if (state.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            // 2. Summary Grid (Total Spent & Net Income)
            item {
                SummaryGridSection(
                    totalExpense = state.totalExpense,
                    totalIncome = state.totalIncome
                )
            }

            // 3. Spending Trends (Updated Custom Chart)
            if (state.monthlyTrends.isNotEmpty()) {
                item {
                    SpendingTrendsSection(monthlyData = state.monthlyTrends)
                }
            }

            // 4. Category Breakdown (Donut Chart)
            item {
                CategoryBreakdownSection(
                    categories = state.categoryBreakdown,
                    totalExpense = state.totalExpense
                )
            }

            // 5. Budget/Category Utilization (Progress Bars)
            item {
                CategoryUtilizationSection(categories = state.categoryBreakdown)
            }

            // 6. Export Button
            item {
                ExportButtonSection()
            }
        }
    }
}

// -------------------------------------------------------------------------
// UI Components
// -------------------------------------------------------------------------

@Composable
fun ReportHeader(currentMonth: String, onPrevClick: () -> Unit, onNextClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Reports",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground
        )

        // "This Month" Selector
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface,
            border = getCardBorder(),
            modifier = Modifier.height(36.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                IconButton(onClick = onPrevClick, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        "Prev",
                        modifier = Modifier.size(14.dp)
                    )
                }

                Text(
                    text = currentMonth,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                IconButton(onClick = onNextClick, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        "Next",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryGridSection(totalExpense: Double, totalIncome: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Total Spent Card
        SummaryCard(
            title = "Total Spent",
            amount = totalExpense,
            isIncome = false,
            modifier = Modifier.weight(1f)
        )

        // Net Income Card (Income - Expense)
        val netIncome = totalIncome - totalExpense
        SummaryCard(
            title = "Net Income",
            amount = netIncome,
            isIncome = true,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SummaryCard(title: String, amount: Double, isIncome: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = getCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isIncome) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIncome) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${if (isIncome && amount > 0) "+" else ""}$${
                    String.format(
                        Locale.US,
                        "%,.0f",
                        amount
                    )
                }",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// -------------------------------------------------------------------------
// NEW: Spending Trends Section matching the image
// -------------------------------------------------------------------------

@Composable
fun SpendingTrendsSection(monthlyData: List<MonthlyFinancials>) {
    // 1. Calculations
    val totalExpense = monthlyData.sumOf { it.expense }
    val days = if (monthlyData.isNotEmpty()) monthlyData.size else 1
    val dailyAvg = if (days > 0) totalExpense / days else 0.0

    // Extract strictly the expense data points
    val dataPoints = monthlyData.map { it.expense.toFloat() }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = getCardBorder(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // --- Header Row (Title & Badge) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Spending Trends",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Daily average: $${String.format("%.2f", dailyAvg)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Percentage Badge
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "12%", // In a real app, calculate this percentage diff
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- The Custom Chart ---
                TrendChart(
                    data = dataPoints,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    graphColor = MaterialTheme.colorScheme.primary,
                    labels = listOf("Oct 1", "Oct 8", "Oct 15", "Oct 22", "Today") // Replace with real date logic if needed
                )
            }
        }
    }
}

@Composable
fun TrendChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    graphColor: Color,
    labels: List<String>
) {
    if (data.isEmpty()) return

    val onSurface = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface
    val dashedLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val chartHeight = height - 40.dp.toPx() // Leave room for text labels at bottom

        // 1. Data Normalization
        val maxVal = data.maxOrNull() ?: 1f
        val minVal = data.minOrNull() ?: 0f
        val range = (maxVal - minVal).coerceAtLeast(1f) // Avoid divide by zero

        // Helper to get Y coordinate (inverted because Canvas Y=0 is top)
        fun getY(value: Float): Float {
            val normalized = (value - minVal) / range
            // Leave some padding top/bottom so points aren't cut off
            val verticalPadding = 20.dp.toPx()
            val availableHeight = chartHeight - (verticalPadding * 2)
            return chartHeight - (normalized * availableHeight) - verticalPadding
        }

        // Helper to get X coordinate
        val spacingX = width / (data.size - 1).coerceAtLeast(1)
        fun getX(index: Int): Float = index * spacingX

        // 2. Draw Dashed Horizontal Grid Lines
        val gridLines = 3
        val gridPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        for (i in 0..gridLines) {
            val y = (chartHeight / gridLines) * i
            drawLine(
                color = dashedLineColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = gridPathEffect
            )
        }

        // 3. Create Smooth Bezier Path
        val path = Path()
        val fillPath = Path() // For the gradient area

        if (data.isNotEmpty()) {
            val firstX = getX(0)
            val firstY = getY(data[0])
            path.moveTo(firstX, firstY)
            fillPath.moveTo(firstX, firstY)

            for (i in 0 until data.size - 1) {
                val x1 = getX(i)
                val y1 = getY(data[i])
                val x2 = getX(i + 1)
                val y2 = getY(data[i + 1])

                // Calculate control points for smooth curve (Catmull-Rom or simple cubic)
                // Simple Cubic approach: Control points are halfway horizontally, same Y
                val cx1 = (x1 + x2) / 2
                val cy1 = y1
                val cx2 = (x1 + x2) / 2
                val cy2 = y2

                path.cubicTo(cx1, cy1, cx2, cy2, x2, y2)
                fillPath.cubicTo(cx1, cy1, cx2, cy2, x2, y2)
            }

            // Close fill path
            fillPath.lineTo(getX(data.size - 1), chartHeight)
            fillPath.lineTo(getX(0), chartHeight)
            fillPath.close()
        }

        // 4. Draw Gradient Fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    graphColor.copy(alpha = 0.3f),
                    graphColor.copy(alpha = 0.0f)
                ),
                startY = 0f,
                endY = chartHeight
            )
        )

        // 5. Draw Line Stroke
        drawPath(
            path = path,
            color = graphColor,
            style = Stroke(
                width = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        // 6. Draw Hollow Points
        data.forEachIndexed { index, value ->
            val x = getX(index)
            val y = getY(value)
            val radius = 6.dp.toPx()
            val strokeWidth = 3.dp.toPx()

            // Draw circle background (to hide the line behind it)
            drawCircle(
                color = surfaceColor, // Matches card background
                radius = radius,
                center = Offset(x, y)
            )
            // Draw circle border (The "Hollow" look)
            drawCircle(
                color = graphColor,
                radius = radius,
                center = Offset(x, y),
                style = Stroke(width = strokeWidth)
            )
            // Draw inner fill (Dark center)
            drawCircle(
                color = surfaceColor, // Or a very dark version of primary
                radius = radius - strokeWidth,
                center = Offset(x, y),
                style = Fill
            )
        }

        // 7. Draw Text Labels
        // Note: We use nativeCanvas for text in generic Canvas
        val textPaint = Paint().apply {
            color = onSurface.copy(alpha = 0.6f).toArgb()
            textSize = 10.sp.toPx()
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Distribute labels evenly
        val labelSpacing = width / (labels.size - 1).coerceAtLeast(1)
        labels.forEachIndexed { index, label ->
            val x = index * labelSpacing
            // Ensure first and last don't get clipped too much
            val adjustedX = when(index) {
                0 -> x + 10.dp.toPx()
                labels.lastIndex -> x - 10.dp.toPx()
                else -> x
            }

            drawContext.canvas.nativeCanvas.drawText(
                label,
                adjustedX,
                height, // Draw at very bottom
                textPaint
            )
        }
    }
}


@Composable
fun CategoryBreakdownSection(categories: List<CategoryReportData>, totalExpense: Double) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = getCardBorder(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Category Breakdown",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                        DonutChart(categories = categories)
                        Surface(
                            modifier = Modifier.size(90.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Top 3",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        categories.take(3).forEach { category ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(category.color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${(category.percentage * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
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
fun DonutChart(categories: List<CategoryReportData>) {
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(categories) {
        animationProgress.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 1000))
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 30f
        val diameter = size.minDimension
        var currentAngle = -90f

        categories.forEach { category ->
            val sweepAngle = 360f * category.percentage * animationProgress.value
            drawArc(
                color = category.color,
                startAngle = currentAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth),
                size = Size(diameter - strokeWidth, diameter - strokeWidth),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            )
            currentAngle += sweepAngle
        }
    }
}

@Composable
fun CategoryUtilizationSection(categories: List<CategoryReportData>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Budget Utilization",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = getCardBorder(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                categories.forEachIndexed { index, category ->
                    CategoryProgressItem(
                        category = category,
                        showDivider = index < categories.size - 1
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryProgressItem(category: CategoryReportData, showDivider: Boolean) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(category.color.copy(alpha = 0.15f))
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = category.color,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "$${String.format(Locale.US, "%,.0f", category.amount)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LinearProgressIndicator(
            progress = { category.percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = category.color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }

    if (showDivider) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            thickness = 1.dp
        )
    }
}

@Composable
fun ExportButtonSection() {
    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* Handle Export */ },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = getCardBorder(),
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Export Data",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Export as PDF, Excel, or CSV",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}