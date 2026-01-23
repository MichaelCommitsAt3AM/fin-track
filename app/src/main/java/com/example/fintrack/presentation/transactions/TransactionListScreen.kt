package com.example.fintrack.presentation.transactions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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


private val filterOrder = listOf(
    "All",
    "Income",
    "Expense"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToTransaction: ((String) -> Unit)? = null,
    viewModel: TransactionListViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedSourceFilter by viewModel.selectedSourceFilter.collectAsState()
    val isSourceFilterExpanded by viewModel.isSourceFilterExpanded.collectAsState()
    val groupedTransactions by viewModel.uiState.collectAsState()
    val plannedTransactions by viewModel.plannedUiState.collectAsState()
    val currency by viewModel.currencyPreference.collectAsState()
    
    // Check if any planned transaction applies tomorrow
    val hasTransactionTomorrow = remember(plannedTransactions) {
        val tomorrowStart = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val tomorrowEnd = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }.timeInMillis
        
        plannedTransactions.any { it.dateMillis in tomorrowStart..tomorrowEnd }
    }
    
    // Auto-expand if transaction applies tomorrow, otherwise collapsed by default
    var isPlannedExpanded by remember(hasTransactionTomorrow) { 
        mutableStateOf(hasTransactionTomorrow) 
    }



    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Transactions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    // Only show back button when used as detail screen
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
                actions = {
                    // Download button removed
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        val listState = rememberLazyListState()

        // Infinite Scroll Logic
        LaunchedEffect(listState) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                .collect { index ->
                    val totalItems = listState.layoutInfo.totalItemsCount
                    if (index != null && totalItems > 0 && index >= totalItems - 5) {
                        viewModel.loadMore()
                    }
                }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // --- Header (Search & Filters) ---
            item(key = "header") {
                TransactionListHeader(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                    selectedFilter = selectedFilter,
                    onFilterSelected = { viewModel.onFilterSelected(it) },
                    isSourceFilterExpanded = isSourceFilterExpanded,
                    toggleSourceFilterExpanded = { viewModel.toggleSourceFilterExpanded() },
                    selectedSourceFilter = selectedSourceFilter,
                    onSourceFilterSelected = { viewModel.onSourceFilterSelected(it) }
                )
            }

            // --- Planned Transactions Section ---
            if (plannedTransactions.isNotEmpty()) {
                item(key = "planned_header") {
                    PlannedTransactionsHeader(
                        isExpanded = isPlannedExpanded,
                        count = plannedTransactions.size,
                        onToggle = { isPlannedExpanded = !isPlannedExpanded }
                    )
                }

                item(key = "planned_content") {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isPlannedExpanded,
                        enter = androidx.compose.animation.expandVertically(
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                            )
                        ) + androidx.compose.animation.fadeIn(
                            animationSpec = androidx.compose.animation.core.tween(300)
                        ),
                        exit = androidx.compose.animation.shrinkVertically(
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                            )
                        ) + androidx.compose.animation.fadeOut(
                            animationSpec = androidx.compose.animation.core.tween(200)
                        )
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            plannedTransactions.forEach { transaction ->
                                PlannedTransactionItem(
                                    transaction = transaction,
                                    currencySymbol = currency.symbol,
                                    onClick = { onNavigateToTransaction?.invoke(transaction.id) }
                                )
                            }

                            // Divider after planned transactions
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }

            // --- Regular Transactions ---
            if (groupedTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No transactions found for $selectedFilter",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                groupedTransactions.forEach { (date, transactions) ->
                    item(key = date) {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(
                        items = transactions,
                        key = { it.id }
                    ) { transaction ->
                        TransactionListItem(
                            transaction,
                            currency.symbol,
                            onClick = { onNavigateToTransaction?.invoke(transaction.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionListSegmentedControl(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val tabs = listOf("All", "Income", "Expense")
    
    // Determine the color based on selection
    val selectedColor = when (selectedFilter) {
        "Expense" -> MaterialTheme.colorScheme.error
        else -> FinTrackGreen
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp) // Slightly taller for touch target
            .clip(RoundedCornerShape(50)) // Fully rounded pill shape
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(4.dp)
    ) {
        val segmentWidth = maxWidth / tabs.size
        val selectedIndex = tabs.indexOf(selectedFilter)

        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            label = "indicatorOffset"
        )
        
        // Animated color for the indicator
        val indicatorColor by animateColorAsState(
            targetValue = selectedColor,
            animationSpec = tween(durationMillis = 300),
            label = "indicatorColor" 
        )

        // Sliding Indicator
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(indicatorColor)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEach { tab ->
                val isSelected = tab == selectedFilter

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 300),
                    label = "textColor"
                )

                Box(
                    modifier = Modifier
                        .width(segmentWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { onFilterSelected(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun ViewMoreButton(
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "View More",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun TransactionSourceSegmentedControl(
    selectedSource: String,
    onSourceSelected: (String) -> Unit
) {
    val tabs = listOf("All", "Manual", "M-Pesa")
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(4.dp)
    ) {
        val segmentWidth = maxWidth / tabs.size
        val selectedIndex = tabs.indexOf(selectedSource)

        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            label = "indicatorOffset"
        )
        
        // Use FinTrackGreen for all options
        val indicatorColor = FinTrackGreen

        // Sliding Indicator
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(indicatorColor)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEach { tab ->
                val isSelected = tab == selectedSource

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 300),
                    label = "textColor"
                )

                Box(
                    modifier = Modifier
                        .width(segmentWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { onSourceSelected(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionListItem(
    item: TransactionItemData,
    currencySymbol: String,
    onClick: () -> Unit = {}
) {
    val amountColor = if (item.amount > 0) FinTrackGreen else MaterialTheme.colorScheme.error
    val amountString = if (item.amount > 0) "+$currencySymbol ${"%.2f".format(item.amount)}" else "-$currencySymbol ${"%.2f".format(kotlin.math.abs(item.amount))}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
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

@Composable
fun PlannedTransactionsHeader(
    isExpanded: Boolean,
    count: Int,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)) // Match transaction item radius
            .background(MaterialTheme.colorScheme.surfaceContainerLow) // Match app UI
            .clickable(onClick = onToggle)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp) // Increased spacing
        ) {
            // Icon Box matching transaction items
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Column {
                Text(
                    text = "Planned Transactions",
                    style = MaterialTheme.typography.bodyLarge, // Match transaction title
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "$count planned",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PlannedTransactionItem(
    transaction: TransactionItemData,
    currencySymbol: String,
    onClick: () -> Unit = {}
) {
    val amountColor = if (transaction.amount > 0) FinTrackGreen else MaterialTheme.colorScheme.error
    val amountString = if (transaction.amount > 0) "+$currencySymbol ${"%.2f".format(transaction.amount)}" else "-$currencySymbol ${"%.2f".format(kotlin.math.abs(transaction.amount))}"

    // Format date for planned transaction
    val dateText = remember(transaction.dateMillis) {
        val cal = java.util.Calendar.getInstance()
        val tomorrow = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        
        val transactionCal = java.util.Calendar.getInstance().apply {
            timeInMillis = transaction.dateMillis
        }
        
        // Check if it's tomorrow
        val isTomorrow = transactionCal.get(java.util.Calendar.YEAR) == tomorrow.get(java.util.Calendar.YEAR) &&
                transactionCal.get(java.util.Calendar.DAY_OF_YEAR) == tomorrow.get(java.util.Calendar.DAY_OF_YEAR)
        
        if (isTomorrow) {
            "Applies Tomorrow"
        } else {
            val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            formatter.format(java.util.Date(transaction.dateMillis))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(transaction.color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = transaction.icon, contentDescription = null, tint = transaction.color)
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (dateText == "Applies Tomorrow") FontWeight.Bold else FontWeight.Normal,
                    color = if (dateText == "Applies Tomorrow") 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
