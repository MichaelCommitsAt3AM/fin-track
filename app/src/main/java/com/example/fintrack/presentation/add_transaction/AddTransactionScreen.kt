package com.example.fintrack.presentation.add_transaction

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fintrack.core.domain.model.TransactionType
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }

    // Listen for navigation events
    LaunchedEffect(key1 = true) {
        viewModel.events.collect { event ->
            when (event) {
                is AddTransactionEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    // Show error toasts
    LaunchedEffect(state.error) {
        state.error?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {

            // --- Income/Expense Toggle ---
            TransactionTypeToggle(
                selectedType = state.transactionType,
                onTypeSelected = { viewModel.onEvent(AddTransactionUiEvent.OnTypeChange(it)) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- Form Fields ---
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Category
                CategorySelector(
                    categories = state.categories.map { it.name },
                    selectedCategory = state.selectedCategory,
                    onCategorySelected = { viewModel.onEvent(AddTransactionUiEvent.OnCategoryChange(it)) },
                    onAddCategory = { /* TODO: Show Add Category Dialog */ }
                )

                // Description
                FinTrackTextField(
                    value = state.description,
                    onValueChange = { viewModel.onEvent(AddTransactionUiEvent.OnDescriptionChange(it)) },
                    label = "Description",
                    placeholder = "e.g. Weekly groceries"
                )

                // Date
                FinTrackTextField(
                    value = viewModel.formatMillisToDate(state.date),
                    onValueChange = {},
                    label = "Date",
                    placeholder = "Select date",
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.CalendarToday, null) },
                    modifier = Modifier.clickable { showDatePicker = true }
                )

                // Amount
                FinTrackTextField(
                    value = state.amount,
                    onValueChange = { viewModel.onEvent(AddTransactionUiEvent.OnAmountChange(it)) },
                    label = "Amount",
                    placeholder = "0.00",
                    leadingIcon = { Text("$", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Submit Button ---
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

    // --- Date Picker Dialog ---
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // Add one day to fix timezone issue (DatePicker defaults to UTC midnight)
                    val selectedDate = datePickerState.selectedDateMillis!!
                    viewModel.onEvent(AddTransactionUiEvent.OnDateChange(selectedDate))
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

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
    onAddCategory: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "Category",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.weight(1f)
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
            }

            // Add Category Button
            IconButton(
                onClick = onAddCategory,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Category", tint = MaterialTheme.colorScheme.primary)
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