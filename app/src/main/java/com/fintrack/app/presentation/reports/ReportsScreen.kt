package com.fintrack.app.presentation.reports

import android.content.Intent
import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.biometric.BiometricPrompt
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import android.widget.Toast
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
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    // --- Biometric Logic ---
    fun launchBiometric() {
        if (activity == null) {
            viewModel.exportData() // Fallback if not an activity (shouldn't happen in normal app flow)
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.exportData()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(context, "Authentication failed: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Prompt handles this (shake/message), no extra action needed usually
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Confirm Export")
            .setSubtitle("Verify your identity to export financial data")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
    // -----------------------

    // Observe Export Events
    LaunchedEffect(key1 = true) {
        viewModel.exportEvent.collect { uri ->
            if (uri != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra(Intent.EXTRA_SUBJECT, "FinTrack Data Export")
                    putExtra(Intent.EXTRA_TEXT, "Here is my transaction history from FinTrack.")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Export Data"))
            }
        }
    }

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
                onNextClick = { viewModel.nextMonth() },
                isNextEnabled = !state.isCurrentMonth
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
                    totalIncome = state.totalIncome,
                    currencySymbol = state.currency.symbol
                )
            }

            // 3. Spending Trends (weekly by default, monthly via toggle)
            item {
                SpendingTrendsSection(
                    points = state.trend,
                    period = state.trendPeriod,
                    onPeriodChange = { viewModel.setTrendPeriod(it) },
                    currencySymbol = state.currency.symbol
                )
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
                CategoryUtilizationSection(
                    categories = state.categoryBreakdown,
                    currencySymbol = state.currency.symbol
                )
            }

            // 6. Export Button
            item {
                ExportButtonSection(onExportClick = {
                    launchBiometric()
                })
            }
        }
    }
}

// -------------------------------------------------------------------------
// UI Components
// -------------------------------------------------------------------------

