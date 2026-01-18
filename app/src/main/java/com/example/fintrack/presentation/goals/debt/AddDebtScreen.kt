package com.example.fintrack.presentation.goals.debt

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.example.fintrack.core.ui.components.CustomDatePickerDialog
import com.example.fintrack.presentation.goals.debt.DebtViewModel
import com.example.fintrack.presentation.ui.theme.FinTrackGreen
import com.example.fintrack.presentation.ui.theme.SpendingBills
import kotlinx.coroutines.CoroutineScope
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.fintrack.core.domain.model.DebtType as DomainDebtType

enum class DebtType {
    I_OWE, OWED_TO_ME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtScreen(
    onNavigateBack: () -> Unit,
    viewModel: DebtViewModel = hiltViewModel()
) {
    val currency by viewModel.currencyPreference.collectAsState()

    // State Hoisting
    var debtType by remember { mutableStateOf(DebtType.I_OWE) }
    var amount by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var interest by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Date Picker State
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }

    val activeColor = if (debtType == DebtType.I_OWE) SpendingBills else FinTrackGreen

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // 1. CHANGE: Use safeDrawing to respect system bars and keyboard awareness
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Add Debt", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // 2. CHANGE: Apply IME padding to the container of the bottom bar
            // This ensures the button moves UP with the keyboard
            Box(modifier = Modifier.fillMaxWidth().imePadding()) {
                SaveDebtButton(
                    activeColor = activeColor,
                    onClick = {
                        val amountValue = amount.toDoubleOrNull() ?: 0.0
                        val interestValue = interest.toDoubleOrNull() ?: 0.0

                        if (title.isNotBlank() && amountValue > 0) {
                            viewModel.addDebt(
                                title = title,
                                originalAmount = amountValue,
                                currentBalance = amountValue,
                                minimumPayment = 0.0,
                                dueDate = selectedDateMillis ?: System.currentTimeMillis(),
                                interestRate = interestValue,
                                notes = notes,
                                iconName = "CreditCard",
                                debtType = when (debtType) {
                                    DebtType.I_OWE -> DomainDebtType.I_OWE
                                    DebtType.OWED_TO_ME -> DomainDebtType.OWED_TO_ME
                                }
                            )
                            onNavigateBack()
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        val bringIntoViewRequester = remember { BringIntoViewRequester() }
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                // 3. CHANGE: REMOVED .imePadding() here.
                // The paddingValues passed by Scaffold already account for the
                // BottomBar (which is now riding the keyboard).
                .padding(paddingValues)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Extracted Component: DebtTypeSelector
            DebtTypeSelector(
                selectedType = debtType,
                onTypeSelected = { debtType = it },
                activeColor = activeColor
            )

            Spacer(modifier = Modifier.height(32.dp))
            AmountSection(
                amount = amount,
                onAmountChange = { amount = it },
                activeColor = activeColor,
                currencySymbol = currency.symbol
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(24.dp))

            FormSection(
                title = title,
                onTitleChange = { title = it },
                selectedDateMillis = selectedDateMillis,
                onDateClick = { showDatePicker = true },
                interest = interest,
                onInterestChange = { interest = it },
                notes = notes,
                onNotesChange = { notes = it },
                bringIntoViewRequester = bringIntoViewRequester,
                coroutineScope = coroutineScope
            )

            // Add extra space at bottom so user can scroll fields above the button if needed
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showDatePicker) {
        CustomDatePickerDialog(
            selectedDateMillis = selectedDateMillis ?: System.currentTimeMillis(),
            onDateSelected = { newDate ->
                selectedDateMillis = newDate
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

// --- Optimization 1: Isolate Amount Input & Fix Intrinsic Measurement ---
@Composable
fun AmountSection(
    amount: String,
    onAmountChange: (String) -> Unit,
    activeColor: Color,
    currencySymbol: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = currencySymbol,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            AmountField(
                amount = amount,
                onAmountChange = onAmountChange,
                activeColor = activeColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Total Amount",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}


@Composable
fun AmountField(
    amount: String,
    onAmountChange: (String) -> Unit,
    activeColor: Color
) {
    TextField(
        value = amount,
        onValueChange = {
            if (it.matches(Regex("""\d*\.?\d{0,2}"""))) {
                onAmountChange(it)
            }
        },
        placeholder = {
            Text(
                "0.00",
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold
            )
        },
        textStyle = TextStyle(
            fontSize = 64.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        ),
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = activeColor
        ),
        modifier = Modifier
            .widthIn(min = 140.dp)
    )
}


// --- Optimization 2: Isolate Form Fields ---
@Composable
fun FormSection(
    title: String,
    onTitleChange: (String) -> Unit,
    selectedDateMillis: Long?,
    onDateClick: () -> Unit,
    interest: String,
    onInterestChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    bringIntoViewRequester: BringIntoViewRequester,
    coroutineScope: CoroutineScope
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        DebtFormField(
            label = "TITLE / PERSON",
            icon = Icons.Default.Person,
            value = title,
            onValueChange = onTitleChange,
            placeholder = "e.g. Car Loan or John Doe",
            trailingIcon = Icons.Default.Contacts
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                DebtFormField(
                    label = "DUE DATE",
                    icon = Icons.Default.CalendarToday,
                    value = selectedDateMillis?.let {
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it)
                    } ?: "",
                    onValueChange = {},
                    placeholder = "Select Date",
                    readOnly = true,
                    trailingIcon = Icons.Default.ExpandMore,
                    modifier = Modifier.clickable { onDateClick() }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { onDateClick() }
                )
            }

            Box(modifier = Modifier.width(120.dp)) {
                DebtFormField(
                    label = "INTEREST",
                    icon = null,
                    value = interest,
                    onValueChange = onInterestChange,
                    placeholder = "0",
                    trailingLabel = "%",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        DebtFormField(
            label = "NOTES",
            icon = Icons.Default.Description,
            value = notes,
            onValueChange = onNotesChange,
            placeholder = "Add any details here...",
            singleLine = false,
            minLines = 1,
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
}

// --- Optimization 3: Isolate Button & Animation ---
@Composable
fun SaveDebtButton(
    activeColor: Color,
    onClick: () -> Unit
) {
    // We animate the color HERE, locally.
    // Changing 'amount' at the top level does not trigger this animation calc.
    // Changing 'debtType' triggers this, but efficient layout boundaries protect the rest.
    val animatedColor by animateColorAsState(
        targetValue = activeColor,
        label = "ButtonColor",
        animationSpec = tween(300)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
            .padding(16.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = animatedColor
            )
        ) {
            Text("Save Debt", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Check, contentDescription = null)
        }
    }
}

@Composable
fun DebtTypeSelector(
    selectedType: DebtType,
    onTypeSelected: (DebtType) -> Unit,
    activeColor: Color
) {
    val density = LocalDensity.current
    var boxWidthPx by remember { mutableIntStateOf(0) } // Optimized primitive state

    // Animate color locally for the selector background
    val animatedSelectorColor by animateColorAsState(targetValue = activeColor, label = "SelectorColor")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            .padding(4.dp)
            .onSizeChanged { boxWidthPx = it.width }
    ) {
        val highlightX by animateDpAsState(
            targetValue = if (selectedType == DebtType.I_OWE) 0.dp else with(density) { (boxWidthPx / 2f).toDp() },
            label = "highlight"
        )

        Box(
            modifier = Modifier
                .offset(x = highlightX)
                .fillMaxWidth(0.5f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(animatedSelectorColor)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            TypeOption(
                modifier = Modifier.weight(1f),
                isSelected = selectedType == DebtType.I_OWE,
                text = "I Owe",
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                onClick = { onTypeSelected(DebtType.I_OWE) }
            )
            TypeOption(
                modifier = Modifier.weight(1f),
                isSelected = selectedType == DebtType.OWED_TO_ME,
                text = "Owed to Me",
                icon = Icons.Default.CallReceived,
                onClick = { onTypeSelected(DebtType.OWED_TO_ME) }
            )
        }
    }
}

@Composable
fun TypeOption(
    modifier: Modifier,
    isSelected: Boolean,
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DebtFormField(
    label: String,
    icon: ImageVector?,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    trailingIcon: ImageVector? = null,
    trailingLabel: String? = null,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
            leadingIcon = icon?.let {
                { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            trailingIcon = when {
                trailingIcon != null -> {
                    { Icon(trailingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                trailingLabel != null -> {
                    { Text(trailingLabel, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp)) }
                }
                else -> null
            },
            readOnly = readOnly,
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}