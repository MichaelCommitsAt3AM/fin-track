package com.example.fintrack.presentation.settings.recurring

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fintrack.core.ui.components.CustomDatePickerDialog
import com.example.fintrack.core.domain.model.RecurrenceFrequency
import com.example.fintrack.core.domain.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecurringTransactionScreen(
    recurringTransactionId: String, // Pass the ID or category name to identify which transaction to edit
    onNavigateBack: () -> Unit,
    onNavigateToManageCategories: () -> Unit,
    viewModel: EditRecurringTransactionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }

    // Load the transaction when screen opens
    LaunchedEffect(recurringTransactionId) {
        viewModel.loadTransaction(recurringTransactionId)
    }

    val relevantCategories = remember(state.categories, state.transactionType) {
        state.categories.filter { it.type.name == state.transactionType.name }
    }

    LaunchedEffect(key1 = true) {
        viewModel.events.collect { event ->
            when (event) {
                is EditRecurringTransactionEvent.NavigateBack -> onNavigateBack()
                is EditRecurringTransactionEvent.ShowSuccess -> {
                    Toast.makeText(context, "Recurring transaction updated", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                }
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Recurring Transaction", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            TransactionTypeToggle(
                selectedType = state.transactionType,
                onTypeSelected = { viewModel.onEvent(EditRecurringTransactionUiEvent.OnTypeChange(it)) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Category Selector
                CategorySelector(
                    categories = relevantCategories.map { it.name },
                    selectedCategory = state.selectedCategory,
                    onCategorySelected = { viewModel.onEvent(EditRecurringTransactionUiEvent.OnCategoryChange(it)) },
                    onAddCategoryClick = onNavigateToManageCategories
                )

                FinTrackTextField(
                    value = state.description,
                    onValueChange = { viewModel.onEvent(EditRecurringTransactionUiEvent.OnDescriptionChange(it)) },
                    label = "Description",
                    placeholder = "e.g. Netflix Subscription"
                )

                FinTrackTextField(
                    value = viewModel.formatMillisToDate(state.startDate),
                    onValueChange = {},
                    label = "Start Date",
                    placeholder = "Select date",
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.CalendarToday, null) },
                    modifier = Modifier.clickable { showDatePicker = true }
                )

                // Frequency Selection
                FrequencySection(
                    selectedFrequency = state.frequency,
                    onFrequencyChange = { viewModel.onEvent(EditRecurringTransactionUiEvent.OnFrequencyChange(it)) }
                )

                FinTrackTextField(
                    value = state.amount,
                    onValueChange = { viewModel.onEvent(EditRecurringTransactionUiEvent.OnAmountChange(it)) },
                    label = "Amount",
                    placeholder = "0.00",
                    leadingIcon = {
                        Text(
                            " Ksh:",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.onEvent(EditRecurringTransactionUiEvent.OnSaveTransaction) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Update Transaction", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDatePicker) {
        CustomDatePickerDialog(
            selectedDateMillis = state.startDate,
            onDateSelected = { newDate ->
                viewModel.onEvent(EditRecurringTransactionUiEvent.OnDateChange(newDate))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
fun FrequencySection(
    selectedFrequency: RecurrenceFrequency,
    onFrequencyChange: (RecurrenceFrequency) -> Unit
) {
    Column {
        Text(
            text = "Frequency",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val frequencies = listOf(
                RecurrenceFrequency.DAILY,
                RecurrenceFrequency.WEEKLY,
                RecurrenceFrequency.MONTHLY,
                RecurrenceFrequency.YEARLY
            )

            frequencies.forEach { freq ->
                val isSelected = selectedFrequency == freq

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

// Reuse components from AddTransactionScreen
@Composable
fun TransactionTypeToggle(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(4.dp)
    ) {
        val expenseColor = if (selectedType == TransactionType.EXPENSE) MaterialTheme.colorScheme.primary else Color.Transparent
        val expenseTextColor = if (selectedType == TransactionType.EXPENSE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

        val incomeColor = if (selectedType == TransactionType.INCOME) MaterialTheme.colorScheme.primary else Color.Transparent
        val incomeTextColor = if (selectedType == TransactionType.INCOME) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

        Button(
            onClick = { onTypeSelected(TransactionType.EXPENSE) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = expenseColor, contentColor = expenseTextColor),
            shape = RoundedCornerShape(8.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text("Expense", fontWeight = FontWeight.SemiBold)
        }

        Button(
            onClick = { onTypeSelected(TransactionType.INCOME) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = incomeColor, contentColor = incomeTextColor),
            shape = RoundedCornerShape(8.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text("Income", fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelector(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit,
    onAddCategoryClick: () -> Unit
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