@Composable
fun ReportHeader(
    currentMonth: String,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    isNextEnabled: Boolean = true
) {
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

                IconButton(onClick = onNextClick, enabled = isNextEnabled, modifier = Modifier.size(28.dp)) {
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
fun SummaryGridSection(totalExpense: Double, totalIncome: Double, currencySymbol: String) {
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
            currencySymbol = currencySymbol,
            modifier = Modifier.weight(1f)
        )

        // Net Income Card (Income - Expense)
        val netIncome = totalIncome - totalExpense
        SummaryCard(
            title = "Net Income",
            amount = netIncome,
            isIncome = true,
            currencySymbol = currencySymbol,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SummaryCard(title: String, amount: Double, isIncome: Boolean, currencySymbol: String, modifier: Modifier = Modifier) {
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
                text = "${if (isIncome && amount > 0) "+" else ""}$currencySymbol ${
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
// Spending Trends Section with Real Data
// -------------------------------------------------------------------------

@Composable
fun SpendingTrendsSection(
    points: List<TrendPoint>,
    period: TrendPeriod,
    onPeriodChange: (TrendPeriod) -> Unit,
    currencySymbol: String
) {
    // The highlighted point defaults to the latest period; tapping the chart moves it.
    var selectedIndex by remember(points, period) { mutableIntStateOf(points.lastIndex) }
    val selected = points.getOrNull(selectedIndex)
    val previous = points.getOrNull(selectedIndex - 1)
    val average = if (points.isNotEmpty()) points.sumOf { it.amount } / points.size else 0.0
    val hasData = points.any { it.amount > 0 }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = getCardBorder(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // --- Title + period toggle ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Spending Trends",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TrendPeriodToggle(selected = period, onSelected = onPeriodChange)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- Selected period readout ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = selected?.detail ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$currencySymbol ${String.format(Locale.US, "%,.2f", selected?.amount ?: 0.0)}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (selected != null && previous != null && previous.amount > 0) {
                        val diff = (selected.amount - previous.amount) / previous.amount * 100
                        TrendBadge(percentage = diff, suffix = "vs prev ${period.unit}")
                    }
                }

                Text(
                    text = "${period.unit.replaceFirstChar { it.uppercase() }}ly average: $currencySymbol ${String.format(Locale.US, "%,.2f", average)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (hasData) {
                    TrendChart(
                        data = points.map { it.amount.toFloat() },
                        labels = points.map { it.label },
                        selectedIndex = selectedIndex,
                        onPointSelected = { selectedIndex = it },
                        graphColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No spending in this period yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendPeriodToggle(selected: TrendPeriod, onSelected: (TrendPeriod) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(2.dp)
    ) {
        TrendPeriod.entries.forEach { period ->
            val isSelected = period == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelected(period) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = period.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TrendBadge(percentage: Double, suffix: String) {
    // More spending = error colour, less spending = primary colour
    val isIncrease = percentage > 0
    val badgeColor = if (isIncrease) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(color = badgeColor.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = if (isIncrease) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${String.format(Locale.US, "%.1f", kotlin.math.abs(percentage))}% $suffix",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = badgeColor
            )
        }
    }
}

@Composable
fun TrendChart(
    data: List<Float>,
    labels: List<String>,
    selectedIndex: Int,
    onPointSelected: (Int) -> Unit,
    graphColor: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val onSurface = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface
    val dashedLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    // Animate the line in when the data set changes (e.g. switching weeks/months)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(500))
    }

    Canvas(
        modifier = modifier.pointerInput(data.size) {
            detectTapGestures { offset ->
                // Map the tap to the nearest point (same x layout as the drawing below)
                val inset = 16.dp.toPx()
                val spacing = (size.width - inset * 2) / (data.size - 1).coerceAtLeast(1)
                val index = ((offset.x - inset) / spacing).roundToInt().coerceIn(0, data.lastIndex)
                onPointSelected(index)
            }
        }
    ) {
        val width = size.width
        val height = size.height
        val chartHeight = height - 28.dp.toPx() // room for x-axis labels
        // Horizontal inset keeps the first/last point and label from being clipped at the edges
        val inset = 16.dp.toPx()
        val verticalPadding = 12.dp.toPx()

        // Scale from 0 so heights are honest (a min-based scale exaggerates small changes)
        val maxVal = (data.maxOrNull() ?: 0f).coerceAtLeast(1f)

        fun getY(value: Float): Float {
            val availableHeight = chartHeight - verticalPadding * 2
            return chartHeight - verticalPadding - (value / maxVal) * availableHeight * progress.value
        }

        val spacingX = (width - inset * 2) / (data.size - 1).coerceAtLeast(1)
        fun getX(index: Int): Float = if (data.size == 1) width / 2 else inset + index * spacingX

        // 1. Dashed horizontal grid lines
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

        // 2. Guide line for the selected period
        if (selectedIndex in data.indices) {
            drawLine(
                color = graphColor.copy(alpha = 0.25f),
                start = Offset(getX(selectedIndex), 0f),
                end = Offset(getX(selectedIndex), chartHeight),
                strokeWidth = 2.dp.toPx()
            )
        }

        // 3. Smooth bezier path + gradient fill
        val path = Path()
        val fillPath = Path()
        path.moveTo(getX(0), getY(data[0]))
        fillPath.moveTo(getX(0), getY(data[0]))
        for (i in 0 until data.size - 1) {
            val x1 = getX(i)
            val y1 = getY(data[i])
            val x2 = getX(i + 1)
            val y2 = getY(data[i + 1])
            val cx = (x1 + x2) / 2
            path.cubicTo(cx, y1, cx, y2, x2, y2)
            fillPath.cubicTo(cx, y1, cx, y2, x2, y2)
        }
        fillPath.lineTo(getX(data.lastIndex), chartHeight)
        fillPath.lineTo(getX(0), chartHeight)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(graphColor.copy(alpha = 0.3f), graphColor.copy(alpha = 0.0f)),
                startY = 0f,
                endY = chartHeight
            )
        )
        drawPath(
            path = path,
            color = graphColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // 4. Points (the selected one is filled and larger)
        data.forEachIndexed { index, value ->
            val center = Offset(getX(index), getY(value))
            val isSelected = index == selectedIndex
            val radius = if (isSelected) 7.dp.toPx() else 5.dp.toPx()
            drawCircle(color = surfaceColor, radius = radius, center = center)
            drawCircle(
                color = graphColor,
                radius = radius,
                center = center,
                style = if (isSelected) Fill else Stroke(width = 2.5.dp.toPx())
            )
        }

        // 5. X-axis labels (selected label emphasised)
        val textPaint = Paint().apply {
            textSize = 10.sp.toPx()
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        labels.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            textPaint.color = (if (isSelected) graphColor else onSurface.copy(alpha = 0.6f)).toArgb()
            textPaint.isFakeBoldText = isSelected
            drawContext.canvas.nativeCanvas.drawText(label, getX(index), height - 4.dp.toPx(), textPaint)
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
fun CategoryUtilizationSection(categories: List<CategoryReportData>, currencySymbol: String) {
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
                        currencySymbol = currencySymbol,
                        showDivider = index < categories.size - 1
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryProgressItem(category: CategoryReportData, currencySymbol: String, showDivider: Boolean) {
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
                text = "$currencySymbol ${String.format(Locale.US, "%,.0f", category.amount)}",
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
fun ExportButtonSection(onExportClick: () -> Unit) {
    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExportClick() },
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
                            text = "Export as CSV",
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
