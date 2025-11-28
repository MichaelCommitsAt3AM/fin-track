package com.example.fintrack.presentation.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.fintrack.presentation.ui.theme.FinTrackGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionListViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val groupedTransactions by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Transactions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Download */ }) {
                        Icon(Icons.Default.Download, "Download", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // --- Search & Filter Bar ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Search transactions", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(50), // Fully rounded
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                // Filter Icon Button
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    onClick = { /* TODO: Open Advanced Filter */ },
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.FilterList, "Filter", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Filter Tabs (All / Income / Expense) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(4.dp)
            ) {
                // FIX: Call ViewModel functions instead of setting local state manually
                FilterTab("All", selectedFilter == "All") { viewModel.onFilterSelected("All") }
                FilterTab("Income", selectedFilter == "Income") { viewModel.onFilterSelected("Income") }
                FilterTab("Expense", selectedFilter == "Expense") { viewModel.onFilterSelected("Expense") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Grouped Transaction List ---
            if (groupedTransactions.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No transactions found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    groupedTransactions.forEach { (date, transactions) ->
                        item {
                            Text(
                                text = date,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(transactions) { transaction ->
                            TransactionListItem(transaction)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.FilterTab(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        shape = RoundedCornerShape(50),
        elevation = ButtonDefaults.buttonElevation(0.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TransactionListItem(item: TransactionItemData) {
    val amountColor = if (item.amount > 0) FinTrackGreen else MaterialTheme.colorScheme.error
    val amountString = if (item.amount > 0) "+Ksh ${"%.2f".format(item.amount)}" else "-Ksh ${"%.2f".format(kotlin.math.abs(item.amount))}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(item.color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = item.icon, contentDescription = null, tint = item.color)
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = item.category,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Amount
        Text(
            text = amountString,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

//data class TransactionItemData(
//    val title: String,
//    val category: String,
//    val amount: Double,
//    val icon: ImageVector,
//    val color: Color,
//    val dateMillis: Long = 0 // Added default value for existing calls if any
//)