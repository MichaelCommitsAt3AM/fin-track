package com.example.fintrack.presentation.goals

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fintrack.presentation.ui.theme.FinTrackGreen
import java.text.SimpleDateFormat
import java.util.*

// Mock data class for demonstration
data class SavingGoal(
    val id: String,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: Long,
    val category: String,
    val icon: ImageVector,
    val color: Color,
    val contributions: List<Contribution>
)

data class Contribution(
    val id: String,
    val amount: Double,
    val date: Long,
    val note: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSavingScreen(
    savingId: String,
    onNavigateBack: () -> Unit,
    onEdit: () -> Unit = {}
) {
    // Mock data - in production this would come from a ViewModel
    val saving = remember {
        when (savingId) {
            "laptop" -> SavingGoal(
                id = "laptop",
                title = "New Laptop",
                targetAmount = 2000.0,
                currentAmount = 1200.0,
                targetDate = System.currentTimeMillis() + 60L * 24 * 60 * 60 * 1000, // 60 days
                category = "Technology",
                icon = Icons.Default.Computer,
                color = Color(0xFF10B981),
                contributions = listOf(
                    Contribution("1", 500.0, System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000, "Initial deposit"),
                    Contribution("2", 300.0, System.currentTimeMillis() - 20L * 24 * 60 * 60 * 1000),
                    Contribution("3", 200.0, System.currentTimeMillis() - 10L * 24 * 60 * 60 * 1000, "Bonus money"),
                    Contribution("4", 200.0, System.currentTimeMillis() - 2L * 24 * 60 * 60 * 1000)
                )
            )
            "vacation" -> SavingGoal(
                id = "vacation",
                title = "Vacation",
                targetAmount = 3500.0,
                currentAmount = 875.0,
                targetDate = System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000,
                category = "Travel",
                icon = Icons.Default.BeachAccess,
                color = Color(0xFF3B82F6),
                contributions = listOf(
                    Contribution("1", 500.0, System.currentTimeMillis() - 45L * 24 * 60 * 60 * 1000),
                    Contribution("2", 375.0, System.currentTimeMillis() - 15L * 24 * 60 * 60 * 1000)
                )
            )
            else -> SavingGoal(
                id = "emergency",
                title = "Emergency Fund",
                targetAmount = 10000.0,
                currentAmount = 8000.0,
                targetDate = System.currentTimeMillis() + 120L * 24 * 60 * 60 * 1000,
                category = "Safety",
                icon = Icons.Default.HealthAndSafety,
                color = Color(0xFFA855F7),
                contributions = listOf(
                    Contribution("1", 5000.0, System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000, "Starting fund"),
                    Contribution("2", 1500.0, System.currentTimeMillis() - 60L * 24 * 60 * 60 * 1000),
                    Contribution("3", 1000.0, System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000),
                    Contribution("4", 500.0, System.currentTimeMillis() - 5L * 24 * 60 * 60 * 1000)
                )
            )
        }
    }

    var showContributionDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val percentage = (saving.currentAmount / saving.targetAmount).toFloat().coerceIn(0f, 1f)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        saving.title,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Goal") },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Goal", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = { 
                                    Icon(
                                        Icons.Default.Delete, 
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    ) 
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            // Hero Section - Circular Progress
            item {
                SavingProgressHero(
                    saving = saving,
                    percentage = percentage
                )
            }

            // Quick Stats Cards
            item {
                QuickStatsRow(saving = saving, percentage = percentage)
            }

            // Add Contribution Button
            item {
                Button(
                    onClick = { showContributionDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = saving.color
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Add Contribution",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Contribution History Section
            item {
                Text(
                    text = "Contribution History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(saving.contributions.sortedByDescending { it.date }) { contribution ->
                ContributionHistoryItem(contribution = contribution)
            }

            // Empty state if no contributions
            if (saving.contributions.isEmpty()) {
                item {
                    EmptyContributionState()
                }
            }
        }
    }

    // Contribution Dialog
    if (showContributionDialog) {
        AddContributionDialog(
            onDismiss = { showContributionDialog = false },
            onConfirm = { amount, note ->
                // TODO: Add contribution to saving
                showContributionDialog = false
            },
            color = saving.color
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { 
                Icon(
                    Icons.Default.Warning, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                ) 
            },
            title = { Text("Delete Saving Goal?") },
            text = { 
                Text("Are you sure you want to delete \"${saving.title}\"? This action cannot be undone.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // TODO: Delete saving
                        showDeleteDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SavingProgressHero(
    saving: SavingGoal,
    percentage: Float
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(percentage) {
        animatedProgress.animateTo(
            targetValue = percentage,
            animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                // Background circle
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color.LightGray.copy(alpha = 0.2f),
                        style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Progress circle
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = saving.color,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress.value,
                        useCenter = false,
                        style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Center content
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = saving.icon,
                        contentDescription = null,
                        tint = saving.color,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(percentage * 100).toInt()}%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Complete",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Amount display
            Text(
                text = "$${String.format("%,.2f", saving.currentAmount)}",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = saving.color
            )
            Text(
                text = "of $${String.format("%,.2f", saving.targetAmount)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickStatsRow(saving: SavingGoal, percentage: Float) {
    val remaining = saving.targetAmount - saving.currentAmount
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.CalendarToday,
            label = "Deadline",
            value = dateFormat.format(Date(saving.targetDate)),
            color = Color(0xFF6366F1)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.TrendingUp,
            label = "Remaining",
            value = "$${String.format("%,.0f", remaining)}",
            color = Color(0xFFF59E0B)
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ContributionHistoryItem(contribution: Contribution) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        .background(FinTrackGreen.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = FinTrackGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = if (contribution.note.isNotEmpty()) contribution.note else "Contribution",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateFormat.format(Date(contribution.date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "+$${String.format("%.2f", contribution.amount)}",
                fontWeight = FontWeight.Bold,
                color = FinTrackGreen,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun EmptyContributionState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AccountBalance,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No contributions yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Start saving by adding your first contribution",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AddContributionDialog(
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String) -> Unit,
    color: Color
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Icon(
                Icons.Default.Add, 
                contentDescription = null,
                tint = color
            ) 
        },
        title = { Text("Add Contribution") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        if (it.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                            amount = it
                        }
                    },
                    label = { Text("Amount") },
                    leadingIcon = { Text("$", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val contributionAmount = amount.toDoubleOrNull()
                    if (contributionAmount != null && contributionAmount > 0) {
                        onConfirm(contributionAmount, note)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = color),
                enabled = amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
