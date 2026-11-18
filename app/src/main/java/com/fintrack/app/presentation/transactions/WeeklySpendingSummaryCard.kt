package com.fintrack.app.presentation.transactions

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fintrack.app.core.domain.util.WeeklySpendingSummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

/**
 * Compact weekly overview shown at the top of the Transactions list.
 *
 * - Header steps between weeks (can't go past the current week).
 * - Big number = total spent that week, with a change badge vs the week before.
 * - 7 bars, one per day; today is highlighted and days still to come are faded.
 *   Tapping a bar shows that day's amount in the footer.
 */
@Composable
fun WeeklySpendingSummaryCard(
    summary: WeeklySpendingSummary,
    currencySymbol: String,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Reset the tapped day whenever the week changes
    var selectedDay by remember(summary.weekStart) { mutableStateOf<LocalDate?>(null) }
    val today = remember { LocalDate.now() }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            WeekNavigator(
                summary = summary,
                onPreviousWeek = onPreviousWeek,
                onNextWeek = onNextWeek
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currencySymbol ${"%,.2f".format(summary.totalSpent)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                ChangeBadge(changePercent = summary.changePercent)
            }

            Spacer(Modifier.height(16.dp))

            DailyBars(
                summary = summary,
                today = today,
                selectedDay = selectedDay,
                onDaySelected = { day -> selectedDay = if (selectedDay == day) null else day }
            )

            Spacer(Modifier.height(12.dp))

            val footer = selectedDay?.let { day ->
                val amount = summary.days.firstOrNull { it.date == day }?.amount ?: 0.0
                "${day.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))}: " +
                    "$currencySymbol ${"%,.2f".format(amount)}"
            } ?: buildString {
                append("Avg/day $currencySymbol ${"%,.0f".format(summary.dailyAverage)}")
                append("  ·  ${summary.expenseCount} expense${if (summary.expenseCount == 1) "" else "s"}")
                if (summary.totalIncome > 0) {
                    append("  ·  In $currencySymbol ${"%,.0f".format(summary.totalIncome)}")
                }
            }

            Text(
                text = footer,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WeekNavigator(
    summary: WeeklySpendingSummary,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit
) {
    val today = LocalDate.now()
    val title = when {
        summary.isCurrentWeek -> "This week"
        today in summary.weekStart.plusWeeks(1)..summary.weekEnd.plusWeeks(1) -> "Last week"
        else -> "Week"
    }
    val range = formatWeekRange(summary.weekStart, summary.weekEnd)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousWeek, modifier = Modifier.size(32.dp)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week")
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = range,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = onNextWeek,
            enabled = !summary.isCurrentWeek,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week")
        }
    }
}

@Composable
private fun ChangeBadge(changePercent: Double?) {
    if (changePercent == null) {
        Text(
            text = "No spending last week",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    // Spending more is bad (error colour), spending less is good (primary colour)
    val isUp = changePercent > 0
    val color = if (isUp) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.12f)) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = "${"%.0f".format(abs(changePercent))}% vs last wk",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun DailyBars(
    summary: WeeklySpendingSummary,
    today: LocalDate,
    selectedDay: LocalDate?,
    onDaySelected: (LocalDate) -> Unit
) {
    val max = summary.days.maxOfOrNull { it.amount }?.takeIf { it > 0 } ?: 1.0
    val barAreaHeight = 72.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        summary.days.forEach { day ->
            val fraction by animateFloatAsState(
                targetValue = (day.amount / max).toFloat(),
                animationSpec = tween(400),
                label = "barHeight"
            )
            val isToday = day.date == today
            val isSelected = day.date == selectedDay
            val barColor = when {
                isSelected || isToday -> MaterialTheme.colorScheme.primary
                day.isFuture -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            }
            val dayName = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !day.isFuture) { onDaySelected(day.date) }
                    .semantics {
                        contentDescription = "${day.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}: ${"%,.2f".format(day.amount)}"
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barAreaHeight),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            // Keep a small stub visible on zero days so the week still reads as 7 slots
                            .height((barAreaHeight * fraction).coerceAtLeast(4.dp))
                            .clip(RoundedCornerShape(6.dp))
                            .background(barColor)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday || isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatWeekRange(start: LocalDate, end: LocalDate): String {
    val locale = Locale.getDefault()
    return if (start.month == end.month) {
        "${start.dayOfMonth} – ${end.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))}"
    } else {
        "${start.format(DateTimeFormatter.ofPattern("d MMM", locale))} – " +
            end.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))
    }
}
