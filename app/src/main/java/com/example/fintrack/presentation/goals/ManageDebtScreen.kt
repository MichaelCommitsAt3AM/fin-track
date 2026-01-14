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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fintrack.presentation.ui.theme.FinTrackGreen
import java.text.SimpleDateFormat
import java.util.*

// Mock data class for demonstration
import com.example.fintrack.core.domain.model.DebtType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageDebtScreen(
    debtId: String,
    onNavigateBack: () -> Unit,
    onEdit: () -> Unit = {},
    viewModel: DebtViewModel = hiltViewModel()
) {
    val domainDebt by viewModel.currentDebt.collectAsState()
    val domainPayments by viewModel.payments.collectAsState()
    val currency by viewModel.currencyPreference.collectAsState()
    
    // Map domain model to UI model
    val debt = domainDebt?.toUiModel(domainPayments)
    
    // Load debt data when screen opens
    LaunchedEffect(debtId) {
        viewModel.loadDebt(debtId)
    }
    
    // Mock data for preview - will be replaced by actual debt
    val mockDebt = remember {
        when (debtId) {
            "student_loan" -> DebtGoal(
                id = "student_loan",
                title = "Student Loan",
                originalAmount = 8000.0,
                currentBalance = 5400.0,
                minimumPayment = 250.0,
                dueDate = System.currentTimeMillis() + 15L * 24 * 60 * 60 * 1000, // 15 days
                interestRate = 4.5,
                icon = Icons.Default.School,
                color = GoalDanger,
                debtType = DebtType.I_OWE,
                payments = listOf(
                    UiPayment("1", 1000.0, System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000, "Initial payment"),
                    UiPayment("2", 800.0, System.currentTimeMillis() - 60L * 24 * 60 * 60 * 1000),
                    UiPayment("3", 500.0, System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000),
                    UiPayment("4", 300.0, System.currentTimeMillis() - 10L * 24 * 60 * 60 * 1000, "Extra payment")
                )
            )
            else -> DebtGoal(
                id = "credit_card",
                title = "Credit Card",
                originalAmount = 2000.0,
                currentBalance = 1250.0,
                minimumPayment = 45.0,
                dueDate = System.currentTimeMillis() + 5L * 24 * 60 * 60 * 1000, // 5 days
                interestRate = 18.9,
                icon = Icons.Default.CreditCard,
                color = GoalWarning,
                debtType = DebtType.I_OWE,
                payments = listOf(
                    UiPayment("1", 500.0, System.currentTimeMillis() - 60L * 24 * 60 * 60 * 1000),
                    UiPayment("2", 250.0, System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000, "Monthly payment")
                )
            )
        }
    }
    
    // Use actual debt or mock for safety
    val displayDebt = debt ?: mockDebt
    val displayPayments = displayDebt.payments

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val paidPercentage = ((displayDebt.originalAmount - displayDebt.currentBalance) / displayDebt.originalAmount).toFloat().coerceIn(0f, 1f)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        displayDebt.title,
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
                                text = { Text("Edit Debt") },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Debt", color = MaterialTheme.colorScheme.error) },
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
                DebtProgressHero(
                    debt = displayDebt,
                    paidPercentage = paidPercentage,
                    currency = currency
                )
            }

            // Quick Stats Cards
            item {
                DebtQuickStatsRow(debt = displayDebt, currency = currency)
            }

            // Make Payment Button
            item {
                Button(
                    onClick = { showPaymentDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FinTrackGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Make Payment",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Payment History Section
            item {
                Text(
                    text = "Payment History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(displayPayments.sortedByDescending { it.date }) { payment ->
                PaymentHistoryItem(payment = payment, currency = currency)
            }

            // Empty state if no payments
            if (displayPayments.isEmpty()) {
                item {
                    EmptyPaymentState()
                }
            }
        }
    }

    // Payment Dialog
    if (showPaymentDialog) {
        MakePaymentDialog(
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amount, note ->
                viewModel.makePayment(debtId, amount, note)
                showPaymentDialog = false
            },
            minimumPayment = displayDebt.minimumPayment,
            currency = currency
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
            title = { Text("Delete Debt?") },
            text = { 
                Text("Are you sure you want to delete \"${displayDebt.title}\"? This action cannot be undone.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDebt(debtId)
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
fun DebtProgressHero(
    debt: DebtGoal,
    paidPercentage: Float,
    currency: com.example.fintrack.core.domain.model.Currency
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(paidPercentage) {
        animatedProgress.animateTo(
            targetValue = paidPercentage,
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

                // Progress circle - shows paid percentage in green
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = FinTrackGreen,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress.value,
                        useCenter = false,
                        style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Center content
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = debt.icon,
                        contentDescription = null,
                        tint = debt.color,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(paidPercentage * 100).toInt()}%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Paid Off",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Balance display
            Text(
                text = "${currency.symbol}${String.format("%,.2f", debt.currentBalance)}",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = debt.color
            )
            Text(
                text = "Remaining Balance",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "of ${currency.symbol}${String.format("%,.2f", debt.originalAmount)} original",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun DebtQuickStatsRow(debt: DebtGoal, currency: com.example.fintrack.core.domain.model.Currency) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.CalendarToday,
            label = "Due Date",
            value = dateFormat.format(Date(debt.dueDate)),
            color = Color(0xFFE74C3C)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Payment,
            label = "Min Payment",
            value = "${currency.symbol}${String.format("%.2f", debt.minimumPayment)}",
            color = Color(0xFFF39C12)
        )
    }
}

@Composable
fun PaymentHistoryItem(payment: UiPayment, currency: com.example.fintrack.core.domain.model.Currency) {
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
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = FinTrackGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = if (payment.note.isNotEmpty()) payment.note else "Payment",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateFormat.format(Date(payment.date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "-${currency.symbol}${String.format("%.2f", payment.amount)}",
                fontWeight = FontWeight.Bold,
                color = FinTrackGreen,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun EmptyPaymentState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Payment,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No payments yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Start reducing your debt by making your first payment",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MakePaymentDialog(
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String) -> Unit,
    minimumPayment: Double,
    currency: com.example.fintrack.core.domain.model.Currency
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Icon(
                Icons.Default.Payment, 
                contentDescription = null,
                tint = FinTrackGreen
            ) 
        },
        title = { Text("Make Payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Minimum payment: ${currency.symbol}${String.format("%.2f", minimumPayment)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        if (it.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                            amount = it
                        }
                    },
                    label = { Text("Amount") },
                    leadingIcon = { Text(currency.symbol, fontWeight = FontWeight.Bold) },
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
                    val paymentAmount = amount.toDoubleOrNull()
                    if (paymentAmount != null && paymentAmount > 0) {
                        onConfirm(paymentAmount, note)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FinTrackGreen),
                enabled = amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0
            ) {
                Text("Make Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
