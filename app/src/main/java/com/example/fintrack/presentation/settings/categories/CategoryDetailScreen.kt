package com.example.fintrack.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fintrack.core.domain.model.CategoryType
import com.example.fintrack.presentation.add_transaction.FinTrackTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoryDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val availableColors = viewModel.availableColors

    // Dynamically select which list of icons to show
    val displayedIcons = if (state.type == CategoryType.EXPENSE) viewModel.expenseIcons else viewModel.incomeIcons

    LaunchedEffect(key1 = true) {
        viewModel.events.collect { event ->
            when (event) {
                is CategoryDetailEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (state.isEditMode) "Edit Category" else "New Category",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = { viewModel.onSaveCategory() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        Text(
                            text = if (state.isEditMode) "Save Changes" else "Save Category",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // --- Type Toggle ---
            // We allow changing type only if NOT in edit mode to avoid data issues,
            // or we can allow it if your backend handles it. Let's allow it for flexibility.
            CategoryTypeToggle(
                selectedType = state.type,
                onTypeSelected = { viewModel.onTypeChange(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- Live Preview ---
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(parseColor(state.selectedColor)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconByName(state.selectedIcon),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Category Name ---
            FinTrackTextField(
                value = state.name,
                onValueChange = { viewModel.onNameChange(it) },
                label = "Category Name",
                placeholder = "e.g., Groceries"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- Icon Selector ---
            Text(
                text = "Icon",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.height(120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayedIcons) { iconName ->
                    val isSelected = state.selectedIcon == iconName
                    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
                    val iconTint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .clickable { viewModel.onIconSelected(iconName) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconByName(iconName),
                            contentDescription = null,
                            tint = iconTint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Color Selector ---
            Text(
                text = "Color",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                availableColors.take(7).forEach { colorHex ->
                    val color = parseColor(colorHex)
                    val isSelected = state.selectedColor.equals(colorHex, ignoreCase = true)

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { viewModel.onColorSelected(colorHex) }
                            .then(
                                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                else Modifier
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun CategoryTypeToggle(
    selectedType: CategoryType,
    onTypeSelected: (CategoryType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(4.dp)
    ) {
        val expenseColor = if (selectedType == CategoryType.EXPENSE) MaterialTheme.colorScheme.primary else Color.Transparent
        val expenseTextColor = if (selectedType == CategoryType.EXPENSE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

        val incomeColor = if (selectedType == CategoryType.INCOME) MaterialTheme.colorScheme.primary else Color.Transparent
        val incomeTextColor = if (selectedType == CategoryType.INCOME) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

        Button(
            onClick = { onTypeSelected(CategoryType.EXPENSE) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = expenseColor, contentColor = expenseTextColor),
            shape = RoundedCornerShape(8.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text("Expense", fontWeight = FontWeight.SemiBold)
        }

        Button(
            onClick = { onTypeSelected(CategoryType.INCOME) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = incomeColor, contentColor = incomeTextColor),
            shape = RoundedCornerShape(8.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text("Income", fontWeight = FontWeight.SemiBold)
        }
    }
}

fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.Gray
    }
}

// Updated Icon Mapper with ALL icons
fun getIconByName(name: String): ImageVector {
    return when (name) {
        // Expense Icons
        "shopping_cart" -> Icons.Default.ShoppingCart
        "restaurant" -> Icons.Default.Restaurant
        "commute", "directions_bus" -> Icons.Default.DirectionsBus
        "home", "house" -> Icons.Default.House
        "receipt_long" -> Icons.Default.ReceiptLong
        "movie" -> Icons.Default.Movie
        "fitness_center" -> Icons.Default.FitnessCenter
        "flight" -> Icons.Default.Flight
        "school" -> Icons.Default.School
        "pets" -> Icons.Default.Pets
        "health_and_safety", "local_hospital" -> Icons.Default.LocalHospital
        "redeem" -> Icons.Default.Redeem
        "local_gas_station" -> Icons.Default.LocalGasStation
        "build" -> Icons.Default.Build

        // Income Icons
        "paid" -> Icons.Default.Paid
        "savings" -> Icons.Default.Savings
        "trending_up" -> Icons.Default.TrendingUp
        "work" -> Icons.Default.Work
        "card_giftcard" -> Icons.Default.CardGiftcard
        "sell" -> Icons.Default.Sell
        "account_balance", "account_balance_wallet" -> Icons.Default.AccountBalanceWallet // Using Wallet for general balance
        "request_quote" -> Icons.Default.RequestQuote
        "currency_exchange" -> Icons.Default.CurrencyExchange
        "wallet" -> Icons.Default.AccountBalanceWallet
        "add_business" -> Icons.Default.AddBusiness

        // Fallback
        else -> Icons.Default.Category
    }
}