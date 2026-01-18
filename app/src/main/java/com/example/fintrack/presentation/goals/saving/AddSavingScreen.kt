package com.example.fintrack.presentation.goals.saving

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.example.fintrack.core.ui.components.CustomDatePickerDialog
import com.example.fintrack.presentation.ui.theme.FinTrackGreen
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.fintrack.presentation.goals.saving.SavingViewModel
import kotlinx.coroutines.CoroutineScope
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSavingScreen(
    onNavigateBack: () -> Unit,
    viewModel: SavingViewModel = hiltViewModel()
) {
    val currency by viewModel.currencyPreference.collectAsState()
    
    // State Hoisting
    var targetAmount by remember { mutableStateOf("") }
    var currentAmount by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf<ImageVector>(Icons.Default.Savings) }

    // Date Picker State for target date
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }

    // Icon Selection Dialog
    var showIconPicker by remember { mutableStateOf(false) }

    val activeColor = FinTrackGreen

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // Let imePadding handle insets
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Add Saving Goal", fontWeight = FontWeight.Bold) },
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
            SaveSavingButton(
                activeColor = activeColor,
                onClick = {
                    // Save to database via ViewModel
                    val targetAmountValue = targetAmount.toDoubleOrNull() ?: 0.0
                    val currentAmountValue = currentAmount.toDoubleOrNull() ?: 0.0
                    
                    if (title.isNotBlank() && targetAmountValue > 0) {
                        viewModel.addSaving(
                            title = title,
                            targetAmount = targetAmountValue,
                            currentAmount = currentAmountValue,
                            targetDate = selectedDateMillis ?: System.currentTimeMillis(),
                            notes = notes,
                            iconName = when (selectedIcon) {
                                Icons.Default.Savings -> "Savings"
                                Icons.Default.Computer -> "Computer"
                                Icons.Default.BeachAccess -> "BeachAccess"
                                Icons.Default.DirectionsCar -> "DirectionsCar"
                                Icons.Default.Home -> "Home"
                                Icons.Default.School -> "School"
                                Icons.Default.HealthAndSafety -> "HealthAndSafety"
                                Icons.Default.CardGiftcard -> "CardGiftcard"
                                Icons.Default.Flight -> "Flight"
                                Icons.Default.Phone -> "Phone"
                                Icons.Default.Watch -> "Watch"
                                Icons.Default.ShoppingBag -> "ShoppingBag"
                                else -> "Savings"
                            }
                        )
                        onNavigateBack()
                    }
                }
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        val bringIntoViewRequester = remember { BringIntoViewRequester() }
        val coroutineScope = rememberCoroutineScope()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding() // Moves content above keyboard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = paddingValues.calculateBottomPadding()), // Bottom bar padding
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon Display Section
                Spacer(modifier = Modifier.height(16.dp))
                SavingIconDisplay(
                    selectedIcon = selectedIcon,
                    activeColor = activeColor,
                    onClick = { showIconPicker = true }
                )

                // Target Amount Section
                Spacer(modifier = Modifier.height(32.dp))
                SavingAmountSection(
                    amount = targetAmount,
                    onAmountChange = { targetAmount = it },
                    activeColor = activeColor,
                    label = "Target Amount",
                    currencySymbol = currency.symbol
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(24.dp))

                // Form Section
                SavingFormSection(
                    title = title,
                    onTitleChange = { title = it },
                    currentAmount = currentAmount,
                    onCurrentAmountChange = { currentAmount = it },
                    selectedDateMillis = selectedDateMillis,
                    onDateClick = { showDatePicker = true },
                    notes = notes,
                    onNotesChange = { notes = it },
                    bringIntoViewRequester = bringIntoViewRequester,
                    coroutineScope = coroutineScope
                )

                // Date Picker Dialog
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
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // Icon Picker Dialog
    if (showIconPicker) {
        IconPickerDialog(
            onDismiss = { showIconPicker = false },
            onIconSelected = { icon ->
                selectedIcon = icon
                showIconPicker = false
            },
            activeColor = activeColor
        )
    }
}

// --- Icon Display Section ---
@Composable
fun SavingIconDisplay(
    selectedIcon: ImageVector,
    activeColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(activeColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = selectedIcon,
                contentDescription = "Saving Icon",
                tint = activeColor,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap to change icon",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// --- Amount Section ---
@Composable
fun SavingAmountSection(
    amount: String,
    onAmountChange: (String) -> Unit,
    activeColor: Color,
    label: String,
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

            SavingAmountField(
                amount = amount,
                onAmountChange = onAmountChange,
                activeColor = activeColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SavingAmountField(
    amount: String,
    onAmountChange: (String) -> Unit,
    activeColor: Color
) {
    TextField(
        value = amount,
        onValueChange = {
            if (it.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
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
        modifier = Modifier.widthIn(min = 140.dp)
    )
}

// --- Form Section ---
@Composable
fun SavingFormSection(
    title: String,
    onTitleChange: (String) -> Unit,
    currentAmount: String,
    onCurrentAmountChange: (String) -> Unit,
    selectedDateMillis: Long?,
    onDateClick: () -> Unit,
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
        SavingFormField(
            label = "GOAL NAME",
            icon = Icons.Default.Flag,
            value = title,
            onValueChange = onTitleChange,
            placeholder = "e.g. New Laptop, Vacation, Emergency Fund"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SavingFormField(
                    label = "CURRENT SAVED",
                    icon = Icons.Default.AccountBalance,
                    value = currentAmount,
                    onValueChange = {
                        if (it.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                            onCurrentAmountChange(it)
                        }
                    },
                    placeholder = "0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingLabel = "$"
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                SavingFormField(
                    label = "TARGET DATE",
                    icon = Icons.Default.CalendarToday,
                    value = selectedDateMillis?.let {
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it)
                    } ?: "",
                    onValueChange = {},
                    placeholder = "Select",
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
        }

        SavingFormField(
            label = "NOTES",
            icon = Icons.Default.Description,
            value = notes,
            onValueChange = onNotesChange,
            placeholder = "Add any details or motivation...",
            singleLine = false,
            minLines = 3,
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

// --- Save Button ---
@Composable
fun SaveSavingButton(
    activeColor: Color,
    onClick: () -> Unit
) {
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
            Text("Save Goal", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Check, contentDescription = null)
        }
    }
}

// --- Form Field Component ---
@Composable
fun SavingFormField(
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

// --- Icon Picker Dialog ---
@Composable
fun IconPickerDialog(
    onDismiss: () -> Unit,
    onIconSelected: (ImageVector) -> Unit,
    activeColor: Color
) {
    val savingIcons = listOf(
        Icons.Default.Savings to "Savings",
        Icons.Default.Computer to "Technology",
        Icons.Default.BeachAccess to "Vacation",
        Icons.Default.DirectionsCar to "Car",
        Icons.Default.Home to "Home",
        Icons.Default.School to "Education",
        Icons.Default.HealthAndSafety to "Emergency",
        Icons.Default.CardGiftcard to "Gift",
        Icons.Default.Flight to "Travel",
        Icons.Default.Phone to "Phone",
        Icons.Default.Watch to "Watch",
        Icons.Default.ShoppingBag to "Shopping"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Icon", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                savingIcons.chunked(4).forEach { rowIcons ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowIcons.forEach { (icon, label) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onIconSelected(icon) }
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(activeColor.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = activeColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = activeColor)
            }
        }
    )
}
