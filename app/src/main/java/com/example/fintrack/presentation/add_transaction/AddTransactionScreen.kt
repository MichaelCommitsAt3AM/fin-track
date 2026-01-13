package com.example.fintrack.presentation.add_transaction

import com.example.fintrack.core.ui.components.ModernDatePicker

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add

import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.example.fintrack.core.domain.model.RecurrenceFrequency
import com.example.fintrack.core.domain.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToManageCategories: () -> Unit,
    onNavigateToPaymentMethods: () -> Unit, // NEW PARAMETER
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val currency by viewModel.currencyPreference.collectAsState()
    val context = LocalContext.current

    val relevantCategories = remember(state.categories, state.transactionType) {
        state.categories.filter { it.type.name == state.transactionType.name }
    }

    LaunchedEffect(key1 = true) {
        viewModel.events.collect { event ->
            when (event) {
                is AddTransactionEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // Let imePadding handle insets
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Add Transaction", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                )
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        val bringIntoViewRequester = remember { BringIntoViewRequester() }
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding() // Automatically adds padding when keyboard appears
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState)
        ) {

            TransactionTypeToggle(
                selectedType = state.transactionType,
                onTypeSelected = { viewModel.onEvent(AddTransactionUiEvent.OnTypeChange(it)) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Category Selector
                CategorySelector(
                    categories = relevantCategories.map { it.name },
                    selectedCategory = state.selectedCategory,
                    onCategorySelected = { viewModel.onEvent(AddTransactionUiEvent.OnCategoryChange(it)) },
                    onAddCategoryClick = onNavigateToManageCategories // Pass the navigation lambda
                )

                // Payment Method Selector
                PaymentMethodSelector(
                    paymentMethods = state.paymentMethods,
                    selectedPaymentMethod = state.selectedPaymentMethod,
                    onPaymentMethodSelected = { viewModel.onEvent(AddTransactionUiEvent.OnPaymentMethodChange(it)) },
                    onAddPaymentMethodClick = onNavigateToPaymentMethods
                )

                ModernDatePicker(
                    selectedDateMillis = state.date,
                    onDateSelected = { viewModel.onEvent(AddTransactionUiEvent.OnDateChange(it)) },
                    modifier = Modifier.fillMaxWidth()
                )

                // --- Recurring Section ---
                RecurrenceSection(
                    isRecurring = state.isRecurring,
                    selectedFrequency = state.recurrenceFrequency,
                    onRecurringChange = { viewModel.onEvent(AddTransactionUiEvent.OnRecurringChange(it)) },
                    onFrequencyChange = { viewModel.onEvent(AddTransactionUiEvent.OnFrequencyChange(it)) }
                )

                FinTrackTextField(
                    value = state.amount,
                    onValueChange = { viewModel.onEvent(AddTransactionUiEvent.OnAmountChange(it)) },
                    label = "Amount",
                    placeholder = "0.00",
                    leadingIcon = { Text(" ${currency.symbol}:", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )

                FinTrackTextField(
                    value = state.description,
                    onValueChange = { viewModel.onEvent(AddTransactionUiEvent.OnDescriptionChange(it)) },
                    label = "Description",
                    placeholder = "e.g. Weekly groceries",
                    modifier = Modifier
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .onFocusEvent { focusState ->
                            if (focusState.isFocused) {
                                coroutineScope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.onEvent(AddTransactionUiEvent.OnSaveTransaction) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Add Transaction", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }


}

@Composable
fun RecurrenceSection(
    isRecurring: Boolean,
    selectedFrequency: RecurrenceFrequency,
    onRecurringChange: (Boolean) -> Unit,
    onFrequencyChange: (RecurrenceFrequency) -> Unit
) {
    Column {
        // Header with Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recurring Transaction?",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Switch(
                checked = isRecurring,
                onCheckedChange = onRecurringChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        // Frequency Options (Visible only if Recurring)
        if (isRecurring) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val frequencies = listOf(
                    RecurrenceFrequency.DAILY,
                    RecurrenceFrequency.WEEKLY,
                    RecurrenceFrequency.MONTHLY
                )

                frequencies.forEach { freq ->
                    val isSelected = selectedFrequency == freq
                    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow
                    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                    FilterChip(
                        selected = isSelected,
                        onClick = { onFrequencyChange(freq) },
                        label = {
                            Text(
                                text = freq.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.Transparent,
                            selectedBorderColor = Color.Transparent,
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelector(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit,
    onAddCategoryClick: () -> Unit // New parameter
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "Category",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedCategory ?: "Select a category",
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    Icon(Icons.Default.ExpandMore, contentDescription = null)
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                if (categories.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No categories found") },
                        onClick = { },
                        enabled = false
                    )
                } else {
                    categories.forEach { categoryName ->
                        DropdownMenuItem(
                            text = { Text(categoryName) },
                            onClick = {
                                onCategorySelected(categoryName)
                                expanded = false
                            }
                        )
                    }
                }

                // --- ADD BUTTON INSIDE DROPDOWN ---
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(
                            "Add new category",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        onAddCategoryClick()
                        expanded = false
                    }
                )
                // ----------------------------------
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodSelector(
    paymentMethods: List<String>,
    selectedPaymentMethod: String?,
    onPaymentMethodSelected: (String) -> Unit,
    onAddPaymentMethodClick: () -> Unit = {} // New parameter
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "Payment Method",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedPaymentMethod ?: "Select payment method",
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    Icon(Icons.Default.ExpandMore, contentDescription = null)
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                if (paymentMethods.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No payment methods found") },
                        onClick = { },
                        enabled = false
                    )
                } else {
                    paymentMethods.forEach { method ->
                        DropdownMenuItem(
                            text = { Text(method) },
                            onClick = {
                                onPaymentMethodSelected(method)
                                expanded = false
                            }
                        )
                    }
                }

                // --- ADD BUTTON INSIDE DROPDOWN ---
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(
                            "Add new payment method",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        onAddPaymentMethodClick()
                        expanded = false
                    }
                )
                // ----------------------------------
            }
        }
    }
}

@Composable
fun TransactionTypeToggle(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit
) {
    val expenseColor = Color(0xFFE53935)
    val incomeColor = MaterialTheme.colorScheme.primary
    val density = androidx.compose.ui.platform.LocalDensity.current

    var boxWidthPx by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(4.dp)
            .onSizeChanged { boxWidthPx = it.width }
    ) {
        val highlightX by animateDpAsState(
            targetValue = if (selectedType == TransactionType.EXPENSE) {
                0.dp
            } else {
                with(density) { (boxWidthPx / 2f).toDp() }
            },
            label = "highlight"
        )

        // 🔵 Sliding highlight
        Box(
            modifier = Modifier
                .offset(x = highlightX)
                .fillMaxWidth(0.5f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (selectedType == TransactionType.EXPENSE)
                        expenseColor
                    else incomeColor
                )
        )

        Row(modifier = Modifier.fillMaxSize()) {
            // EXPENSE
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTypeSelected(TransactionType.EXPENSE) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Expense",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedType == TransactionType.EXPENSE)
                        Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // INCOME
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTypeSelected(TransactionType.INCOME) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Income",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedType == TransactionType.INCOME)
                        Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}



@Composable
fun FinTrackTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: androidx.compose.ui.text.TextStyle = LocalTextStyle.current
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            readOnly = readOnly,
            keyboardOptions = keyboardOptions,
            textStyle = textStyle,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = modifier.fillMaxWidth()
        )
    }
}
