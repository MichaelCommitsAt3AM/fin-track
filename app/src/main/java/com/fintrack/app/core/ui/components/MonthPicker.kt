package com.fintrack.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MonthPicker(
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    label: String = "Month",
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val formattedDate = remember(selectedDateMillis) {
        Instant.ofEpochMilli(selectedDateMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = formattedDate,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            enabled = enabled,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Select Month",
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) focusManager.clearFocus() },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        if (enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { showDatePicker = true }
            )
        }
    }

    if (showDatePicker) {
        MonthPickerDialog(
            selectedDateMillis = selectedDateMillis,
            onDateSelected = { newDate ->
                onDateSelected(newDate)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
fun MonthPickerDialog(
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val currentYear = remember { Year.now() }
    val currentMonth = remember { LocalDate.now().month }
    
    // Parse currently selected date
    val selectedDate = remember(selectedDateMillis) {
        Instant.ofEpochMilli(selectedDateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }
    
    var tempSelectedMonth by remember { mutableStateOf(selectedDate.month) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Text(
                    text = "SELECT MONTH",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentYear.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                // Months Grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val months = Month.values()
                    // 4 rows of 3 months each
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (col in 0 until 3) {
                                val monthIndex = row * 3 + col
                                val month = months[monthIndex]
                                val isSelected = month == tempSelectedMonth
                                val isDisabled = month < currentMonth
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else Color.Transparent
                                        )
                                        .clickable(enabled = !isDisabled) {
                                            tempSelectedMonth = month
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isDisabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newDate = LocalDate.of(currentYear.value, tempSelectedMonth, 1)
                            val millis = newDate
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                            onDateSelected(millis)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
